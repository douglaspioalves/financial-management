package com.gastos.repository;

import com.gastos.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByDateBetweenOrderByDateAscCreatedAtAsc(LocalDate start, LocalDate end);
}
