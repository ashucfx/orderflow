package com.orderflow.payment.service;

import com.orderflow.payment.dto.PaymentResponse;
import com.orderflow.payment.dto.ProcessPaymentRequest;

import java.util.UUID;

public interface PaymentService {

    PaymentResponse processPayment(String userEmail, ProcessPaymentRequest request);

    PaymentResponse getPaymentByOrderId(String userEmail, UUID orderId);

    PaymentResponse getPaymentById(String userEmail, UUID paymentId);
}
