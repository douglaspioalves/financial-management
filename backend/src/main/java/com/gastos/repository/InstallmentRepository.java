package com.gastos.repository;

import com.gastos.domain.Installment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InstallmentRepository extends JpaRepository<Installment, UUID> {

    List<Installment> findByTransactionIdOrderByNumberAsc(UUID transactionId);

    /**
     * Soma o valor de parcelas de despesas de uma categoria que caem em um mês
     * específico (referenceMonth). Usado no cálculo do gasto real de orçamento.
     */
    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Installment i " +
           "WHERE i.transaction.category.id = :categoryId " +
           "AND i.transaction.type = com.gastos.domain.TransactionType.EXPENSE " +
           "AND i.referenceMonth = :referenceMonth")
    BigDecimal sumByCategoryAndReferenceMonth(
            @Param("categoryId") UUID categoryId,
            @Param("referenceMonth") LocalDate referenceMonth);
}
