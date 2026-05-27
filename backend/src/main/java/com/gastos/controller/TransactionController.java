package com.gastos.controller;

import com.gastos.dto.transaction.CreateTransactionRequest;
import com.gastos.dto.transaction.TransactionResponse;
import com.gastos.dto.transaction.UpdateTransactionRequest;
import com.gastos.service.TransactionService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * POST /api/transactions — Cria um lançamento financeiro.
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody CreateTransactionRequest request) {
        TransactionResponse response = transactionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/transactions?month=yyyy-MM — Lista lançamentos do mês.
     */
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> findByMonth(
            @RequestParam(name = "month", required = false) String month) {
        List<TransactionResponse> response = transactionService.findByMonth(month);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/transactions/{id} — Retorna detalhe de um lançamento.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> findById(@PathVariable UUID id) {
        TransactionResponse response = transactionService.findById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/transactions/{id} — Edita um lançamento existente.
     */
    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTransactionRequest request) {
        TransactionResponse response = transactionService.update(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/transactions/{id} — Remove permanentemente um lançamento à vista.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        transactionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
