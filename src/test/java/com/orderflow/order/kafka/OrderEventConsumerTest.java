package com.orderflow.order.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.audit.service.AuditLogService;
import com.orderflow.order.event.OrderCancelledEvent;
import com.orderflow.order.event.OrderConfirmedEvent;
import com.orderflow.order.event.OrderPlacedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    @Mock
    private AuditLogService auditLogService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @InjectMocks
    private OrderEventConsumer orderEventConsumer;

    private UUID orderId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void handleOrderPlaced_logsAuditRecord() {
        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .orderId(orderId)
                .userId(userId)
                .userEmail("shopper@example.com")
                .totalAmount(new BigDecimal("99.99"))
                .itemCount(3)
                .timestamp(Instant.now())
                .build();

        orderEventConsumer.handleOrderPlaced(event);

        verify(auditLogService).logEvent(eq(userId), eq("ORDER_PLACED"), eq("ORDER"), eq(orderId.toString()), any());
    }

    @Test
    void handleOrderConfirmed_logsAuditRecord() {
        OrderConfirmedEvent event = OrderConfirmedEvent.builder()
                .orderId(orderId)
                .userId(userId)
                .userEmail("shopper@example.com")
                .totalAmount(new BigDecimal("99.99"))
                .paymentId(UUID.randomUUID())
                .timestamp(Instant.now())
                .build();

        orderEventConsumer.handleOrderConfirmed(event);

        verify(auditLogService).logEvent(eq(userId), eq("ORDER_CONFIRMED"), eq("ORDER"), eq(orderId.toString()), any());
    }

    @Test
    void handleOrderCancelled_logsAuditRecord() {
        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(orderId)
                .userId(userId)
                .userEmail("shopper@example.com")
                .reason("User cancellation")
                .timestamp(Instant.now())
                .build();

        orderEventConsumer.handleOrderCancelled(event);

        verify(auditLogService).logEvent(eq(userId), eq("ORDER_CANCELLED"), eq("ORDER"), eq(orderId.toString()), any());
    }
}
