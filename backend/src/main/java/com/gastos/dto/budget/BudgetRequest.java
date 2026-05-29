package com.gastos.dto.budget;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BudgetRequest(
        @NotNull(message = "O ID da categoria é obrigatório.")
        UUID categoryId,

        @NotNull(message = "O mês é obrigatório.")
        LocalDate month,

        @NotNull(message = "O valor limite é obrigatório.")
        @Positive(message = "O valor limite deve ser positivo.")
        BigDecimal limitAmount,

        /**
         * Versão para optimistic locking. Obrigatória apenas no PUT.
         * No POST pode ser omitida (null).
         */
        Long version
) {}
