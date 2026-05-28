package com.gastos.dto.card;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CardRequest(

        @NotBlank(message = "Nome do cartão é obrigatório.")
        @Size(max = 100, message = "Nome do cartão deve ter no máximo 100 caracteres.")
        String name,

        @NotNull(message = "Proprietário do cartão é obrigatório.")
        UUID ownerPersonId,

        @NotNull(message = "Dia de fechamento é obrigatório.")
        @Min(value = 1, message = "Dia de fechamento deve estar entre 1 e 31.")
        @Max(value = 31, message = "Dia de fechamento deve estar entre 1 e 31.")
        Integer closingDay,

        @NotNull(message = "Dia de vencimento é obrigatório.")
        @Min(value = 1, message = "Dia de vencimento deve estar entre 1 e 31.")
        @Max(value = 31, message = "Dia de vencimento deve estar entre 1 e 31.")
        Integer dueDay
) {
}
