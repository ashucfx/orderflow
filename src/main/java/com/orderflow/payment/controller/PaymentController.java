package com.orderflow.payment.controller;

import com.orderflow.auth.util.SecurityUtils;
import com.orderflow.common.api.ApiResponse;
import com.orderflow.payment.dto.PaymentResponse;
import com.orderflow.payment.dto.ProcessPaymentRequest;
import com.orderflow.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment simulation and settlement lifecycle")
@SecurityRequirement(name = "BearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/process")
    @Operation(summary = "Process payment for an order (simulates settlement, transitions order, confirms/releases inventory)")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @Valid @RequestBody ProcessPaymentRequest request) {
        String email = SecurityUtils.getCurrentUserEmail();
        PaymentResponse response = paymentService.processPayment(email, request);
        return ResponseEntity.ok(ApiResponse.ok("Payment processed", response));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payment details by order ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrderId(@PathVariable UUID orderId) {
        String email = SecurityUtils.getCurrentUserEmail();
        PaymentResponse response = paymentService.getPaymentByOrderId(email, orderId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment details by payment ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(@PathVariable UUID id) {
        String email = SecurityUtils.getCurrentUserEmail();
        PaymentResponse response = paymentService.getPaymentById(email, id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
