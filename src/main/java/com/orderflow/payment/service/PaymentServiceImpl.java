package com.orderflow.payment.service;

import com.orderflow.common.exception.BadRequestException;
import com.orderflow.common.exception.InvalidStateTransitionException;
import com.orderflow.common.exception.ResourceNotFoundException;
import com.orderflow.inventory.service.InventoryService;
import com.orderflow.order.domain.Order;
import com.orderflow.order.domain.OrderItem;
import com.orderflow.order.domain.OrderStatus;
import com.orderflow.order.repository.OrderRepository;
import com.orderflow.payment.domain.Payment;
import com.orderflow.payment.domain.PaymentStatus;
import com.orderflow.payment.dto.PaymentResponse;
import com.orderflow.payment.dto.ProcessPaymentRequest;
import com.orderflow.payment.repository.PaymentRepository;
import com.orderflow.user.domain.Role;
import com.orderflow.user.domain.User;
import com.orderflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final com.orderflow.order.kafka.OrderEventProducer orderEventProducer;

    @Override
    @Transactional
    public PaymentResponse processPayment(String userEmail, ProcessPaymentRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", request.getOrderId()));

        if (!isAdmin(user) && !order.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not authorized to process payment for this order");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidStateTransitionException(
                    "Order " + order.getId() + " is in " + order.getStatus() + " status and cannot be paid for"
            );
        }

        if (request.getAmount().compareTo(order.getTotalAmount()) != 0) {
            throw new BadRequestException(
                    "Payment amount " + request.getAmount() + " does not match order total " + order.getTotalAmount()
            );
        }

        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseGet(() -> {
                    Payment p = new Payment();
                    p.setOrder(order);
                    p.setAmount(request.getAmount());
                    return p;
                });

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment for order {} has already succeeded. Returning existing record.", order.getId());
            return PaymentResponse.fromEntity(payment);
        }

        if (request.isSimulateFailure()) {
            log.warn("Simulating payment failure for order {}", order.getId());
            payment.markFailed();
            paymentRepository.save(payment);

            order.transitionTo(OrderStatus.CANCELLED);
            for (OrderItem item : order.getItems()) {
                inventoryService.releaseStock(item.getProduct().getId(), item.getQuantity());
            }
            orderRepository.save(order);

            orderEventProducer.sendOrderCancelled(com.orderflow.order.event.OrderCancelledEvent.builder()
                    .orderId(order.getId())
                    .userId(order.getUser().getId())
                    .userEmail(order.getUser().getEmail())
                    .reason("Payment processing failed")
                    .timestamp(java.time.Instant.now())
                    .build());

            return PaymentResponse.fromEntity(payment);
        }

        log.info("Processing successful payment simulation for order {}", order.getId());
        payment.markSuccess();
        Payment savedPayment = paymentRepository.save(payment);

        order.transitionTo(OrderStatus.CONFIRMED);
        for (OrderItem item : order.getItems()) {
            inventoryService.confirmStock(item.getProduct().getId(), item.getQuantity());
        }
        orderRepository.save(order);

        orderEventProducer.sendOrderConfirmed(com.orderflow.order.event.OrderConfirmedEvent.builder()
                .orderId(order.getId())
                .userId(order.getUser().getId())
                .userEmail(order.getUser().getEmail())
                .totalAmount(order.getTotalAmount())
                .paymentId(savedPayment.getId())
                .timestamp(java.time.Instant.now())
                .build());

        return PaymentResponse.fromEntity(savedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(String userEmail, UUID orderId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (!isAdmin(user) && !order.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not authorized to view payment for this order");
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", orderId));

        return PaymentResponse.fromEntity(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(String userEmail, UUID paymentId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        if (!isAdmin(user) && !payment.getOrder().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not authorized to view this payment");
        }

        return PaymentResponse.fromEntity(payment);
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream().anyMatch(r -> r.getName() == Role.RoleName.ADMIN);
    }
}
