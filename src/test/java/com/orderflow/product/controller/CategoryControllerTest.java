package com.orderflow.product.controller;

import com.orderflow.auth.config.SecurityConfig;
import com.orderflow.auth.filter.JwtAuthenticationFilter;
import com.orderflow.auth.service.JwtService;
import com.orderflow.product.dto.CategoryResponse;
import com.orderflow.product.dto.CreateCategoryRequest;
import com.orderflow.product.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@ActiveProfiles("test")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void getAllCategories_whenUnauthenticated_returns200Ok() throws Exception {
        CategoryResponse category = CategoryResponse.builder()
                .id(UUID.randomUUID())
                .name("Electronics")
                .description("Electronic goods")
                .createdAt(Instant.now())
                .build();

        when(categoryService.getAllCategories()).thenReturn(List.of(category));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Electronics"));
    }

    @Test
    void createCategory_whenUnauthenticated_returns401Unauthorized() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest("Electronics", "Electronic goods");

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"CUSTOMER"})
    void createCategory_whenCustomerRole_returns403Forbidden() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest("Electronics", "Electronic goods");

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"INVENTORY_MANAGER"})
    void createCategory_whenInventoryManagerRole_returns201Created() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest("Electronics", "Electronic goods");
        CategoryResponse response = CategoryResponse.builder()
                .id(UUID.randomUUID())
                .name("Electronics")
                .description("Electronic goods")
                .createdAt(Instant.now())
                .build();

        when(categoryService.createCategory(any(CreateCategoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Electronics"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void deleteCategory_whenAdminRole_returns200Ok() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/{id}", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
