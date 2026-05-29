package com.gastos.service;

import com.gastos.domain.Budget;
import com.gastos.domain.Category;
import com.gastos.domain.CategoryType;
import com.gastos.dto.budget.BudgetRequest;
import com.gastos.dto.budget.BudgetResponse;
import com.gastos.repository.BudgetRepository;
import com.gastos.repository.CategoryRepository;
import com.gastos.repository.InstallmentRepository;
import com.gastos.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private InstallmentRepository installmentRepository;

    @InjectMocks
    private BudgetService budgetService;

    private Category expenseCategory;
    private Budget budget;

    private static final LocalDate MAY_2026 = LocalDate.of(2026, 5, 1);

    @BeforeEach
    void setUp() {
        expenseCategory = Category.builder()
                .id(UUID.randomUUID())
                .name("Alimentação")
                .type(CategoryType.EXPENSE)
                .color("#FF5733")
                .inactive(false)
                .version(0L)
                .build();

        budget = Budget.builder()
                .id(UUID.randomUUID())
                .category(expenseCategory)
                .month(MAY_2026)
                .limitAmount(new BigDecimal("1000.00"))
                .version(0L)
                .build();
    }

    // -------------------------------------------------------------------------
    // validateFirstDayOfMonth
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getByMonth lança IllegalArgumentException quando month não é dia 1")
    void getByMonth_whenNotFirstDay_throwsIllegalArgument() {
        LocalDate notFirstDay = LocalDate.of(2026, 5, 15);

        assertThatThrownBy(() -> budgetService.getByMonth(notFirstDay))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("primeiro dia do mês");
    }

    @Test
    @DisplayName("create lança IllegalArgumentException quando month não é dia 1")
    void create_whenMonthNotFirstDay_throwsIllegalArgument() {
        BudgetRequest req = new BudgetRequest(
                expenseCategory.getId(), LocalDate.of(2026, 5, 10),
                new BigDecimal("500.00"), null);

        assertThatThrownBy(() -> budgetService.create(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("primeiro dia do mês");

        verify(budgetRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // getByMonth
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getByMonth retorna lista de BudgetResponse com gasto e status calculados")
    void getByMonth_returnsListWithSpentAndStatus() {
        when(budgetRepository.findByMonth(MAY_2026)).thenReturn(List.of(budget));
        stubSpentCalculation(expenseCategory.getId(), MAY_2026, new BigDecimal("500.00"));

        List<BudgetResponse> result = budgetService.getByMonth(MAY_2026);

        assertThat(result).hasSize(1);
        BudgetResponse response = result.get(0);
        assertThat(response.categoryId()).isEqualTo(expenseCategory.getId());
        assertThat(response.spentAmount()).isEqualByComparingTo("500.00");
        assertThat(response.percentage()).isEqualByComparingTo("50.00");
        assertThat(response.status()).isEqualTo("OK");
        assertThat(response.limitAmount()).isEqualByComparingTo("1000.00");
    }

    // -------------------------------------------------------------------------
    // create
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("create salva e retorna BudgetResponse quando dados são válidos")
    void create_withValidRequest_savesAndReturns() {
        BudgetRequest req = new BudgetRequest(
                expenseCategory.getId(), MAY_2026, new BigDecimal("800.00"), null);

        when(categoryRepository.findByIdAndInactiveFalse(expenseCategory.getId()))
                .thenReturn(Optional.of(expenseCategory));
        when(budgetRepository.existsByCategoryIdAndMonth(expenseCategory.getId(), MAY_2026))
                .thenReturn(false);
        when(budgetRepository.save(any(Budget.class))).thenReturn(budget);
        stubSpentCalculation(expenseCategory.getId(), MAY_2026, BigDecimal.ZERO);

        BudgetResponse result = budgetService.create(req);

        assertThat(result).isNotNull();
        assertThat(result.categoryId()).isEqualTo(expenseCategory.getId());
        assertThat(result.status()).isEqualTo("OK");
        verify(budgetRepository).save(any(Budget.class));
    }

    @Test
    @DisplayName("create lança 409 quando já existe orçamento para a mesma categoria+mês")
    void create_whenBudgetAlreadyExists_throwsConflict() {
        BudgetRequest req = new BudgetRequest(
                expenseCategory.getId(), MAY_2026, new BigDecimal("800.00"), null);

        when(categoryRepository.findByIdAndInactiveFalse(expenseCategory.getId()))
                .thenReturn(Optional.of(expenseCategory));
        when(budgetRepository.existsByCategoryIdAndMonth(expenseCategory.getId(), MAY_2026))
                .thenReturn(true);

        assertThatThrownBy(() -> budgetService.create(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("Já existe um orçamento");

        verify(budgetRepository, never()).save(any());
    }

    @Test
    @DisplayName("create lança EntityNotFoundException quando categoria não existe")
    void create_whenCategoryNotFound_throwsNotFound() {
        UUID unknownCategoryId = UUID.randomUUID();
        BudgetRequest req = new BudgetRequest(
                unknownCategoryId, MAY_2026, new BigDecimal("500.00"), null);

        when(categoryRepository.findByIdAndInactiveFalse(unknownCategoryId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.create(req))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Categoria não encontrada");

        verify(budgetRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("update atualiza limitAmount e retorna response atualizado")
    void update_withValidRequest_updatesLimitAmount() {
        UUID id = budget.getId();
        BudgetRequest req = new BudgetRequest(
                expenseCategory.getId(), MAY_2026, new BigDecimal("2000.00"), 0L);

        when(budgetRepository.findById(id)).thenReturn(Optional.of(budget));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));
        stubSpentCalculation(expenseCategory.getId(), MAY_2026, new BigDecimal("1500.00"));

        BudgetResponse result = budgetService.update(id, req);

        assertThat(result.limitAmount()).isEqualByComparingTo("2000.00");
        assertThat(result.spentAmount()).isEqualByComparingTo("1500.00");
        assertThat(result.status()).isEqualTo("WARNING");
        verify(budgetRepository).save(budget);
    }

    @Test
    @DisplayName("update lança ObjectOptimisticLockingFailureException quando versão diverge")
    void update_withWrongVersion_throwsOptimisticLocking() {
        UUID id = budget.getId();
        // budget está na versão 0; request envia versão 5
        BudgetRequest req = new BudgetRequest(
                expenseCategory.getId(), MAY_2026, new BigDecimal("500.00"), 5L);

        when(budgetRepository.findById(id)).thenReturn(Optional.of(budget));

        assertThatThrownBy(() -> budgetService.update(id, req))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        verify(budgetRepository, never()).save(any());
    }

    @Test
    @DisplayName("update lança EntityNotFoundException quando orçamento não existe")
    void update_whenBudgetNotFound_throwsNotFound() {
        UUID unknownId = UUID.randomUUID();
        BudgetRequest req = new BudgetRequest(
                expenseCategory.getId(), MAY_2026, new BigDecimal("500.00"), 0L);

        when(budgetRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.update(unknownId, req))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Orçamento não encontrado");
    }

    // -------------------------------------------------------------------------
    // delete
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("delete remove o orçamento quando existe")
    void delete_whenBudgetExists_deletesSuccessfully() {
        UUID id = budget.getId();
        when(budgetRepository.findById(id)).thenReturn(Optional.of(budget));

        budgetService.delete(id);

        verify(budgetRepository).delete(budget);
    }

    @Test
    @DisplayName("delete lança EntityNotFoundException quando orçamento não existe")
    void delete_whenBudgetNotFound_throwsNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(budgetRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.delete(unknownId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Orçamento não encontrado");

        verify(budgetRepository, never()).delete(any());
    }

    // -------------------------------------------------------------------------
    // Status calculation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("status OK quando percentual <= 70%")
    void status_whenPercentageUpTo70_returnsOK() {
        when(budgetRepository.findByMonth(MAY_2026)).thenReturn(List.of(budget));
        // 700 / 1000 = 70%
        stubSpentCalculation(expenseCategory.getId(), MAY_2026, new BigDecimal("700.00"));

        BudgetResponse result = budgetService.getByMonth(MAY_2026).get(0);
        assertThat(result.status()).isEqualTo("OK");
        assertThat(result.percentage()).isEqualByComparingTo("70.00");
    }

    @Test
    @DisplayName("status WARNING quando percentual > 70% e <= 100%")
    void status_whenPercentageBetween71And100_returnsWARNING() {
        when(budgetRepository.findByMonth(MAY_2026)).thenReturn(List.of(budget));
        // 900 / 1000 = 90%
        stubSpentCalculation(expenseCategory.getId(), MAY_2026, new BigDecimal("900.00"));

        BudgetResponse result = budgetService.getByMonth(MAY_2026).get(0);
        assertThat(result.status()).isEqualTo("WARNING");
        assertThat(result.percentage()).isEqualByComparingTo("90.00");
    }

    @Test
    @DisplayName("status EXCEEDED quando percentual > 100%")
    void status_whenPercentageAbove100_returnsEXCEEDED() {
        when(budgetRepository.findByMonth(MAY_2026)).thenReturn(List.of(budget));
        // 1200 / 1000 = 120%
        stubSpentCalculation(expenseCategory.getId(), MAY_2026, new BigDecimal("1200.00"));

        BudgetResponse result = budgetService.getByMonth(MAY_2026).get(0);
        assertThat(result.status()).isEqualTo("EXCEEDED");
        assertThat(result.percentage()).isEqualByComparingTo("120.00");
    }

    // -------------------------------------------------------------------------
    // calculateSpent
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("calculateSpent soma despesas à vista com parcelas do mês")
    void calculateSpent_sumsBothNonInstallmentAndInstallments() {
        UUID catId = expenseCategory.getId();

        when(transactionRepository.sumNonInstallmentByTypeAndCategoryAndDateBetween(
                catId, com.gastos.domain.TransactionType.EXPENSE,
                MAY_2026, LocalDate.of(2026, 5, 31)))
                .thenReturn(new BigDecimal("300.00"));
        when(installmentRepository.sumByCategoryAndReferenceMonth(catId, MAY_2026))
                .thenReturn(new BigDecimal("200.00"));

        BigDecimal spent = budgetService.calculateSpent(catId, MAY_2026);

        assertThat(spent).isEqualByComparingTo("500.00");
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private void stubSpentCalculation(UUID categoryId, LocalDate month, BigDecimal nonInstallment) {
        LocalDate end = month.withDayOfMonth(month.lengthOfMonth());
        when(transactionRepository.sumNonInstallmentByTypeAndCategoryAndDateBetween(
                categoryId, com.gastos.domain.TransactionType.EXPENSE, month, end))
                .thenReturn(nonInstallment);
        when(installmentRepository.sumByCategoryAndReferenceMonth(categoryId, month))
                .thenReturn(BigDecimal.ZERO);
    }
}
