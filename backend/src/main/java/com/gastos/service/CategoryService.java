package com.gastos.service;

import com.gastos.domain.Category;
import com.gastos.domain.CategoryType;
import com.gastos.dto.category.CategoryResponse;
import com.gastos.dto.category.CreateCategoryRequest;
import com.gastos.dto.category.UpdateCategoryRequest;
import com.gastos.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll(CategoryType type) {
        List<Category> categories;

        if (type == null) {
            categories = categoryRepository.findAllByInactiveFalseOrderByNameAsc();
        } else {
            List<CategoryType> compatibleTypes = switch (type) {
                case EXPENSE -> List.of(CategoryType.EXPENSE, CategoryType.BOTH);
                case INCOME  -> List.of(CategoryType.INCOME, CategoryType.BOTH);
                case BOTH    -> List.of(CategoryType.EXPENSE, CategoryType.INCOME, CategoryType.BOTH);
            };
            categories = categoryRepository.findAllByInactiveFalseAndTypeInOrderByNameAsc(compatibleTypes);
        }

        return categories.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(UUID id) {
        Category category = categoryRepository.findByIdAndInactiveFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada."));
        return toResponse(category);
    }

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCaseAndInactiveFalse(request.name())) {
            throw new IllegalArgumentException("Já existe uma categoria ativa com este nome.");
        }

        Category category = Category.builder()
                .name(request.name())
                .type(request.type())
                .color(request.color())
                .inactive(false)
                .build();

        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    @Transactional
    public CategoryResponse update(UUID id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findByIdAndInactiveFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada."));

        // Optimistic locking: verifica versão manualmente antes de salvar
        if (!category.getVersion().equals(request.version())) {
            throw new org.springframework.orm.ObjectOptimisticLockingFailureException(Category.class, id);
        }

        if (request.name() != null && !request.name().isBlank()) {
            category.setName(request.name());
        }
        if (request.color() != null && !request.color().isBlank()) {
            category.setColor(request.color());
        }

        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Category category = categoryRepository.findByIdAndInactiveFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada."));

        category.setInactive(true);
        categoryRepository.save(category);
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType(),
                category.getColor(),
                category.getVersion()
        );
    }
}
