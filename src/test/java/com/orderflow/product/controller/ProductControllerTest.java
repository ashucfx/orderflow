package com.orderflow.product.controller;

import com.orderflow.auth.config.SecurityConfig;
import com.orderflow.auth.filter.JwtAuthenticationFilter;
import com.orderflow.auth.service.JwtService;
import com.orderflow.common.api.PagedResponse;
import com.orderflow.product.dto.CreateProductRequest;
import com.orderflow.product.dto.ProductResponse;
import com.orderflow.product.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@ActiveProfiles("test")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void getProducts_whenUnauthenticated_returns200Ok() throws Exception {
        ProductResponse product = ProductResponse.builder()
                .id(UUID.randomUUID())
                .categoryId(UUID.randomUUID())
                .categoryName("Electronics")
                .name("Keyboard")
                .sku("KB-01")
                .unitPrice(new BigDecimal("49.99"))
                .active(true)
                .createdAt(Instant.now())
                .build();

        PagedResponse<ProductResponse> paged = PagedResponse.from(
                new PageImpl<>(List.of(product), PageRequest.of(0, 20), 1)
        );

        when(productService.getProducts(any(), any(), any())).thenReturn(paged);

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("Keyboard"));
    }

    @Test
    void getProductById_whenUnauthenticated_returns200Ok() throws Exception {
        UUID id = UUID.randomUUID();
        ProductResponse product = ProductResponse.builder()
                .id(id)
                .categoryId(UUID.randomUUID())
                .categoryName("Electronics")
                .name("Monitor")
                .sku("MON-01")
                .unitPrice(new BigDecimal("199.99"))
                .active(true)
                .createdAt(Instant.now())
                .build();

        when(productService.getProductById(id)).thenReturn(product);

        mockMvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sku").value("MON-01"));
    }

    @Test
    void createProduct_whenUnauthenticated_returns401Unauthorized() throws Exception {
        CreateProductRequest request = new CreateProductRequest(
                UUID.randomUUID(), "Monitor", "4K Monitor", "MON-02", new BigDecimal("299.99")
        );

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"CUSTOMER"})
    void createProduct_whenCustomerRole_returns403Forbidden() throws Exception {
        CreateProductRequest request = new CreateProductRequest(
                UUID.randomUUID(), "Monitor", "4K Monitor", "MON-02", new BigDecimal("299.99")
        );

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"INVENTORY_MANAGER"})
    void createProduct_whenInventoryManagerRole_returns201Created() throws Exception {
        UUID catId = UUID.randomUUID();
        CreateProductRequest request = new CreateProductRequest(
                catId, "Monitor", "4K Monitor", "MON-02", new BigDecimal("299.99")
        );

        ProductResponse response = ProductResponse.builder()
                .id(UUID.randomUUID())
                .categoryId(catId)
                .categoryName("Electronics")
                .name("Monitor")
                .sku("MON-02")
                .unitPrice(new BigDecimal("299.99"))
                .active(true)
                .createdAt(Instant.now())
                .build();

        when(productService.createProduct(any(CreateProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sku").value("MON-02"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void createProduct_whenAdminRole_returns201Created() throws Exception {
        UUID catId = UUID.randomUUID();
        CreateProductRequest request = new CreateProductRequest(
                catId, "Monitor", "4K Monitor", "MON-03", new BigDecimal("299.99")
        );

        ProductResponse response = ProductResponse.builder()
                .id(UUID.randomUUID())
                .categoryId(catId)
                .categoryName("Electronics")
                .name("Monitor")
                .sku("MON-03")
                .unitPrice(new BigDecimal("299.99"))
                .active(true)
                .createdAt(Instant.now())
                .build();

        when(productService.createProduct(any(CreateProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = {"INVENTORY_MANAGER"})
    void deleteProduct_whenInventoryManagerRole_returns403Forbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/products/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void deleteProduct_whenAdminRole_returns200Ok() throws Exception {
        mockMvc.perform(delete("/api/v1/products/{id}", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
