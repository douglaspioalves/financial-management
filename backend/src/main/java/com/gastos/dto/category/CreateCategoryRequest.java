package com.gastos.dto.category;

import com.gastos.domain.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(

        @NotBlank(message = "Nome da categoria é obrigatório.")
        @Size(max = 100, message = "Nome da categoria deve ter no máximo 100 caracteres.")
        String name,

        @NotNull(message = "Tipo inválido. Use EXPENSE, INCOME ou BOTH.")
        CategoryType type,

        @NotBlank(message = "Cor deve estar no formato hexadecimal #RRGGBB.")
        @Pattern(regexp = "#[0-9A-Fa-f]{6}", message = "Cor deve estar no formato hexadecimal #RRGGBB.")
        String color
) {
}
