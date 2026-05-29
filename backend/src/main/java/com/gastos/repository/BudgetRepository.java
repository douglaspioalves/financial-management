package com.gastos.repository;

import com.gastos.domain.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    List<Budget> findByMonth(LocalDate month);

    Optional<Budget> findByCategoryIdAndMonth(UUID categoryId, LocalDate month);

    boolean existsByCategoryIdAndMonth(UUID categoryId, LocalDate month);
}
