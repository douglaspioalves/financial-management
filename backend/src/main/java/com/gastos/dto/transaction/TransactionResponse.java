package com.gastos.dto.transaction;

import com.gastos.domain.CategoryType;
import com.gastos.domain.PaymentMethod;
import com.gastos.domain.SplitRule;
import com.gastos.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        TransactionType type,
        BigDecimal amount,
        LocalDate date,
        String description,
        CategorySummary category,
        PersonSummary paidByPerson,
        PaymentMethod paymentMethod,
        CardSummary card,
        SplitRule splitRule,
        int installmentsTotal,
        LocalDateTime createdAt,
        Long version
) {

    public record CategorySummary(
            UUID id,
            String name,
            CategoryType type,
            String color
    ) {}

    public record PersonSummary(
            UUID id,
            String name,
            String color
    ) {}

    /**
     * Resumo do cartão de crédito vinculado ao lançamento.
     * Retornado como null quando o lançamento não usa cartão de crédito.
     */
    public record CardSummary(
            UUID id,
            String name
    ) {}
}
