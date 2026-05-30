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
     * Retorna receitas individuais do mês — type=INCOME com splitRule PERSON_A ou PERSON_B.
     * Usadas no cálculo da proporção do acerto de contas (receitas FIFTY_FIFTY excluídas).
     */
    @Query("SELECT t FROM Transaction t " +
           "JOIN FETCH t.category " +
           "JOIN FETCH t.paidByPerson " +
           "WHERE t.type = 'INCOME' " +
           "AND (t.splitRule = 'PERSON_A' OR t.splitRule = 'PERSON_B') " +
           "AND t.date BETWEEN :start AND :end")
    List<Transaction> findIndividualIncomesByMonth(@Param("start") LocalDate start,
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

    /**
     * Retorna receitas individuais do mês — transações type=INCOME com splitRule PERSON_A ou PERSON_B.
     * Essas receitas são usadas para calcular a proporção no acerto de contas.
     * Carrega category e paidByPerson via JOIN FETCH para evitar N+1.
     */
    @Query("SELECT t FROM Transaction t " +
           "JOIN FETCH t.category " +
           "JOIN FETCH t.paidByPerson " +
           "WHERE t.type = 'INCOME' " +
           "AND (t.splitRule = 'PERSON_A' OR t.splitRule = 'PERSON_B') " +
           "AND t.date BETWEEN :start AND :end")
    List<Transaction> findIndividualIncomesByMonth(@Param("start") LocalDate start,
                                                   @Param("end") LocalDate end);

    /**
     * Verifica se já existe uma transação com os mesmos campos-chave para idempotência
     * do job de lançamentos recorrentes (evita duplicata se o job rodar mais de uma vez).
     */
    boolean existsByDescriptionAndAmountAndDateAndCategoryId(
            String description,
            java.math.BigDecimal amount,
            java.time.LocalDate date,
            UUID categoryId);

}
