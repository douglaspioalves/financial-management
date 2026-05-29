package com.gastos.service;

import com.gastos.domain.Installment;
import com.gastos.domain.Transaction;
import com.gastos.dto.dashboard.CategoryExpenseDTO;
import com.gastos.dto.dashboard.DashboardResponse;
import com.gastos.dto.dashboard.MonthSummaryDTO;
import com.gastos.dto.dashboard.RecentTransactionDTO;
import com.gastos.repository.InstallmentRepository;
import com.gastos.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final InstallmentRepository installmentRepository;

    /**
     * Calcula o dashboard completo para o mês informado.
     *
     * Regra de agregação de despesas:
     *  - Transações CREDIT com installmentsTotal > 1: soma via Installment.amount
     *    onde installment.referenceMonth == primeiro dia do mês
     *  - Todas as demais: soma transaction.amount onde transaction.date está no mês
     *
     * Receitas: sempre por transaction.date
     */
    @Transactional(readOnly = true)
    public DashboardResponse calculate(YearMonth month) {
        MonthData current = computeMonthData(month);
        MonthData previous = computeMonthData(month.minusMonths(1));

        BigDecimal incomeVariation = computeVariation(current.totalIncome(), previous.totalIncome());
        BigDecimal expenseVariation = computeVariation(current.totalExpense(), previous.totalExpense());

        MonthSummaryDTO previousMonth = new MonthSummaryDTO(
                previous.totalIncome(),
                previous.totalExpense(),
                previous.totalIncome().subtract(previous.totalExpense())
        );

        List<CategoryExpenseDTO> expenseByCategory = buildCategoryBreakdown(
                current.cashExpenses(), current.installments(), current.totalExpense()
        );

        List<RecentTransactionDTO> recentTransactions = buildRecentTransactions(month);

        return new DashboardResponse(
                month.toString(),
                current.totalIncome(),
                current.totalExpense(),
                current.totalIncome().subtract(current.totalExpense()),
                previousMonth,
                incomeVariation,
                expenseVariation,
                expenseByCategory,
                recentTransactions
        );
    }

    // --- internals ---

    /**
     * Agrega todos os dados financeiros do mês especificado.
     */
    private MonthData computeMonthData(YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        // Despesas à vista (cash, debit, pix, transfer, crédito à vista)
        List<Transaction> cashExpenses = transactionRepository.findCashExpensesByMonth(start, end);

        // Despesas parceladas via installment
        List<Installment> installments = installmentRepository.findExpenseInstallmentsByMonth(start);

        // Receitas do mês
        List<Transaction> incomes = transactionRepository.findIncomesByMonth(start, end);

        BigDecimal totalExpense = sumTransactions(cashExpenses)
                .add(sumInstallments(installments));

        BigDecimal totalIncome = sumTransactions(incomes);

        return new MonthData(totalIncome, totalExpense, cashExpenses, installments);
    }

    private BigDecimal sumTransactions(List<Transaction> transactions) {
        return transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumInstallments(List<Installment> installments) {
        return installments.stream()
                .map(Installment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calcula variação percentual: ((current - previous) / previous) * 100.
     * Retorna null se o valor anterior for zero (sem dados suficientes).
     */
    private BigDecimal computeVariation(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, RoundingMode.HALF_UP);
    }

    /**
     * Agrupa as despesas do mês por categoria e calcula o percentual de cada uma.
     * Ordenado por total DESC.
     */
    private List<CategoryExpenseDTO> buildCategoryBreakdown(
            List<Transaction> cashExpenses,
            List<Installment> installments,
            BigDecimal totalExpense) {

        // Mapa: categoryId -> [name, color, total]
        Map<UUID, CategoryAccumulator> map = new LinkedHashMap<>();

        for (Transaction t : cashExpenses) {
            UUID catId = t.getCategory().getId();
            map.computeIfAbsent(catId, id -> new CategoryAccumulator(
                    t.getCategory().getName(),
                    t.getCategory().getColor()
            )).add(t.getAmount());
        }

        for (Installment i : installments) {
            Transaction t = i.getTransaction();
            UUID catId = t.getCategory().getId();
            map.computeIfAbsent(catId, id -> new CategoryAccumulator(
                    t.getCategory().getName(),
                    t.getCategory().getColor()
            )).add(i.getAmount());
        }

        List<CategoryExpenseDTO> result = new ArrayList<>();
        for (Map.Entry<UUID, CategoryAccumulator> entry : map.entrySet()) {
            UUID catId = entry.getKey();
            CategoryAccumulator acc = entry.getValue();
            BigDecimal percentage = totalExpense.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : acc.total.multiply(BigDecimal.valueOf(100))
                               .divide(totalExpense, 2, RoundingMode.HALF_UP);
            result.add(new CategoryExpenseDTO(catId, acc.name, acc.color, acc.total, percentage));
        }

        // Ordenar por total DESC
        result.sort(Comparator.comparing(CategoryExpenseDTO::total).reversed());
        return result;
    }

    /**
     * Retorna as 10 transações mais recentes do mês, ordenadas por date DESC.
     * Para parceladas, usa a data da transaction pai.
     */
    private List<RecentTransactionDTO> buildRecentTransactions(YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        return transactionRepository.findRecentByMonth(start, end)
                .stream()
                .limit(10)
                .map(t -> new RecentTransactionDTO(
                        t.getId(),
                        t.getDate(),
                        t.getDescription(),
                        t.getAmount(),
                        t.getType().name(),
                        t.getCategory().getName(),
                        t.getPaidByPerson().getName()
                ))
                .toList();
    }

    // --- value objects internos ---

    private record MonthData(
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            List<Transaction> cashExpenses,
            List<Installment> installments
    ) {}

    private static class CategoryAccumulator {
        final String name;
        final String color;
        BigDecimal total = BigDecimal.ZERO;

        CategoryAccumulator(String name, String color) {
            this.name = name;
            this.color = color;
        }

        void add(BigDecimal value) {
            this.total = this.total.add(value);
        }
    }
}
