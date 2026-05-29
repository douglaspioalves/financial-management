package com.gastos.service;

import com.gastos.domain.Card;
import com.gastos.domain.Installment;
import com.gastos.domain.Transaction;
import com.gastos.repository.InstallmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InstallmentService {

    private final InstallmentRepository installmentRepository;

    /**
     * Gera e persiste N parcelas para uma compra parcelada.
     *
     * Algoritmo de reference_month:
     * - Se o dia da compra é anterior ao fechamento efetivo do cartão no mês da compra,
     *   a primeira parcela cai no mesmo mês da compra; caso contrário, cai no mês seguinte.
     * - O fechamento efetivo é min(closingDay, lastDayOfPurchaseMonth), para lidar
     *   corretamente com cartões cujo closingDay=31 em meses com menos de 31 dias
     *   (ex.: abril tem 30 dias — sem essa correção, purchaseDay < 31 sempre seria
     *   verdadeiro, colocando todas as compras do mês no mesmo ciclo).
     * - As parcelas seguintes são meses consecutivos.
     *
     * Arredondamento:
     * - Cada parcela tem floor(totalAmount / N) arredondado para baixo (RoundingMode.DOWN).
     * - A última parcela absorve a diferença para garantir que a soma == totalAmount.
     *
     * @param transaction Transaction já salva (com id gerado)
     * @param card        Card cujo closingDay determina o primeiro mês de referência
     * @return lista das parcelas salvas, ordenadas por número
     */
    @Transactional
    public List<Installment> generateInstallments(Transaction transaction, Card card) {
        int n = transaction.getInstallmentsTotal();
        BigDecimal totalAmount = transaction.getAmount();
        LocalDate purchaseDate = transaction.getDate();
        int closingDay = card.getClosingDay();

        // Fechamento efetivo: evita bug em meses curtos quando closingDay=31
        // (ex.: abril tem 30 dias — min(31, 30) = 30)
        int effectiveClosingDay = Math.min(closingDay, purchaseDate.lengthOfMonth());

        // Determina o YearMonth da primeira parcela
        YearMonth firstYearMonth = (purchaseDate.getDayOfMonth() < effectiveClosingDay)
                ? YearMonth.from(purchaseDate)
                : YearMonth.from(purchaseDate).plusMonths(1);

        // Calcula valor por parcela (arredondamento para baixo)
        BigDecimal perInstallment = totalAmount.divide(
                BigDecimal.valueOf(n), 2, RoundingMode.DOWN);

        // Última parcela absorve a diferença de centavos
        BigDecimal lastInstallment = totalAmount.subtract(
                perInstallment.multiply(BigDecimal.valueOf(n - 1)));

        List<Installment> installments = new ArrayList<>(n);
        for (int i = 1; i <= n; i++) {
            LocalDate referenceMonth = firstYearMonth.plusMonths(i - 1).atDay(1);
            BigDecimal installmentAmount = (i == n) ? lastInstallment : perInstallment;

            Installment installment = Installment.builder()
                    .transaction(transaction)
                    .number(i)
                    .amount(installmentAmount)
                    .referenceMonth(referenceMonth)
                    .build();

            installments.add(installment);
        }

        return installmentRepository.saveAll(installments);
    }
}
