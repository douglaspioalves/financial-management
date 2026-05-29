package com.gastos.dto.category;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(

        @Size(max = 100, message = "Nome da categoria deve ter no máximo 100 caracteres.")
        String name,

        @Pattern(regexp = "#[0-9A-Fa-f]{6}", message = "Cor deve estar no formato hexadecimal #RRGGBB.")
        String color,

        @NotNull(message = "O campo version é obrigatório para atualização.")
        Long version
) {
}
