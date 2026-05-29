package com.gastos.repository;

import com.gastos.domain.RecurringRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface RecurringRuleRepository extends JpaRepository<RecurringRule, UUID> {

    List<RecurringRule> findByActiveTrue();

    List<RecurringRule> findByActiveTrueAndNextDateLessThanEqual(LocalDate date);
}
