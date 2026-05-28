package com.gastos.dto.person;

import java.util.UUID;

public record PersonResponse(
        UUID id,
        String name,
        String color
) {
}
