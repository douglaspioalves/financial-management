package com.gastos.controller;

import com.gastos.dto.card.CardRequest;
import com.gastos.dto.card.CardResponse;
import com.gastos.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    /**
     * GET /api/cards — Lista todos os cartões ordenados por nome.
     */
    @GetMapping
    public ResponseEntity<List<CardResponse>> findAll() {
        return ResponseEntity.ok(cardService.findAll());
    }

    /**
     * GET /api/cards/{id} — Retorna um cartão pelo id.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CardResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(cardService.findById(id));
    }

    /**
     * POST /api/cards — Cria um novo cartão.
     */
    @PostMapping
    public ResponseEntity<CardResponse> create(@Valid @RequestBody CardRequest request) {
        CardResponse response = cardService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/cards/{id} — Atualiza um cartão existente.
     */
    @PutMapping("/{id}")
    public ResponseEntity<CardResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody CardRequest request) {
        return ResponseEntity.ok(cardService.update(id, request));
    }

    /**
     * DELETE /api/cards/{id} — Remove um cartão (409 se tiver lançamentos vinculados).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        cardService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
