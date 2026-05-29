package com.gastos.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecentTransactionDTO(
        UUID id,
        LocalDate date,
        String description,
        BigDecimal amount,
        String type,
        String categoryName,
        String paidByPersonName
) {}
