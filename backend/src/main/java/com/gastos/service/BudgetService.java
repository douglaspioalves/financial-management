package com.gastos.service;

import com.gastos.domain.Budget;
import com.gastos.domain.Category;
import com.gastos.domain.TransactionType;
import com.gastos.dto.budget.BudgetRequest;
import com.gastos.dto.budget.BudgetResponse;
import com.gastos.repository.BudgetRepository;
import com.gastos.repository.CategoryRepository;
import com.gastos.repository.InstallmentRepository;
import com.gastos.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final InstallmentRepository installmentRepository;

    // -------------------------------------------------------------------------
    // Consulta
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<BudgetResponse> getByMonth(LocalDate month) {
        validateFirstDayOfMonth(month);

        return budgetRepository.findByMonth(month)
                .stream()
                .map(budget -> toResponse(budget, calculateSpent(budget.getCategory().getId(), month)))
                .toList();
    }

    // -------------------------------------------------------------------------
    // Criação
    // -------------------------------------------------------------------------

    @Transactional
    public BudgetResponse create(BudgetRequest request) {
        validateFirstDayOfMonth(request.month());

        Category category = categoryRepository.findByIdAndInactiveFalse(request.categoryId())
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada."));

        if (budgetRepository.existsByCategoryIdAndMonth(request.categoryId(), request.month())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe um orçamento para esta categoria neste mês.");
        }

        Budget budget = Budget.builder()
                .category(category)
                .month(request.month())
                .limitAmount(request.limitAmount())
                .build();

        Budget saved = budgetRepository.save(budget);
        BigDecimal spent = calculateSpent(saved.getCategory().getId(), saved.getMonth());
        return toResponse(saved, spent);
    }

    // -------------------------------------------------------------------------
    // Atualização
    // -------------------------------------------------------------------------

    @Transactional
    public BudgetResponse update(UUID id, BudgetRequest request) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Orçamento não encontrado."));

        // Optimistic locking: verifica versão manualmente
        if (!budget.getVersion().equals(request.version())) {
            throw new ObjectOptimisticLockingFailureException(Budget.class, id);
        }

        budget.setLimitAmount(request.limitAmount());

        Budget saved = budgetRepository.save(budget);
        BigDecimal spent = calculateSpent(saved.getCategory().getId(), saved.getMonth());
        return toResponse(saved, spent);
    }

    // -------------------------------------------------------------------------
    // Remoção
    // -------------------------------------------------------------------------

    @Transactional
    public void delete(UUID id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Orçamento não encontrado."));
        budgetRepository.delete(budget);
    }

    // -------------------------------------------------------------------------
    // Lógica de cálculo de gasto real
    // -------------------------------------------------------------------------

    /**
     * Calcula o gasto real de uma categoria em um determinado mês.
     *
     * <p>Regra:
     * <ol>
     *   <li>Despesas à vista (installments_total = 1) do mês, pela data da transação.</li>
     *   <li>Parcelas de compras parceladas (Installment) cujo reference_month é o mês.</li>
     * </ol>
     * <p>Equivalente à lógica do Dashboard: cash/pix/debit/transfer = pela data;
     * credit parcelado = pelo reference_month da parcela.
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateSpent(UUID categoryId, LocalDate month) {
        YearMonth yearMonth = YearMonth.from(month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        // Despesas à vista (não-parceladas)
        BigDecimal nonInstallmentSpent = transactionRepository
                .sumNonInstallmentByTypeAndCategoryAndDateBetween(
                        categoryId, TransactionType.EXPENSE, start, end);

        // Parcelas de compras parceladas que caem no mês
        BigDecimal installmentSpent = installmentRepository
                .sumByCategoryAndReferenceMonth(categoryId, month);

        return nonInstallmentSpent.add(installmentSpent).setScale(2, RoundingMode.HALF_UP);
    }

    // -------------------------------------------------------------------------
    // Auxiliares
    // -------------------------------------------------------------------------

    private void validateFirstDayOfMonth(LocalDate date) {
        if (date == null || date.getDayOfMonth() != 1) {
            throw new IllegalArgumentException(
                    "O mês deve ser informado como o primeiro dia do mês (ex.: 2026-05-01).");
        }
    }

    private BudgetResponse toResponse(Budget budget, BigDecimal spent) {
        BigDecimal limit = budget.getLimitAmount();
        BigDecimal percentage;
        String status;

        if (limit.compareTo(BigDecimal.ZERO) > 0) {
            percentage = spent
                    .multiply(BigDecimal.valueOf(100))
                    .divide(limit, 2, RoundingMode.HALF_UP);
        } else {
            percentage = BigDecimal.ZERO;
        }

        if (percentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            status = "EXCEEDED";
        } else if (percentage.compareTo(BigDecimal.valueOf(70)) > 0) {
            status = "WARNING";
        } else {
            status = "OK";
        }

        return new BudgetResponse(
                budget.getId(),
                budget.getCategory().getId(),
                budget.getCategory().getName(),
                budget.getCategory().getColor(),
                budget.getMonth(),
                limit,
                spent,
                percentage,
                status,
                budget.getVersion()
        );
    }
}
