package com.gastos.service;

import com.gastos.domain.Installment;
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
import java.util.UUID;

/**
 * Serviço de acerto de contas entre PersonA e PersonB para um mês de referência.
 *
 * <p>Regras de negócio:
 * <ul>
 *   <li>As despesas do mês vêm das {@code Installment} com {@code referenceMonth} = mês
 *       <em>e</em> das despesas à vista (não-parceladas) com {@code date} dentro do mês.</li>
 *   <li>FIFTY_FIFTY → 50% para cada pessoa.</li>
 *   <li>PERSON_A → 100% para PersonA; PERSON_B → 100% para PersonB.</li>
 *   <li>PROPORTIONAL → proporção das receitas individuais do mês (type=INCOME, split=PERSON_A|B).
 *       Receitas FIFTY_FIFTY são compartilhadas e NÃO entram no cálculo da proporção.
 *       Se não há receitas individuais, o acerto fica pendente.</li>
 *   <li>Se qualquer despesa PROPORTIONAL estiver pendente, o acerto inteiro fica pendente.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final TransactionRepository transactionRepository;
    private final InstallmentRepository installmentRepository;
    private final PersonRepository personRepository;

    @Transactional(readOnly = true)
    public SettlementResponse calculate(LocalDate month, UUID personAId, UUID personBId) {
        YearMonth yearMonth = YearMonth.from(month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        // Carrega as duas pessoas
        var personA = personRepository.findById(personAId)
                .orElseThrow(() -> new IllegalArgumentException("Person A não encontrada: " + personAId));
        var personB = personRepository.findById(personBId)
                .orElseThrow(() -> new IllegalArgumentException("Person B não encontrada: " + personBId));

        // Receitas individuais do mês (para proporção PROPORTIONAL)
        List<Transaction> incomes = transactionRepository.findIncomesByMonth(start, end);
        BigDecimal incomeA = incomes.stream()
                .filter(t -> t.getSplitRule() == SplitRule.PERSON_A)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal incomeB = incomes.stream()
                .filter(t -> t.getSplitRule() == SplitRule.PERSON_B)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalIndividualIncome = incomeA.add(incomeB);
        boolean hasIndividualIncome = totalIndividualIncome.compareTo(BigDecimal.ZERO) > 0;

        // Despesas do mês: parcelas de cartão + despesas à vista
        List<Installment> installments = installmentRepository.findExpenseInstallmentsByMonth(start);
        List<Transaction> cashExpenses = transactionRepository.findCashExpensesByMonth(start, end);

        // Acumula shouldPay por pessoa e totalPaid por pessoa
        BigDecimal shouldPayA = BigDecimal.ZERO;
        BigDecimal shouldPayB = BigDecimal.ZERO;
        BigDecimal totalPaidA = BigDecimal.ZERO;
        BigDecimal totalPaidB = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        boolean pendingProportional = false;

        // Processa parcelas de cartão
        for (Installment installment : installments) {
            Transaction tx = installment.getTransaction();
            BigDecimal amount = installment.getAmount();
            totalExpense = totalExpense.add(amount);

            // quem pagou
            if (tx.getPaidByPerson().getId().equals(personAId)) {
                totalPaidA = totalPaidA.add(amount);
            } else {
                totalPaidB = totalPaidB.add(amount);
            }

            // quanto cada um deveria pagar
            BigDecimal[] shares = calculateShares(amount, tx.getSplitRule(),
                    incomeA, incomeB, totalIndividualIncome, hasIndividualIncome);
            if (shares == null) {
                pendingProportional = true;
            } else {
                shouldPayA = shouldPayA.add(shares[0]);
                shouldPayB = shouldPayB.add(shares[1]);
            }
        }

        // Processa despesas à vista
        for (Transaction tx : cashExpenses) {
            BigDecimal amount = tx.getAmount();
            totalExpense = totalExpense.add(amount);

            if (tx.getPaidByPerson().getId().equals(personAId)) {
                totalPaidA = totalPaidA.add(amount);
            } else {
                totalPaidB = totalPaidB.add(amount);
            }

            BigDecimal[] shares = calculateShares(amount, tx.getSplitRule(),
                    incomeA, incomeB, totalIndividualIncome, hasIndividualIncome);
            if (shares == null) {
                pendingProportional = true;
            } else {
                shouldPayA = shouldPayA.add(shares[0]);
                shouldPayB = shouldPayB.add(shares[1]);
            }
        }

        // Monta DTOs das pessoas
        BigDecimal balanceA = totalPaidA.subtract(shouldPayA).setScale(2, RoundingMode.HALF_UP);
        BigDecimal balanceB = totalPaidB.subtract(shouldPayB).setScale(2, RoundingMode.HALF_UP);

        var dtoA = new PersonSettlementDTO(personAId, personA.getName(),
                totalPaidA.setScale(2, RoundingMode.HALF_UP),
                shouldPayA.setScale(2, RoundingMode.HALF_UP),
                balanceA);
        var dtoB = new PersonSettlementDTO(personBId, personB.getName(),
                totalPaidB.setScale(2, RoundingMode.HALF_UP),
                shouldPayB.setScale(2, RoundingMode.HALF_UP),
                balanceB);

        // Se há PROPORTIONAL pendente, retorna pendente (sem calcular amountOwed)
        if (pendingProportional) {
            return new SettlementResponse(
                    start,
                    totalExpense.setScale(2, RoundingMode.HALF_UP),
                    dtoA, dtoB,
                    null, null, null,
                    false, true,
                    "Cadastre as receitas do mês para calcular a divisão proporcional."
            );
        }

        // Calcula quem deve para quem
        // balanceA > 0 → PersonA é credora (pagou mais); balanceA < 0 → PersonA é devedora
        BigDecimal absBalance = balanceA.abs();
        boolean settled = absBalance.compareTo(new BigDecimal("0.01")) < 0;

        String debtor = null;
        String creditor = null;
        BigDecimal amountOwed = null;

        if (!settled) {
            if (balanceA.compareTo(BigDecimal.ZERO) > 0) {
                // PersonA pagou mais do que deveria → PersonB deve para PersonA
                debtor = "PERSON_B";
                creditor = "PERSON_A";
            } else {
                // PersonB pagou mais do que deveria → PersonA deve para PersonB
                debtor = "PERSON_A";
                creditor = "PERSON_B";
            }
            amountOwed = absBalance;
        }

        return new SettlementResponse(
                start,
                totalExpense.setScale(2, RoundingMode.HALF_UP),
                dtoA, dtoB,
                debtor, creditor, amountOwed,
                settled, false, null
        );
    }

    /**
     * Calcula as fatias de uma despesa por pessoa conforme a regra de divisão.
     *
     * @return array [shareA, shareB] ou null se PROPORTIONAL sem receitas individuais
     */
    BigDecimal[] calculateShares(
            BigDecimal amount,
            SplitRule splitRule,
            BigDecimal incomeA,
            BigDecimal incomeB,
            BigDecimal totalIndividualIncome,
            boolean hasIndividualIncome) {

        return switch (splitRule) {
            case FIFTY_FIFTY -> {
                BigDecimal half = amount.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
                BigDecimal otherHalf = amount.subtract(half);
                yield new BigDecimal[]{half, otherHalf};
            }
            case PERSON_A -> new BigDecimal[]{amount, BigDecimal.ZERO};
            case PERSON_B -> new BigDecimal[]{BigDecimal.ZERO, amount};
            case PROPORTIONAL -> {
                if (!hasIndividualIncome) {
                    yield null; // pendente
                }
                BigDecimal ratioA = incomeA.divide(totalIndividualIncome, 10, RoundingMode.HALF_UP);
                BigDecimal shareA = amount.multiply(ratioA).setScale(2, RoundingMode.HALF_UP);
                BigDecimal shareB = amount.subtract(shareA);
                yield new BigDecimal[]{shareA, shareB};
            }
        };
    }
}
