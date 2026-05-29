package com.gastos.controller;

import com.gastos.dto.dashboard.DashboardResponse;
import com.gastos.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * GET /api/dashboard?month=yyyy-MM
     *
     * Retorna o resumo financeiro do mês: totais de receita e despesa,
     * comparativo com mês anterior, gastos por categoria e transações recentes.
     *
     * Se o parâmetro month não for informado, utiliza o mês corrente.
     *
     * Requer autenticação JWT.
     */
    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(
            @RequestParam(name = "month", required = false)
            @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {

        if (month == null) {
            month = YearMonth.now();
        }

        DashboardResponse response = dashboardService.calculate(month);
        return ResponseEntity.ok(response);
    }
}
