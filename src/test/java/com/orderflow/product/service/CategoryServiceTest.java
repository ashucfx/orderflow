package com.orderflow.product.service;

import com.orderflow.common.exception.ConflictException;
import com.orderflow.common.exception.ResourceNotFoundException;
import com.orderflow.product.domain.Category;
import com.orderflow.product.dto.CategoryResponse;
import com.orderflow.product.dto.CreateCategoryRequest;
import com.orderflow.product.dto.UpdateCategoryRequest;
import com.orderflow.product.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryServiceImpl categoryService;

    private Category sampleCategory;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryServiceImpl(categoryRepository);

        sampleCategory = new Category();
        sampleCategory.setId(UUID.randomUUID());
        sampleCategory.setName("Electronics");
        sampleCategory.setDescription("Electronic devices and accessories");
    }

    @Test
    void getAllCategories_returnsList() {
        when(categoryRepository.findAll()).thenReturn(List.of(sampleCategory));

        List<CategoryResponse> result = categoryService.getAllCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Electronics");
    }

    @Test
    void getCategoryById_whenFound_returnsCategory() {
        UUID id = sampleCategory.getId();
        when(categoryRepository.findById(id)).thenReturn(Optional.of(sampleCategory));

        CategoryResponse result = categoryService.getCategoryById(id);

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getName()).isEqualTo("Electronics");
    }

    @Test
    void getCategoryById_whenNotFound_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createCategory_success() {
        when(categoryRepository.existsByName("Books")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        CreateCategoryRequest request = new CreateCategoryRequest("Books", "Printed and digital books");
        CategoryResponse response = categoryService.createCategory(request);

        assertThat(response.getName()).isEqualTo("Books");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createCategory_duplicateName_throwsConflictException() {
        when(categoryRepository.existsByName("Electronics")).thenReturn(true);

        CreateCategoryRequest request = new CreateCategoryRequest("Electronics", "Duplicate category");

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateCategory_success() {
        UUID id = sampleCategory.getId();
        when(categoryRepository.findById(id)).thenReturn(Optional.of(sampleCategory));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateCategoryRequest request = new UpdateCategoryRequest("Consumer Electronics", "Updated description");
        CategoryResponse response = categoryService.updateCategory(id, request);

        assertThat(response.getName()).isEqualTo("Consumer Electronics");
    }

    @Test
    void deleteCategory_success() {
        UUID id = sampleCategory.getId();
        when(categoryRepository.findById(id)).thenReturn(Optional.of(sampleCategory));

        categoryService.deleteCategory(id);

        verify(categoryRepository).delete(sampleCategory);
    }
}
