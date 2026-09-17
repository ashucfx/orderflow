package com.orderflow.product.service;

import com.orderflow.common.api.PagedResponse;
import com.orderflow.common.exception.ConflictException;
import com.orderflow.common.exception.ResourceNotFoundException;
import com.orderflow.inventory.domain.Inventory;
import com.orderflow.inventory.repository.InventoryRepository;
import com.orderflow.product.domain.Category;
import com.orderflow.product.domain.Product;
import com.orderflow.product.dto.CreateProductRequest;
import com.orderflow.product.dto.ProductResponse;
import com.orderflow.product.dto.UpdateProductRequest;
import com.orderflow.product.repository.CategoryRepository;
import com.orderflow.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;

    @Override
    public PagedResponse<ProductResponse> getProducts(UUID categoryId, String search, Pageable pageable) {
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        Page<ProductResponse> page = productRepository.findActiveProducts(categoryId, cleanSearch, pageable)
                .map(ProductResponse::fromEntity);
        return PagedResponse.from(page);
    }

    @Override
    @Cacheable(value = "products", key = "#id")
    public ProductResponse getProductById(UUID id) {
        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        return ProductResponse.fromEntity(product);
    }

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));

        String trimmedSku = request.getSku().trim();
        if (productRepository.existsBySku(trimmedSku)) {
            throw new ConflictException("Product already exists with SKU: " + trimmedSku);
        }

        Product product = new Product();
        product.setCategory(category);
        product.setName(request.getName().trim());
        product.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        product.setSku(trimmedSku);
        product.setUnitPrice(request.getUnitPrice());
        product.setActive(true);

        Product savedProduct = productRepository.save(product);

        // Initialize inventory for the new product
        Inventory inventory = new Inventory();
        inventory.setProduct(savedProduct);
        inventory.setAvailableQuantity(0);
        inventory.setReservedQuantity(0);
        inventory.setSoldQuantity(0);
        inventoryRepository.save(inventory);

        return ProductResponse.fromEntity(savedProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public ProductResponse updateProduct(UUID id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));
            product.setCategory(category);
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            product.setName(request.getName().trim());
        }

        if (request.getDescription() != null) {
            product.setDescription(request.getDescription().trim());
        }

        if (request.getUnitPrice() != null) {
            product.setUnitPrice(request.getUnitPrice());
        }

        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }

        Product savedProduct = productRepository.save(product);
        return ProductResponse.fromEntity(savedProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        product.setActive(false);
        productRepository.save(product);
    }
}
