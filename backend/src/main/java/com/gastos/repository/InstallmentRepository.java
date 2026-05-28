package com.gastos.repository;

import com.gastos.domain.Installment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InstallmentRepository extends JpaRepository<Installment, UUID> {

    List<Installment> findByTransactionIdOrderByNumberAsc(UUID transactionId);
}
