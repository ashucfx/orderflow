package com.orderflow.cart.controller;

import com.orderflow.auth.config.SecurityConfig;
import com.orderflow.auth.filter.JwtAuthenticationFilter;
import com.orderflow.auth.service.JwtService;
import com.orderflow.cart.dto.AddToCartRequest;
import com.orderflow.cart.dto.CartItemResponse;
import com.orderflow.cart.dto.CartResponse;
import com.orderflow.cart.dto.UpdateCartItemRequest;
import com.orderflow.cart.service.CartService;
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

@WebMvcTest(CartController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@ActiveProfiles("test")
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void getCart_whenUnauthenticated_returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "shopper@example.com", roles = {"CUSTOMER"})
    void getCart_whenAuthenticated_returnsCartResponse() throws Exception {
        CartResponse response = CartResponse.builder()
                .id(UUID.randomUUID())
                .items(List.of())
                .totalItems(0)
                .subtotal(BigDecimal.ZERO)
                .updatedAt(Instant.now())
                .build();

        when(cartService.getCart("shopper@example.com")).thenReturn(response);

        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalItems").value(0));
    }

    @Test
    @WithMockUser(username = "shopper@example.com", roles = {"CUSTOMER"})
    void addItem_whenValidRequest_returnsCartResponse() throws Exception {
        UUID productId = UUID.randomUUID();
        AddToCartRequest request = new AddToCartRequest(productId, 2);

        CartItemResponse item = CartItemResponse.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .productName("Item")
                .sku("ITEM-01")
                .unitPrice(new BigDecimal("10.00"))
                .quantity(2)
                .lineTotal(new BigDecimal("20.00"))
                .addedAt(Instant.now())
                .build();

        CartResponse response = CartResponse.builder()
                .id(UUID.randomUUID())
                .items(List.of(item))
                .totalItems(2)
                .subtotal(new BigDecimal("20.00"))
                .updatedAt(Instant.now())
                .build();

        when(cartService.addItem(eq("shopper@example.com"), any(AddToCartRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalItems").value(2));
    }

    @Test
    @WithMockUser(username = "shopper@example.com", roles = {"CUSTOMER"})
    void updateItemQuantity_whenValidRequest_returnsUpdatedCart() throws Exception {
        UUID itemId = UUID.randomUUID();
        UpdateCartItemRequest request = new UpdateCartItemRequest(5);

        CartResponse response = CartResponse.builder()
                .id(UUID.randomUUID())
                .items(List.of())
                .totalItems(5)
                .subtotal(new BigDecimal("50.00"))
                .updatedAt(Instant.now())
                .build();

        when(cartService.updateItemQuantity(eq("shopper@example.com"), eq(itemId), any(UpdateCartItemRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/cart/items/{itemId}", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalItems").value(5));
    }

    @Test
    @WithMockUser(username = "shopper@example.com", roles = {"CUSTOMER"})
    void removeItem_whenCalled_returnsUpdatedCart() throws Exception {
        UUID itemId = UUID.randomUUID();

        CartResponse response = CartResponse.builder()
                .id(UUID.randomUUID())
                .items(List.of())
                .totalItems(0)
                .subtotal(BigDecimal.ZERO)
                .updatedAt(Instant.now())
                .build();

        when(cartService.removeItem("shopper@example.com", itemId)).thenReturn(response);

        mockMvc.perform(delete("/api/v1/cart/items/{itemId}", itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "shopper@example.com", roles = {"CUSTOMER"})
    void clearCart_whenCalled_returnsSuccess() throws Exception {
        mockMvc.perform(delete("/api/v1/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cart cleared"));
    }
}
