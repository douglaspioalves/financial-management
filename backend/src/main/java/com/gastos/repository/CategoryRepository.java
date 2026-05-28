package com.gastos.repository;

import com.gastos.domain.Category;
import com.gastos.domain.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findAllByInactiveFalseOrderByNameAsc();

    List<Category> findAllByInactiveFalseAndTypeInOrderByNameAsc(List<CategoryType> types);

    Optional<Category> findByIdAndInactiveFalse(UUID id);

    boolean existsByNameIgnoreCaseAndInactiveFalse(String name);
}
