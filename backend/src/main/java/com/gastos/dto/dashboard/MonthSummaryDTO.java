package com.gastos.dto.dashboard;

import java.math.BigDecimal;

public record MonthSummaryDTO(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance
) {}
