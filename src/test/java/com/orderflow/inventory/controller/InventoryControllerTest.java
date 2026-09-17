package com.orderflow.inventory.controller;

import com.orderflow.auth.config.SecurityConfig;
import com.orderflow.auth.filter.JwtAuthenticationFilter;
import com.orderflow.auth.service.JwtService;
import com.orderflow.inventory.dto.InventoryResponse;
import com.orderflow.inventory.dto.StockAdjustmentRequest;
import com.orderflow.inventory.service.InventoryService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@ActiveProfiles("test")
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InventoryService inventoryService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void getInventory_whenUnauthenticated_returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/{productId}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"CUSTOMER"})
    void getInventory_whenCustomerRole_returns403Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/{productId}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"INVENTORY_MANAGER"})
    void getInventory_whenInventoryManagerRole_returns200Ok() throws Exception {
        UUID productId = UUID.randomUUID();
        InventoryResponse response = InventoryResponse.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .productName("Widget")
                .sku("WIDGET-01")
                .availableQuantity(50)
                .reservedQuantity(5)
                .soldQuantity(10)
                .totalQuantity(65)
                .version(1L)
                .updatedAt(Instant.now())
                .build();

        when(inventoryService.getInventoryByProductId(productId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/inventory/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.availableQuantity").value(50))
                .andExpect(jsonPath("$.data.productName").value("Widget"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getInventory_whenAdminRole_returns200Ok() throws Exception {
        UUID productId = UUID.randomUUID();
        InventoryResponse response = InventoryResponse.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .productName("Widget")
                .sku("WIDGET-01")
                .availableQuantity(100)
                .reservedQuantity(0)
                .soldQuantity(0)
                .totalQuantity(100)
                .version(1L)
                .updatedAt(Instant.now())
                .build();

        when(inventoryService.getInventoryByProductId(productId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/inventory/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.availableQuantity").value(100));
    }

    @Test
    @WithMockUser(roles = {"CUSTOMER"})
    void adjustStock_whenCustomerRole_returns403Forbidden() throws Exception {
        StockAdjustmentRequest request = new StockAdjustmentRequest(50, "Restock");

        mockMvc.perform(post("/api/v1/inventory/{productId}/adjust", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"INVENTORY_MANAGER"})
    void adjustStock_whenInventoryManagerRole_returns200Ok() throws Exception {
        UUID productId = UUID.randomUUID();
        StockAdjustmentRequest request = new StockAdjustmentRequest(25, "Restock shipment");

        InventoryResponse response = InventoryResponse.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .productName("Widget")
                .sku("WIDGET-01")
                .availableQuantity(75)
                .reservedQuantity(0)
                .soldQuantity(0)
                .totalQuantity(75)
                .version(2L)
                .updatedAt(Instant.now())
                .build();

        when(inventoryService.adjustStock(eq(productId), any(StockAdjustmentRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/inventory/{productId}/adjust", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.availableQuantity").value(75));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void adjustStock_whenAdminRole_returns200Ok() throws Exception {
        UUID productId = UUID.randomUUID();
        StockAdjustmentRequest request = new StockAdjustmentRequest(-10, "Damaged items");

        InventoryResponse response = InventoryResponse.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .productName("Widget")
                .sku("WIDGET-01")
                .availableQuantity(40)
                .reservedQuantity(0)
                .soldQuantity(0)
                .totalQuantity(40)
                .version(2L)
                .updatedAt(Instant.now())
                .build();

        when(inventoryService.adjustStock(eq(productId), any(StockAdjustmentRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/inventory/{productId}/adjust", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.availableQuantity").value(40));
    }
}
