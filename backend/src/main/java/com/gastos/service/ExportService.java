package com.gastos.service;

import com.gastos.domain.Installment;
import com.gastos.domain.SplitRule;
import com.gastos.domain.Transaction;
import com.gastos.repository.InstallmentRepository;
import com.gastos.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final TransactionRepository transactionRepository;
    private final InstallmentRepository installmentRepository;

    // Cabeçalho em pt-br conforme contrato
    private static final String[] HEADERS = {
        "Data", "Descrição", "Categoria", "Quem Pagou",
        "Tipo", "Valor", "Divisão", "Parcela"
    };

    /**
     * Exporta os lançamentos do mês em formato CSV (UTF-8, separador vírgula).
     */
    @Transactional(readOnly = true)
    public byte[] exportCsv(YearMonth month) {
        List<ExportRow> rows = buildRows(month);

        StringBuilder sb = new StringBuilder();
        // BOM UTF-8 para compatibilidade com Excel
        sb.append('﻿');
        sb.append(String.join(",", HEADERS)).append("\n");

        for (ExportRow row : rows) {
            sb.append(escapeCsv(row.date().toString())).append(",");
            sb.append(escapeCsv(row.description())).append(",");
            sb.append(escapeCsv(row.category())).append(",");
            sb.append(escapeCsv(row.paidBy())).append(",");
            sb.append(escapeCsv(row.type())).append(",");
            sb.append(row.amount().toPlainString()).append(",");
            sb.append(escapeCsv(row.splitLabel())).append(",");
            sb.append(escapeCsv(row.installmentLabel())).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Exporta os lançamentos do mês em formato XLSX (Apache POI).
     * Primeira linha com cabeçalho em negrito.
     */
    @Transactional(readOnly = true)
    public byte[] exportXlsx(YearMonth month) {
        List<ExportRow> rows = buildRows(month);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Lançamentos " + month);

            // Estilo do cabeçalho: negrito com fundo cinza claro
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Cria linha de cabeçalho
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                var cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Cria linhas de dados
            int rowIndex = 1;
            for (ExportRow row : rows) {
                Row dataRow = sheet.createRow(rowIndex++);
                dataRow.createCell(0).setCellValue(row.date().toString());
                dataRow.createCell(1).setCellValue(row.description() != null ? row.description() : "");
                dataRow.createCell(2).setCellValue(row.category());
                dataRow.createCell(3).setCellValue(row.paidBy());
                dataRow.createCell(4).setCellValue(row.type());
                // Valor como número (BigDecimal → double é aceitável apenas para serialização XLS)
                dataRow.createCell(5).setCellValue(row.amount().doubleValue());
                dataRow.createCell(6).setCellValue(row.splitLabel());
                dataRow.createCell(7).setCellValue(row.installmentLabel());
            }

            // Ajusta largura das colunas automaticamente
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao gerar arquivo XLSX.", e);
        }
    }

    // --- internos ---

    /**
     * Consolida todas as linhas de exportação para o mês:
     * 1. Transações à vista (despesas não-parceladas + receitas) — pela data da transação.
     * 2. Parcelas de despesas — pelo referenceMonth.
     *
     * Ordenação: data ASC, descrição ASC.
     */
    private List<ExportRow> buildRows(YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        List<ExportRow> rows = new ArrayList<>();

        // Despesas à vista (não parceladas)
        List<Transaction> cashExpenses = transactionRepository.findCashExpensesByMonth(start, end);
        for (Transaction t : cashExpenses) {
            rows.add(toRow(t, t.getDate(), t.getAmount(), null, null));
        }

        // Receitas
        List<Transaction> incomes = transactionRepository.findIncomesByMonth(start, end);
        for (Transaction t : incomes) {
            rows.add(toRow(t, t.getDate(), t.getAmount(), null, null));
        }

        // Parcelas de despesas
        List<Installment> installments = installmentRepository.findExpenseInstallmentsByMonth(start);
        for (Installment i : installments) {
            Transaction t = i.getTransaction();
            rows.add(toRow(t, i.getReferenceMonth(), i.getAmount(),
                    i.getNumber(), t.getInstallmentsTotal()));
        }

        // Ordena por data ASC, depois descrição ASC
        rows.sort(Comparator.comparing(ExportRow::date)
                .thenComparing(r -> r.description() != null ? r.description() : ""));

        return rows;
    }

    /**
     * Converte uma Transaction (com data e valor possivelmente vindos da Installment)
     * em uma linha de exportação.
     *
     * @param t                 Transação pai
     * @param date              Data efetiva (da transação ou da parcela)
     * @param amount            Valor efetivo
     * @param installmentNumber Número da parcela atual (null se à vista)
     * @param installmentsTotal Total de parcelas (null se à vista)
     */
    private ExportRow toRow(Transaction t, LocalDate date, BigDecimal amount,
                            Integer installmentNumber, Integer installmentsTotal) {
        String type = t.getType().name().equals("INCOME") ? "Receita" : "Despesa";
        String splitLabel = splitRuleLabel(t.getSplitRule());

        String installmentLabel;
        if (installmentNumber != null && installmentsTotal != null && installmentsTotal > 1) {
            installmentLabel = installmentNumber + "/" + installmentsTotal;
        } else {
            installmentLabel = "À vista";
        }

        return new ExportRow(
                date,
                t.getDescription(),
                t.getCategory().getName(),
                t.getPaidByPerson().getName(),
                type,
                amount,
                splitLabel,
                installmentLabel
        );
    }

    /**
     * Retorna o rótulo em pt-br da regra de divisão.
     */
    private String splitRuleLabel(SplitRule rule) {
        return switch (rule) {
            case FIFTY_FIFTY -> "50/50";
            case PERSON_A -> "Pessoa A";
            case PERSON_B -> "Pessoa B";
            case PROPORTIONAL -> "Proporcional";
        };
    }

    /**
     * Escapa um campo para CSV: envolve em aspas duplas se contiver vírgula, aspas ou quebra de linha.
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Record interno com os 8 campos da linha de exportação.
     */
    public record ExportRow(
            LocalDate date,
            String description,
            String category,
            String paidBy,
            String type,
            BigDecimal amount,
            String splitLabel,
            String installmentLabel
    ) {}
}
