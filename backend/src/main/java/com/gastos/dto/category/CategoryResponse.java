package com.gastos.dto.category;

import com.gastos.domain.CategoryType;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        CategoryType type,
        String color,
        Long version
) {
}
