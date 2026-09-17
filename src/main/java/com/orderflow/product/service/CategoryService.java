package com.orderflow.product.service;

import com.orderflow.product.dto.CategoryResponse;
import com.orderflow.product.dto.CreateCategoryRequest;
import com.orderflow.product.dto.UpdateCategoryRequest;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    List<CategoryResponse> getAllCategories();

    CategoryResponse getCategoryById(UUID id);

    CategoryResponse createCategory(CreateCategoryRequest request);

    CategoryResponse updateCategory(UUID id, UpdateCategoryRequest request);

    void deleteCategory(UUID id);
}
