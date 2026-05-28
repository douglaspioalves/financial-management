package com.gastos.dto.installment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InstallmentResponse(
        UUID id,
        int number,
        BigDecimal amount,
        LocalDate referenceMonth
) {}
