package com.gastos.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        String month,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance,
        MonthSummaryDTO previousMonth,
        BigDecimal incomeVariation,
        BigDecimal expenseVariation,
        List<CategoryExpenseDTO> expenseByCategory,
        List<RecentTransactionDTO> recentTransactions
) {}
