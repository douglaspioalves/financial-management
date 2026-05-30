package com.gastos.service;

import com.gastos.domain.Installment;
import com.gastos.domain.Person;
import com.gastos.domain.SplitRule;
import com.gastos.domain.Transaction;
import com.gastos.dto.settlement.PersonSettlementDTO;
import com.gastos.dto.settlement.SettlementResponse;
import com.gastos.repository.InstallmentRepository;
import com.gastos.repository.PersonRepository;
import com.gastos.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Calcula o acerto de contas mensal.
 *
 * Regras de negócio críticas:
 *  1. Despesas parceladas (CREDIT, installmentsTotal > 1): entram via Installment.referenceMonth.
 *  2. Demais despesas: entram via transaction.date no mês.
 *  3. PROPORTIONAL: proporção calculada sobre receitas individuais (INCOME + PERSON_A/PERSON_B) do mês.
 *     Se não houver receitas, as despesas PROPORTIONAL ficam pendentes.
 *  4. Todos os cálculos usam BigDecimal com RoundingMode.HALF_UP, scale=2.
 */
@Service
@RequiredArgsConstructor
public class SettlementService {

    private static final BigDecimal SETTLED_THRESHOLD = new BigDecimal("0.01");

    private final TransactionRepository transactionRepository;
    private final InstallmentRepository installmentRepository;
    private final PersonRepository personRepository;

    /**
     * Calcula o acerto de contas do mês informado.
     *
     * @param month mês de referência
     * @return SettlementResponse com saldos e quem deve a quem
     */
    @Transactional(readOnly = true)
    public SettlementResponse calculate(YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        // Busca as duas pessoas (ordem determinística: pelo nome)
        List<Person> persons = new java.util.ArrayList<>(personRepository.findAll());
        if (persons.size() < 2) {
            throw new IllegalStateException("O sistema requer exatamente duas pessoas cadastradas.");
        }
        persons.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        Person personA = persons.get(0);
        Person personB = persons.get(1);

        // Passo 1 — Agregar despesas do mês
        List<Transaction> cashExpenses = transactionRepository.findCashExpensesByMonth(start, end);
        List<Installment> installments = installmentRepository.findExpenseInstallmentsByMonth(start);

        // Passo 3 — Proporção para PROPORTIONAL (receitas individuais do mês)
        List<Transaction> individualIncomes = transactionRepository.findIndividualIncomesByMonth(start, end);

        BigDecimal incomeA = individualIncomes.stream()
                .filter(t -> t.getSplitRule() == SplitRule.PERSON_A)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal incomeB = individualIncomes.stream()
                .filter(t -> t.getSplitRule() == SplitRule.PERSON_B)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalIndividualIncome = incomeA.add(incomeB);
        boolean hasProportionalIncome = totalIndividualIncome.compareTo(BigDecimal.ZERO) > 0;

        BigDecimal ratioA = BigDecimal.ZERO;
        BigDecimal ratioB = BigDecimal.ZERO;
        if (hasProportionalIncome) {
            ratioA = incomeA.divide(totalIndividualIncome, 10, RoundingMode.HALF_UP);
            ratioB = BigDecimal.ONE.subtract(ratioA);
        }

        // Acumuladores
        BigDecimal paidA = BigDecimal.ZERO;
        BigDecimal paidB = BigDecimal.ZERO;
        BigDecimal shouldPayA = BigDecimal.ZERO;
        BigDecimal shouldPayB = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        boolean hasPendingProportional = false;

        // Passo 2 — Processar despesas à vista
        for (Transaction t : cashExpenses) {
            BigDecimal amount = t.getAmount().setScale(2, RoundingMode.HALF_UP);
            totalExpense = totalExpense.add(amount);

            // Quem pagou
            if (t.getPaidByPerson().getId().equals(personA.getId())) {
                paidA = paidA.add(amount);
            } else {
                paidB = paidB.add(amount);
            }

            // Divisão
            ShareResult share = computeShare(t.getSplitRule(), amount, ratioA, ratioB, hasProportionalIncome);
            if (share.pending()) {
                hasPendingProportional = true;
                // Não computa shouldPay para este lançamento
            } else {
                shouldPayA = shouldPayA.add(share.shareA());
                shouldPayB = shouldPayB.add(share.shareB());
            }
        }

        // Passo 2 — Processar parcelas
        for (Installment i : installments) {
            BigDecimal amount = i.getAmount().setScale(2, RoundingMode.HALF_UP);
            totalExpense = totalExpense.add(amount);

            Transaction t = i.getTransaction();

            // Quem pagou
            if (t.getPaidByPerson().getId().equals(personA.getId())) {
                paidA = paidA.add(amount);
            } else {
                paidB = paidB.add(amount);
            }

            // Divisão (proporção do mês em que a parcela cai = mês passado como parâmetro)
            ShareResult share = computeShare(t.getSplitRule(), amount, ratioA, ratioB, hasProportionalIncome);
            if (share.pending()) {
                hasPendingProportional = true;
            } else {
                shouldPayA = shouldPayA.add(share.shareA());
                shouldPayB = shouldPayB.add(share.shareB());
            }
        }

        // Passo 5 — Resultado
        BigDecimal balanceA = paidA.subtract(shouldPayA).setScale(2, RoundingMode.HALF_UP);
        BigDecimal balanceB = paidB.subtract(shouldPayB).setScale(2, RoundingMode.HALF_UP);

        BigDecimal amountOwed = balanceA.abs().setScale(2, RoundingMode.HALF_UP);

        boolean settled;
        String debtor = null;
        String creditor = null;
        BigDecimal amountOwedResult = null;

        if (amountOwed.compareTo(SETTLED_THRESHOLD) < 0) {
            settled = true;
        } else {
            settled = false;
            amountOwedResult = amountOwed;
            if (balanceA.compareTo(BigDecimal.ZERO) > 0) {
                // personA pagou mais → personB deve para personA
                debtor = "PERSON_B";
                creditor = "PERSON_A";
            } else {
                // personB pagou mais → personA deve para personB
                debtor = "PERSON_A";
                creditor = "PERSON_B";
            }
        }

        // Se há pendência, amountOwed fica null (acerto incompleto)
        if (hasPendingProportional) {
            amountOwedResult = null;
            debtor = null;
            creditor = null;
        }

        String pendingMessage = hasPendingProportional
                ? "Há despesas com divisão proporcional neste mês, mas nenhuma receita individual foi cadastrada. " +
                  "Cadastre as receitas de cada pessoa para calcular a divisão proporcional."
                : null;

        PersonSettlementDTO dtoA = new PersonSettlementDTO(
                personA.getId(),
                personA.getName(),
                paidA.setScale(2, RoundingMode.HALF_UP),
                shouldPayA.setScale(2, RoundingMode.HALF_UP),
                balanceA
        );

        PersonSettlementDTO dtoB = new PersonSettlementDTO(
                personB.getId(),
                personB.getName(),
                paidB.setScale(2, RoundingMode.HALF_UP),
                shouldPayB.setScale(2, RoundingMode.HALF_UP),
                balanceB
        );

        return new SettlementResponse(
                start,
                totalExpense.setScale(2, RoundingMode.HALF_UP),
                dtoA,
                dtoB,
                debtor,
                creditor,
                amountOwedResult,
                settled,
                hasPendingProportional,
                pendingMessage
        );
    }

    /**
     * Calcula a parcela de cada pessoa para uma despesa, conforme a regra de divisão.
     *
     * @param splitRule          regra de divisão da transação
     * @param amount             valor da despesa (ou parcela)
     * @param ratioA             proporção da pessoa A (só relevante para PROPORTIONAL)
     * @param ratioB             proporção da pessoa B (só relevante para PROPORTIONAL)
     * @param hasProportionalIncome se true, há receitas individuais para calcular a proporção
     * @return ShareResult com as parcelas ou flag pending=true
     */
    private ShareResult computeShare(SplitRule splitRule, BigDecimal amount,
                                     BigDecimal ratioA, BigDecimal ratioB,
                                     boolean hasProportionalIncome) {
        return switch (splitRule) {
            case FIFTY_FIFTY -> {
                BigDecimal half = amount.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
                // Garante que shareA + shareB == amount (sem centavo perdido)
                BigDecimal otherHalf = amount.subtract(half);
                yield new ShareResult(half, otherHalf, false);
            }
            case PERSON_A -> new ShareResult(amount, BigDecimal.ZERO, false);
            case PERSON_B -> new ShareResult(BigDecimal.ZERO, amount, false);
            case PROPORTIONAL -> {
                if (!hasProportionalIncome) {
                    yield new ShareResult(BigDecimal.ZERO, BigDecimal.ZERO, true);
                }
                BigDecimal shareA = amount.multiply(ratioA).setScale(2, RoundingMode.HALF_UP);
                BigDecimal shareB = amount.subtract(shareA);  // ajusta arredondamento
                yield new ShareResult(shareA, shareB, false);
            }
        };
    }

    /**
     * Resultado do cálculo de divisão para uma despesa.
     *
     * @param shareA  quanto a Pessoa A deve pagar
     * @param shareB  quanto a Pessoa B deve pagar
     * @param pending true se a proporção não pode ser calculada (sem receitas individuais)
     */
    private record ShareResult(BigDecimal shareA, BigDecimal shareB, boolean pending) {}
}
