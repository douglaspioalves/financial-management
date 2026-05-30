package com.gastos.controller;

import com.gastos.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.YearMonth;

/**
 * Controlador de exportação de lançamentos.
 *
 * GET /api/export?month=yyyy-MM&format=csv|xlsx
 *
 * Requer autenticação JWT (sem exceção no SecurityConfig).
 */
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    @GetMapping
    public ResponseEntity<byte[]> export(
            @RequestParam(name = "month", required = false)
            @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam(name = "format", required = false) String format) {

        // Se month não informado, usa o mês corrente
        if (month == null) {
            month = YearMonth.now();
        }

        // Valida o formato solicitado
        if (format == null || (!format.equalsIgnoreCase("csv") && !format.equalsIgnoreCase("xlsx"))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Parâmetro 'format' é obrigatório e deve ser 'csv' ou 'xlsx'."
            );
        }

        String monthStr = month.toString(); // "yyyy-MM"

        if (format.equalsIgnoreCase("csv")) {
            byte[] content = exportService.exportCsv(month);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"gastos-" + monthStr + ".csv\"");
            return ResponseEntity.ok().headers(headers).body(content);
        } else {
            // xlsx
            byte[] content = exportService.exportXlsx(month);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"gastos-" + monthStr + ".xlsx\"");
            return ResponseEntity.ok().headers(headers).body(content);
        }
    }
}
