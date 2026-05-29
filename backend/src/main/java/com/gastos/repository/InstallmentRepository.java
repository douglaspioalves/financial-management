package com.gastos.repository;

import com.gastos.domain.Installment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InstallmentRepository extends JpaRepository<Installment, UUID> {

    List<Installment> findByTransactionIdOrderByNumberAsc(UUID transactionId);

    /**
     * Retorna parcelas de despesas de um determinado mês de referência.
     * O parâmetro month deve ser o primeiro dia do mês (ex.: 2026-05-01).
     * Carrega transaction e category via JOIN FETCH para evitar N+1.
     */
    @Query("SELECT i FROM Installment i " +
           "JOIN FETCH i.transaction t " +
           "JOIN FETCH t.category " +
           "JOIN FETCH t.paidByPerson " +
           "WHERE i.referenceMonth = :month " +
           "AND t.type = 'EXPENSE'")
    List<Installment> findExpenseInstallmentsByMonth(@Param("month") LocalDate month);
}
