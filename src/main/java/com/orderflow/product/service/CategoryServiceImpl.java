package com.orderflow.product.service;

import com.orderflow.common.exception.ConflictException;
import com.orderflow.common.exception.ResourceNotFoundException;
import com.orderflow.product.domain.Category;
import com.orderflow.product.dto.CategoryResponse;
import com.orderflow.product.dto.CreateCategoryRequest;
import com.orderflow.product.dto.UpdateCategoryRequest;
import com.orderflow.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(CategoryResponse::fromEntity)
                .toList();
    }

    @Override
    public CategoryResponse getCategoryById(UUID id) {
        Category category = findCategoryById(id);
        return CategoryResponse.fromEntity(category);
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        String trimmedName = request.getName().trim();
        if (categoryRepository.existsByName(trimmedName)) {
            throw new ConflictException("Category already exists with name: " + trimmedName);
        }

        Category category = new Category();
        category.setName(trimmedName);
        category.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);

        Category saved = categoryRepository.save(category);
        return CategoryResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(UUID id, UpdateCategoryRequest request) {
        Category category = findCategoryById(id);
        String trimmedName = request.getName().trim();

        if (!category.getName().equalsIgnoreCase(trimmedName) && categoryRepository.existsByName(trimmedName)) {
            throw new ConflictException("Category already exists with name: " + trimmedName);
        }

        category.setName(trimmedName);
        category.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);

        Category saved = categoryRepository.save(category);
        return CategoryResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void deleteCategory(UUID id) {
        Category category = findCategoryById(id);
        categoryRepository.delete(category);
    }

    private Category findCategoryById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }
}
