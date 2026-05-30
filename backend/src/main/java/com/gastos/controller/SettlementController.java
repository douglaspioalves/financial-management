package com.gastos.controller;

import com.gastos.dto.settlement.SettlementResponse;
import com.gastos.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

/**
 * Endpoint de acerto de contas.
 *
 * GET /api/settlement?month=yyyy-MM
 *
 * Requer autenticação JWT.
 * Retorna o acerto de contas do mês: quem pagou, quem deveria pagar,
 * saldo e quem deve quanto a quem.
 */
@RestController
@RequestMapping("/api/settlement")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    /**
     * Calcula o acerto de contas do mês informado.
     *
     * @param month mês no formato yyyy-MM (ex.: 2026-05). Se não informado, usa o mês corrente.
     * @return acerto de contas completo do mês
     */
    @GetMapping
    public ResponseEntity<SettlementResponse> getSettlement(
            @RequestParam(name = "month", required = false)
            @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {

        if (month == null) {
            month = YearMonth.now();
        }

        SettlementResponse response = settlementService.calculate(month);
        return ResponseEntity.ok(response);
    }
}
