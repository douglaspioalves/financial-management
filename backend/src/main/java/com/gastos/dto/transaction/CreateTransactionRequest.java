package com.gastos.dto.transaction;

import com.gastos.domain.PaymentMethod;
import com.gastos.domain.SplitRule;
import com.gastos.domain.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateTransactionRequest(

        @NotNull(message = "O tipo do lançamento é obrigatório.")
        TransactionType type,

        @NotNull(message = "O valor é obrigatório.")
        @Positive(message = "O valor deve ser maior que zero.")
        BigDecimal amount,

        @NotNull(message = "A data é obrigatória.")
        LocalDate date,

        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
        String description,

        @NotNull(message = "Categoria é obrigatória.")
        UUID categoryId,

        @NotNull(message = "O participante pagador é obrigatório.")
        UUID paidByPersonId,

        @NotNull(message = "O método de pagamento é obrigatório.")
        PaymentMethod paymentMethod,

        UUID cardId,

        @NotNull(message = "A regra de divisão é obrigatória.")
        SplitRule splitRule,

        Integer installmentsTotal
) {}
