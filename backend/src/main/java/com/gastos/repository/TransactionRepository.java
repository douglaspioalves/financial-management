package com.gastos.repository;

import com.gastos.domain.Transaction;
import com.gastos.domain.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByDateBetweenOrderByDateAscCreatedAtAsc(LocalDate start, LocalDate end);

    boolean existsByCardId(UUID cardId);

    /**
     * Soma despesas à vista (não-parceladas: installments_total = 1) de uma categoria
     * em um mês (pela data da transação).
     * Usado no cálculo do gasto real de orçamento.
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.category.id = :categoryId " +
           "AND t.type = :type " +
           "AND t.installmentsTotal = 1 " +
           "AND t.date >= :startDate " +
           "AND t.date <= :endDate")
    BigDecimal sumNonInstallmentByTypeAndCategoryAndDateBetween(
            @Param("categoryId") UUID categoryId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
