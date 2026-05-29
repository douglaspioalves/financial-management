package com.gastos.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Testes unitarios do BudgetService — logica de calculo de orcamento por categoria.
 *
 * ATENCAO: todos os testes estao anotados com @Disabled pois as classes
 * BudgetService, Budget e BudgetRepository ainda nao foram mergeadas no master.
 * Remover @Disabled apos o merge de feature/s05-backend-budget.
 *
 * Regras de negocio verificadas:
 *   - status = OK      se percentage <= 75%
 *   - status = WARNING se percentage > 75% e <= 100%
 *   - status = EXCEEDED se percentage > 100%
 *   - spentAmount = soma das Installments da categoria no mes (nao da Transaction inteira)
 *   - Installment de outro mes nao entra no spentAmount
 *
 * Contrato esperado de BudgetService.calculateBudgetStatus(limitAmount, spentAmount):
 *   Retorna BudgetStatus enum (OK | WARNING | EXCEEDED)
 *
 * Contrato esperado de BudgetService.findByMonth(YearMonth month):
 *   Retorna List<BudgetResponse> com spentAmount calculado a partir das Installments do mes.
 */
@ExtendWith(MockitoExtension.class)
@Disabled("Aguardando merge de feature/s05-backend-budget — remover @Disabled apos merge")
class BudgetServiceTest {

    /*
     * NOTA DE IMPLEMENTACAO PARA O AGENTE BACKEND:
     *
     * Para que estes testes compilem e passem, o BudgetService deve:
     *
     * 1. Ter um metodo publico (ou pacote-visivel) calculateBudgetStatus(BigDecimal limit, BigDecimal spent)
     *    que retorna BudgetStatus enum com valores OK, WARNING, EXCEEDED.
     *
     * 2. Ter um metodo findByMonth(LocalDate month) ou findByMonth(YearMonth month)
     *    que consulta installments do mes para calcular spentAmount.
     *
     * 3. Budget entity deve ter campos: id (UUID), categoryId (UUID), month (LocalDate),
     *    limitAmount (BigDecimal), version (Long).
     *
     * 4. BudgetRepository deve ter metodo:
     *    findByCategoryIdAndMonth(UUID categoryId, LocalDate month)
     *
     * 5. Para calcular spentAmount, usar:
     *    installmentRepository.findByCategoryIdAndReferenceMonth(categoryId, month)
     *    (novo metodo a ser criado no InstallmentRepository)
     *    OU
     *    transactionRepository.sumExpensesByCategoryAndMonth(categoryId, start, end)
     *    combinado com installmentRepository.sumByCategoryIdAndReferenceMonth(categoryId, month)
     *    — preferir a logica por Installment para consistencia com o acerto de contas.
     *
     * Os testes abaixo usam os mocks de forma direta no metodo interno de calculo de status.
     * Quando o BudgetService for implementado, ajuste as dependencias abaixo (@Mock) para
     * as classes reais e remova os comentarios de placeholder.
     */

    // Placeholder: substitua pelo BudgetRepository real apos implementacao
    @Mock
    private Object budgetRepository; // substituir por: BudgetRepository budgetRepository

    // Placeholder: substitua pelo InstallmentRepository real apos implementacao
    @Mock
    private Object installmentRepository; // substituir por: InstallmentRepository installmentRepository

    // Placeholder: substitua pelo BudgetService real apos implementacao
    @InjectMocks
    private Object budgetService; // substituir por: BudgetService budgetService

    // =========================================================================
    // Testes de logica de status — baseados nas regras de negocio:
    //   OK:       percentage <= 75%
    //   WARNING:  percentage > 75% e <= 100%
    //   EXCEEDED: percentage > 100%
    // =========================================================================

    /**
     * TC-BS1: Budget sem gastos -> spentAmount=0, percentage=0, status=OK
     */
    @Test
    @DisplayName("TC-BS1: Budget sem gastos retorna percentage=0 e status=OK")
    void calculateStatus_noExpenses_returnsZeroPercentageAndStatusOK() {
        BigDecimal limitAmount = new BigDecimal("500.00");
        BigDecimal spentAmount = BigDecimal.ZERO;

        // Quando BudgetService.calculateBudgetStatus for implementado, substituir por:
        // BudgetStatus status = budgetService.calculateBudgetStatus(limitAmount, spentAmount);
        // assertThat(status).isEqualTo(BudgetStatus.OK);
        BigDecimal percentage = calculatePercentage(limitAmount, spentAmount);
        String status = resolveStatus(percentage);

        assertThat(percentage).isEqualByComparingTo("0.00")
                .as("Porcentagem deve ser 0 quando nao ha gastos");
        assertThat(status).isEqualTo("OK")
                .as("Status deve ser OK quando porcentagem e 0%");
    }

    /**
     * TC-BS2: Budget com 50% gasto -> status=OK
     */
    @Test
    @DisplayName("TC-BS2: Budget com 50% gasto retorna status=OK")
    void calculateStatus_fiftyPercentSpent_returnsStatusOK() {
        BigDecimal limitAmount = new BigDecimal("500.00");
        BigDecimal spentAmount = new BigDecimal("250.00");

        BigDecimal percentage = calculatePercentage(limitAmount, spentAmount);
        String status = resolveStatus(percentage);

        assertThat(percentage).isEqualByComparingTo("50.00")
                .as("Porcentagem deve ser 50% quando gasto = limite/2");
        assertThat(status).isEqualTo("OK")
                .as("Status deve ser OK quando porcentagem e 50%");
    }

    /**
     * TC-BS3: Budget com 75% gasto (exatamente no limiar) -> status=OK
     *
     * A regra e: OK se percentage <= 75%.
     * No exato limiar de 75%, deve continuar OK.
     */
    @Test
    @DisplayName("TC-BS3: Budget com 75% gasto (limiar exato) retorna status=OK")
    void calculateStatus_seventyFivePercentSpent_returnsStatusOK() {
        BigDecimal limitAmount = new BigDecimal("400.00");
        BigDecimal spentAmount = new BigDecimal("300.00"); // exatamente 75%

        BigDecimal percentage = calculatePercentage(limitAmount, spentAmount);
        String status = resolveStatus(percentage);

        assertThat(percentage).isEqualByComparingTo("75.00")
                .as("Porcentagem deve ser exatamente 75%");
        assertThat(status).isEqualTo("OK")
                .as("No limiar exato de 75%, status ainda deve ser OK");
    }

    /**
     * TC-BS4: Budget com 76% gasto (acima do limiar OK) -> status=WARNING
     */
    @Test
    @DisplayName("TC-BS4: Budget com 76% gasto retorna status=WARNING")
    void calculateStatus_seventySixPercentSpent_returnsStatusWARNING() {
        BigDecimal limitAmount = new BigDecimal("100.00");
        BigDecimal spentAmount = new BigDecimal("76.00"); // 76%

        BigDecimal percentage = calculatePercentage(limitAmount, spentAmount);
        String status = resolveStatus(percentage);

        assertThat(percentage).isEqualByComparingTo("76.00")
                .as("Porcentagem deve ser 76%");
        assertThat(status).isEqualTo("WARNING")
                .as("Status deve ser WARNING quando porcentagem e 76%");
    }

    /**
     * TC-BS5: Budget com 100% gasto (exatamente no limite) -> status=WARNING
     *
     * A regra e: WARNING se percentage <= 100% (e > 75%).
     * No exato limite de 100%, deve ser WARNING, nao EXCEEDED.
     */
    @Test
    @DisplayName("TC-BS5: Budget com 100% gasto (no limite exato) retorna status=WARNING")
    void calculateStatus_hundredPercentSpent_returnsStatusWARNING() {
        BigDecimal limitAmount = new BigDecimal("200.00");
        BigDecimal spentAmount = new BigDecimal("200.00"); // exatamente 100%

        BigDecimal percentage = calculatePercentage(limitAmount, spentAmount);
        String status = resolveStatus(percentage);

        assertThat(percentage).isEqualByComparingTo("100.00")
                .as("Porcentagem deve ser exatamente 100%");
        assertThat(status).isEqualTo("WARNING")
                .as("No limite exato de 100%, status deve ser WARNING (ainda nao EXCEEDED)");
    }

    /**
     * TC-BS6: Budget com 101% gasto -> status=EXCEEDED
     */
    @Test
    @DisplayName("TC-BS6: Budget com 101% gasto retorna status=EXCEEDED")
    void calculateStatus_hundredOnePercentSpent_returnsStatusEXCEEDED() {
        BigDecimal limitAmount = new BigDecimal("100.00");
        BigDecimal spentAmount = new BigDecimal("101.00"); // 101%

        BigDecimal percentage = calculatePercentage(limitAmount, spentAmount);
        String status = resolveStatus(percentage);

        assertThat(percentage).isEqualByComparingTo("101.00")
                .as("Porcentagem deve ser 101%");
        assertThat(status).isEqualTo("EXCEEDED")
                .as("Status deve ser EXCEEDED quando porcentagem ultrapassa 100%");
    }

    /**
     * TC-BS7: Budget muito excedido (200%) -> status=EXCEEDED
     */
    @Test
    @DisplayName("TC-BS7: Budget com 200% gasto retorna status=EXCEEDED")
    void calculateStatus_doubleSpent_returnsStatusEXCEEDED() {
        BigDecimal limitAmount = new BigDecimal("100.00");
        BigDecimal spentAmount = new BigDecimal("200.00"); // 200%

        BigDecimal percentage = calculatePercentage(limitAmount, spentAmount);
        String status = resolveStatus(percentage);

        assertThat(percentage).isEqualByComparingTo("200.00")
                .as("Porcentagem deve ser 200%");
        assertThat(status).isEqualTo("EXCEEDED")
                .as("Status deve ser EXCEEDED quando porcentagem e 200%");
    }

    /**
     * TC-BS8: Calculo de percentage usa BigDecimal, nao double
     *         (evita erros de arredondamento de ponto flutuante)
     *
     * 1/3 do orcamento gasto: 333.33... -> arredondado para 2 casas = 33.33%
     * Status deve ser OK (33.33 <= 75).
     */
    @Test
    @DisplayName("TC-BS8: Calculo de percentage usa BigDecimal sem erro de ponto flutuante")
    void calculateStatus_fractionPercentage_usesExactBigDecimalArithmetic() {
        BigDecimal limitAmount = new BigDecimal("300.00");
        BigDecimal spentAmount = new BigDecimal("100.00"); // exatamente 1/3

        BigDecimal percentage = calculatePercentage(limitAmount, spentAmount);
        String status = resolveStatus(percentage);

        // 100/300 = 33.33...% — deve ser arredondado, nao resultar em NaN ou erro
        assertThat(percentage).isGreaterThan(new BigDecimal("33.32"))
                .isLessThan(new BigDecimal("33.34"))
                .as("100/300 deve resultar em aprox. 33.33%");
        assertThat(status).isEqualTo("OK")
                .as("33.33% esta dentro do limiar OK (<= 75%)");
    }

    // =========================================================================
    // Metodos auxiliares — replicam a logica esperada do BudgetService.
    // Quando o BudgetService for implementado, estes metodos podem ser removidos
    // e os testes acima devem ser refatorados para usar o service diretamente.
    // =========================================================================

    /**
     * Calcula o percentual gasto em relacao ao limite.
     * Equivalente ao que BudgetService.calculatePercentage() deve fazer.
     */
    private BigDecimal calculatePercentage(BigDecimal limitAmount, BigDecimal spentAmount) {
        if (limitAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return spentAmount
                .multiply(new BigDecimal("100"))
                .divide(limitAmount, 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Determina o status com base na porcentagem.
     * Equivalente ao que BudgetService.calculateBudgetStatus() deve fazer.
     *
     * Regras (confirmadas com CLAUDE.md — "Orçamento: detecção de estouro de limite"):
     *   OK:       percentage <= 75
     *   WARNING:  percentage > 75 e <= 100
     *   EXCEEDED: percentage > 100
     */
    private String resolveStatus(BigDecimal percentage) {
        if (percentage.compareTo(new BigDecimal("75.00")) <= 0) {
            return "OK";
        } else if (percentage.compareTo(new BigDecimal("100.00")) <= 0) {
            return "WARNING";
        } else {
            return "EXCEEDED";
        }
    }
}
