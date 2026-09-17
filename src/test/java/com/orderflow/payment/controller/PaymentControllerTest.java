package com.orderflow.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.auth.config.SecurityConfig;
import com.orderflow.auth.filter.JwtAuthenticationFilter;
import com.orderflow.auth.service.JwtService;
import com.orderflow.payment.domain.PaymentStatus;
import com.orderflow.payment.dto.PaymentResponse;
import com.orderflow.payment.dto.ProcessPaymentRequest;
import com.orderflow.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@ActiveProfiles("test")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    private PaymentResponse samplePaymentResponse;
    private UUID orderId;
    private UUID paymentId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        paymentId = UUID.randomUUID();

        samplePaymentResponse = PaymentResponse.builder()
                .id(paymentId)
                .orderId(orderId)
                .status(PaymentStatus.SUCCESS)
                .amount(new BigDecimal("99.99"))
                .processedAt(Instant.now())
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void processPayment_whenUnauthenticated_returns401Unauthorized() throws Exception {
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .orderId(orderId)
                .amount(new BigDecimal("99.99"))
                .build();

        mockMvc.perform(post("/api/v1/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void processPayment_whenAuthenticatedCustomer_returns200Ok() throws Exception {
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .orderId(orderId)
                .amount(new BigDecimal("99.99"))
                .paymentMethod("CREDIT_CARD")
                .build();

        when(paymentService.processPayment(eq("customer@example.com"), any(ProcessPaymentRequest.class)))
                .thenReturn(samplePaymentResponse);

        mockMvc.perform(post("/api/v1/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(paymentId.toString()))
                .andExpect(jsonPath("$.data.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.amount").value(99.99));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void processPayment_whenInvalidRequest_returns400BadRequest() throws Exception {
        ProcessPaymentRequest invalidRequest = ProcessPaymentRequest.builder()
                .orderId(null) // invalid null orderId
                .amount(new BigDecimal("-10.00")) // invalid negative amount
                .build();

        mockMvc.perform(post("/api/v1/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void getPaymentByOrderId_returns200Ok() throws Exception {
        when(paymentService.getPaymentByOrderId("customer@example.com", orderId))
                .thenReturn(samplePaymentResponse);

        mockMvc.perform(get("/api/v1/payments/order/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void getPaymentById_returns200Ok() throws Exception {
        when(paymentService.getPaymentById("customer@example.com", paymentId))
                .thenReturn(samplePaymentResponse);

        mockMvc.perform(get("/api/v1/payments/{id}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(paymentId.toString()))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }
}
