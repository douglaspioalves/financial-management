package com.gastos.dto.card;

import java.util.UUID;

public record CardResponse(
        UUID id,
        String name,
        UUID ownerPersonId,
        String ownerPersonName,
        int closingDay,
        int dueDay,
        Long version
) {
}
