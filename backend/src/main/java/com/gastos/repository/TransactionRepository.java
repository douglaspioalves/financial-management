package com.gastos.repository;

import com.gastos.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByDateBetweenOrderByDateAscCreatedAtAsc(LocalDate start, LocalDate end);

    boolean existsByCardId(UUID cardId);

    /**
     * Retorna despesas não-parceladas do mês (cash, debit, pix, transfer, ou crédito à vista).
     * Carrega category e paidByPerson via JOIN FETCH para evitar N+1.
     */
    @Query("SELECT t FROM Transaction t " +
           "JOIN FETCH t.category " +
           "JOIN FETCH t.paidByPerson " +
           "WHERE t.type = 'EXPENSE' " +
           "AND (t.paymentMethod <> 'CREDIT' OR t.installmentsTotal = 1) " +
           "AND t.date BETWEEN :start AND :end")
    List<Transaction> findCashExpensesByMonth(@Param("start") LocalDate start,
                                              @Param("end") LocalDate end);

    /**
     * Retorna receitas do mês.
     * Carrega category e paidByPerson via JOIN FETCH para evitar N+1.
     */
    @Query("SELECT t FROM Transaction t " +
           "JOIN FETCH t.category " +
           "JOIN FETCH t.paidByPerson " +
           "WHERE t.type = 'INCOME' " +
           "AND t.date BETWEEN :start AND :end")
    List<Transaction> findIncomesByMonth(@Param("start") LocalDate start,
                                         @Param("end") LocalDate end);

    /**
     * Retorna as N transações mais recentes do mês (para recentTransactions no dashboard).
     * Inclui receitas e despesas, ordenadas por date DESC depois por createdAt DESC.
     * Carrega category e paidByPerson via JOIN FETCH para evitar N+1.
     */
    @Query("SELECT t FROM Transaction t " +
           "JOIN FETCH t.category " +
           "JOIN FETCH t.paidByPerson " +
           "WHERE t.date BETWEEN :start AND :end " +
           "ORDER BY t.date DESC, t.createdAt DESC")
    List<Transaction> findRecentByMonth(@Param("start") LocalDate start,
                                        @Param("end") LocalDate end);
}
