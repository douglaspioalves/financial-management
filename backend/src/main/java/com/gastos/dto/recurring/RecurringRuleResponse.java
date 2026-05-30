package com.gastos.dto.recurring;

import com.gastos.domain.PaymentMethod;
import com.gastos.domain.RecurringFrequency;
import com.gastos.domain.SplitRule;
import com.gastos.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record RecurringRuleResponse(
        UUID id,
        TransactionType type,
        BigDecimal amount,
        String description,
        UUID categoryId,
        String categoryName,
        UUID paidByPersonId,
        String paidByPersonName,
        PaymentMethod paymentMethod,
        SplitRule splitRule,
        RecurringFrequency frequency,
        LocalDate nextDate,
        boolean active,
        LocalDateTime createdAt,
        Long version
) {}
