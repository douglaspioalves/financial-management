package com.gastos.repository;

import com.gastos.domain.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID> {

    List<Card> findAllByOrderByNameAsc();

    boolean existsByOwnerId(UUID ownerId);
}
