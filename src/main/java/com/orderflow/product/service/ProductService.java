package com.orderflow.product.service;

import com.orderflow.common.api.PagedResponse;
import com.orderflow.product.dto.CreateProductRequest;
import com.orderflow.product.dto.ProductResponse;
import com.orderflow.product.dto.UpdateProductRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProductService {

    PagedResponse<ProductResponse> getProducts(UUID categoryId, String search, Pageable pageable);

    ProductResponse getProductById(UUID id);

    ProductResponse createProduct(CreateProductRequest request);

    ProductResponse updateProduct(UUID id, UpdateProductRequest request);

    void deleteProduct(UUID id);
}
