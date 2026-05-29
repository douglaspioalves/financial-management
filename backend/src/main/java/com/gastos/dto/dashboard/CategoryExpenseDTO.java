package com.gastos.dto.dashboard;

import java.math.BigDecimal;
import java.util.UUID;

public record CategoryExpenseDTO(
        UUID categoryId,
        String categoryName,
        String categoryColor,
        BigDecimal total,
        BigDecimal percentage
) {}
