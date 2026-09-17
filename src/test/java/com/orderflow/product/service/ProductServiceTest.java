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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    private ProductServiceImpl productService;

    private Category sampleCategory;
    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(productRepository, categoryRepository, inventoryRepository);

        sampleCategory = new Category();
        sampleCategory.setId(UUID.randomUUID());
        sampleCategory.setName("Electronics");

        sampleProduct = new Product();
        sampleProduct.setId(UUID.randomUUID());
        sampleProduct.setCategory(sampleCategory);
        sampleProduct.setName("Wireless Mouse");
        sampleProduct.setDescription("Ergonomic wireless mouse");
        sampleProduct.setSku("MOUSE-WL-01");
        sampleProduct.setUnitPrice(new BigDecimal("29.99"));
        sampleProduct.setActive(true);
    }

    @Test
    void getProducts_returnsPagedResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.findActiveProducts(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(sampleProduct), pageable, 1));

        PagedResponse<ProductResponse> result = productService.getProducts(null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Wireless Mouse");
        assertThat(result.getContent().get(0).getSku()).isEqualTo("MOUSE-WL-01");
    }

    @Test
    void getProductById_whenActive_returnsProduct() {
        UUID id = sampleProduct.getId();
        when(productRepository.findByIdAndActiveTrue(id)).thenReturn(Optional.of(sampleProduct));

        ProductResponse response = productService.getProductById(id);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getSku()).isEqualTo("MOUSE-WL-01");
        assertThat(response.getCategoryName()).isEqualTo("Electronics");
    }

    @Test
    void getProductById_whenNotFound_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(productRepository.findByIdAndActiveTrue(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createProduct_success_initializesInventory() {
        UUID categoryId = sampleCategory.getId();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(sampleCategory));
        when(productRepository.existsBySku("KB-RGB-01")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        CreateProductRequest request = new CreateProductRequest(
                categoryId, "Gaming Keyboard", "RGB Mechanical Keyboard", "KB-RGB-01", new BigDecimal("79.99")
        );

        ProductResponse response = productService.createProduct(request);

        assertThat(response.getName()).isEqualTo("Gaming Keyboard");
        assertThat(response.getSku()).isEqualTo("KB-RGB-01");

        // Verify product was saved
        verify(productRepository).save(any(Product.class));
        // Verify inventory record was initialized
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void createProduct_categoryNotFound_throwsResourceNotFoundException() {
        UUID categoryId = UUID.randomUUID();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        CreateProductRequest request = new CreateProductRequest(
                categoryId, "Keyboard", null, "KB-01", new BigDecimal("49.99")
        );

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createProduct_duplicateSku_throwsConflictException() {
        UUID categoryId = sampleCategory.getId();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(sampleCategory));
        when(productRepository.existsBySku("MOUSE-WL-01")).thenReturn(true);

        CreateProductRequest request = new CreateProductRequest(
                categoryId, "Duplicate Mouse", null, "MOUSE-WL-01", new BigDecimal("19.99")
        );

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateProduct_success() {
        UUID id = sampleProduct.getId();
        when(productRepository.findById(id)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProductRequest request = new UpdateProductRequest(
                null, "Updated Wireless Mouse", "New desc", new BigDecimal("34.99"), null
        );

        ProductResponse response = productService.updateProduct(id, request);

        assertThat(response.getName()).isEqualTo("Updated Wireless Mouse");
        assertThat(response.getUnitPrice()).isEqualTo(new BigDecimal("34.99"));
        verify(productRepository).save(sampleProduct);
    }

    @Test
    void deleteProduct_setsActiveFalse() {
        UUID id = sampleProduct.getId();
        when(productRepository.findById(id)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        productService.deleteProduct(id);

        assertThat(sampleProduct.isActive()).isFalse();
        verify(productRepository).save(sampleProduct);
    }
}
