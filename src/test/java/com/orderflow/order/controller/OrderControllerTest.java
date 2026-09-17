package com.orderflow.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.auth.config.SecurityConfig;
import com.orderflow.auth.filter.JwtAuthenticationFilter;
import com.orderflow.auth.service.JwtService;
import com.orderflow.common.api.PagedResponse;
import com.orderflow.order.domain.OrderStatus;
import com.orderflow.order.dto.CheckoutRequest;
import com.orderflow.order.dto.OrderItemResponse;
import com.orderflow.order.dto.OrderResponse;
import com.orderflow.order.dto.UpdateOrderStatusRequest;
import com.orderflow.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@ActiveProfiles("test")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    private OrderResponse sampleOrderResponse;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        OrderItemResponse item = OrderItemResponse.builder()
                .id(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .productName("Sample Product")
                .sku("SMPL-001")
                .quantity(2)
                .unitPrice(new BigDecimal("49.99"))
                .subtotal(new BigDecimal("99.98"))
                .build();

        sampleOrderResponse = OrderResponse.builder()
                .id(orderId)
                .userId(UUID.randomUUID())
                .userEmail("customer@example.com")
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("99.98"))
                .idempotencyKey("KEY-12345")
                .items(List.of(item))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void checkout_whenUnauthenticated_returns401Unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckoutRequest("KEY-12345"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void checkout_whenAuthenticatedCustomer_returns201Created() throws Exception {
        CheckoutRequest request = new CheckoutRequest("KEY-12345");
        when(orderService.checkout(eq("customer@example.com"), any(CheckoutRequest.class)))
                .thenReturn(sampleOrderResponse);

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.totalAmount").value(99.98))
                .andExpect(jsonPath("$.data.idempotencyKey").value("KEY-12345"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void getMyOrders_returns200WithPagedOrders() throws Exception {
        PagedResponse<OrderResponse> pagedResponse = new PagedResponse<>(
                List.of(sampleOrderResponse), 0, 10, 1, 1, true, true
        );

        when(orderService.getMyOrders(eq("customer@example.com"), any(Pageable.class)))
                .thenReturn(pagedResponse);

        mockMvc.perform(get("/api/v1/orders/my-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].userEmail").value("customer@example.com"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void getOrderById_returns200WithOrderDetails() throws Exception {
        when(orderService.getOrderById("customer@example.com", orderId))
                .thenReturn(sampleOrderResponse);

        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(orderId.toString()));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void getAllOrders_whenCustomer_returns403Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void getAllOrders_whenAdmin_returns200Ok() throws Exception {
        PagedResponse<OrderResponse> pagedResponse = new PagedResponse<>(
                List.of(sampleOrderResponse), 0, 20, 1, 1, true, true
        );

        when(orderService.getAllOrders(any(Pageable.class))).thenReturn(pagedResponse);

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void updateOrderStatus_whenCustomer_returns403Forbidden() throws Exception {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.CONFIRMED);

        mockMvc.perform(patch("/api/v1/orders/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = {"INVENTORY_MANAGER"})
    void updateOrderStatus_whenInventoryManager_returns200Ok() throws Exception {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.CONFIRMED);
        OrderResponse confirmedOrder = OrderResponse.builder()
                .id(orderId)
                .userEmail("customer@example.com")
                .status(OrderStatus.CONFIRMED)
                .totalAmount(new BigDecimal("99.98"))
                .build();

        when(orderService.updateOrderStatus(eq(orderId), any(UpdateOrderStatusRequest.class)))
                .thenReturn(confirmedOrder);

        mockMvc.perform(patch("/api/v1/orders/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void cancelOrder_whenCustomer_returns200Ok() throws Exception {
        OrderResponse cancelledOrder = OrderResponse.builder()
                .id(orderId)
                .userEmail("customer@example.com")
                .status(OrderStatus.CANCELLED)
                .totalAmount(new BigDecimal("99.98"))
                .build();

        when(orderService.cancelOrder("customer@example.com", orderId))
                .thenReturn(cancelledOrder);

        mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }
}
