package com.gastos.service;

import com.gastos.domain.Category;
import com.gastos.domain.CategoryType;
import com.gastos.dto.category.CategoryResponse;
import com.gastos.dto.category.CreateCategoryRequest;
import com.gastos.dto.category.UpdateCategoryRequest;
import com.gastos.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category expenseCategory;
    private Category incomeCategory;
    private Category bothCategory;

    @BeforeEach
    void setUp() {
        expenseCategory = Category.builder()
                .id(UUID.randomUUID())
                .name("Alimentação")
                .type(CategoryType.EXPENSE)
                .color("#FF5733")
                .inactive(false)
                .version(0L)
                .build();

        incomeCategory = Category.builder()
                .id(UUID.randomUUID())
                .name("Salário")
                .type(CategoryType.INCOME)
                .color("#33FF57")
                .inactive(false)
                .version(0L)
                .build();

        bothCategory = Category.builder()
                .id(UUID.randomUUID())
                .name("Investimentos")
                .type(CategoryType.BOTH)
                .color("#5733FF")
                .inactive(false)
                .version(0L)
                .build();
    }

    // -------------------------------------------------------------------------
    // findAll
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("findAll(null) returns all active categories")
    void findAll_withNullType_returnsAllActiveCategories() {
        when(categoryRepository.findAllByInactiveFalseOrderByNameAsc())
                .thenReturn(List.of(expenseCategory, incomeCategory, bothCategory));

        List<CategoryResponse> result = categoryService.findAll(null);

        assertThat(result).hasSize(3)
                .as("Deve retornar todas as categorias ativas quando tipo é nulo");
        verify(categoryRepository).findAllByInactiveFalseOrderByNameAsc();
        verify(categoryRepository, never()).findAllByInactiveFalseAndTypeInOrderByNameAsc(any());
    }

    @Test
    @DisplayName("findAll(EXPENSE) returns EXPENSE and BOTH categories")
    void findAll_withExpenseType_returnsExpenseAndBothCategories() {
        when(categoryRepository.findAllByInactiveFalseAndTypeInOrderByNameAsc(
                List.of(CategoryType.EXPENSE, CategoryType.BOTH)))
                .thenReturn(List.of(expenseCategory, bothCategory));

        List<CategoryResponse> result = categoryService.findAll(CategoryType.EXPENSE);

        assertThat(result).hasSize(2)
                .as("Deve retornar apenas categorias EXPENSE e BOTH ao filtrar por EXPENSE");
        assertThat(result).extracting(CategoryResponse::type)
                .containsExactlyInAnyOrder(CategoryType.EXPENSE, CategoryType.BOTH);
        verify(categoryRepository, never()).findAllByInactiveFalseOrderByNameAsc();
    }

    @Test
    @DisplayName("findAll(INCOME) returns INCOME and BOTH categories")
    void findAll_withIncomeType_returnsIncomeAndBothCategories() {
        when(categoryRepository.findAllByInactiveFalseAndTypeInOrderByNameAsc(
                List.of(CategoryType.INCOME, CategoryType.BOTH)))
                .thenReturn(List.of(incomeCategory, bothCategory));

        List<CategoryResponse> result = categoryService.findAll(CategoryType.INCOME);

        assertThat(result).hasSize(2)
                .as("Deve retornar apenas categorias INCOME e BOTH ao filtrar por INCOME");
        assertThat(result).extracting(CategoryResponse::type)
                .containsExactlyInAnyOrder(CategoryType.INCOME, CategoryType.BOTH);
        verify(categoryRepository, never()).findAllByInactiveFalseOrderByNameAsc();
    }

    // -------------------------------------------------------------------------
    // findById
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("findById returns DTO when category exists")
    void findById_whenCategoryExists_returnsCategoryResponse() {
        when(categoryRepository.findByIdAndInactiveFalse(expenseCategory.getId()))
                .thenReturn(Optional.of(expenseCategory));

        CategoryResponse result = categoryService.findById(expenseCategory.getId());

        assertThat(result).isNotNull()
                .as("Deve retornar DTO ao encontrar categoria ativa");
        assertThat(result.id()).isEqualTo(expenseCategory.getId());
        assertThat(result.name()).isEqualTo("Alimentação");
        assertThat(result.type()).isEqualTo(CategoryType.EXPENSE);
        assertThat(result.color()).isEqualTo("#FF5733");
        assertThat(result.version()).isEqualTo(0L);
    }

    @Test
    @DisplayName("findById throws EntityNotFoundException when category does not exist")
    void findById_whenCategoryDoesNotExist_throwsEntityNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        when(categoryRepository.findByIdAndInactiveFalse(unknownId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findById(unknownId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Categoria não encontrada")
                .as("Deve lançar EntityNotFoundException quando categoria não existe ou está inativa");
    }

    // -------------------------------------------------------------------------
    // create
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("create saves and returns DTO on success")
    void create_withValidRequest_savesAndReturnsCategoryResponse() {
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Transporte", CategoryType.EXPENSE, "#FF9900");

        Category saved = Category.builder()
                .id(UUID.randomUUID())
                .name("Transporte")
                .type(CategoryType.EXPENSE)
                .color("#FF9900")
                .inactive(false)
                .version(0L)
                .build();

        when(categoryRepository.existsByNameIgnoreCaseAndInactiveFalse("Transporte"))
                .thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        CategoryResponse result = categoryService.create(request);

        assertThat(result).isNotNull()
                .as("Deve retornar DTO após criar categoria com sucesso");
        assertThat(result.name()).isEqualTo("Transporte");
        assertThat(result.type()).isEqualTo(CategoryType.EXPENSE);
        assertThat(result.color()).isEqualTo("#FF9900");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("create throws IllegalArgumentException when name is duplicate")
    void create_withDuplicateName_throwsIllegalArgumentException() {
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Alimentação", CategoryType.EXPENSE, "#FF5733");

        when(categoryRepository.existsByNameIgnoreCaseAndInactiveFalse("Alimentação"))
                .thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Já existe uma categoria ativa com este nome")
                .as("Deve lançar IllegalArgumentException ao tentar criar categoria com nome duplicado");

        verify(categoryRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("update modifies name and color on success")
    void update_withValidRequest_updatesNameAndColor() {
        UUID id = expenseCategory.getId();
        UpdateCategoryRequest request = new UpdateCategoryRequest("Alimentação e Bebidas", "#AA1122", 0L);

        when(categoryRepository.findByIdAndInactiveFalse(id))
                .thenReturn(Optional.of(expenseCategory));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse result = categoryService.update(id, request);

        assertThat(result.name()).isEqualTo("Alimentação e Bebidas")
                .as("Deve atualizar o nome da categoria");
        assertThat(result.color()).isEqualTo("#AA1122")
                .as("Deve atualizar a cor da categoria");
        verify(categoryRepository).save(expenseCategory);
    }

    @Test
    @DisplayName("update throws ObjectOptimisticLockingFailureException when version mismatch")
    void update_withWrongVersion_throwsOptimisticLockingException() {
        UUID id = expenseCategory.getId();
        // Categoria está na versão 0, request envia versão 5 (desatualizada)
        UpdateCategoryRequest request = new UpdateCategoryRequest("Novo Nome", "#001122", 5L);

        when(categoryRepository.findByIdAndInactiveFalse(id))
                .thenReturn(Optional.of(expenseCategory));

        assertThatThrownBy(() -> categoryService.update(id, request))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class)
                .as("Deve lançar ObjectOptimisticLockingFailureException quando versão não bate (conflito de edição concorrente)");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("update throws EntityNotFoundException when category does not exist")
    void update_whenCategoryDoesNotExist_throwsEntityNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        UpdateCategoryRequest request = new UpdateCategoryRequest("Qualquer", "#AABBCC", 0L);

        when(categoryRepository.findByIdAndInactiveFalse(unknownId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.update(unknownId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Categoria não encontrada")
                .as("Deve lançar EntityNotFoundException ao tentar atualizar categoria inexistente");

        verify(categoryRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // delete (soft delete)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("delete sets inactive=true on success")
    void delete_whenCategoryExists_setsInactiveTrue() {
        UUID id = expenseCategory.getId();
        when(categoryRepository.findByIdAndInactiveFalse(id))
                .thenReturn(Optional.of(expenseCategory));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        categoryService.delete(id);

        assertThat(expenseCategory.isInactive()).isTrue()
                .as("Soft delete deve marcar inactive=true na entidade");
        verify(categoryRepository).save(expenseCategory);
    }

    @Test
    @DisplayName("delete throws EntityNotFoundException when category does not exist")
    void delete_whenCategoryDoesNotExist_throwsEntityNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        when(categoryRepository.findByIdAndInactiveFalse(unknownId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.delete(unknownId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Categoria não encontrada")
                .as("Deve lançar EntityNotFoundException ao tentar deletar categoria inexistente");

        verify(categoryRepository, never()).save(any());
    }
}
