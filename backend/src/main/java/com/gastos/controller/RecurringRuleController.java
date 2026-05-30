package com.gastos.controller;

import com.gastos.dto.recurring.RecurringRuleRequest;
import com.gastos.dto.recurring.RecurringRuleResponse;
import com.gastos.service.RecurringRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recurring-rules")
@RequiredArgsConstructor
public class RecurringRuleController {

    private final RecurringRuleService recurringRuleService;

    /**
     * Lista todas as regras de lançamento recorrente ativas.
     */
    @GetMapping
    public ResponseEntity<List<RecurringRuleResponse>> getAll() {
        return ResponseEntity.ok(recurringRuleService.getAll());
    }

    /**
     * Cria uma nova regra de lançamento recorrente.
     */
    @PostMapping
    public ResponseEntity<RecurringRuleResponse> create(@Valid @RequestBody RecurringRuleRequest request) {
        RecurringRuleResponse response = recurringRuleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Desativa uma regra recorrente (soft delete — não apaga o registro).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        recurringRuleService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
