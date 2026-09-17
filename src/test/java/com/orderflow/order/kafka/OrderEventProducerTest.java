package com.orderflow.order.kafka;

import com.orderflow.common.kafka.KafkaTopics;
import com.orderflow.order.event.OrderCancelledEvent;
import com.orderflow.order.event.OrderConfirmedEvent;
import com.orderflow.order.event.OrderPlacedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OrderEventProducer orderEventProducer;

    private UUID orderId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void sendOrderPlaced_publishesToKafkaTopic() {
        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .orderId(orderId)
                .userId(userId)
                .userEmail("shopper@example.com")
                .totalAmount(new BigDecimal("99.99"))
                .itemCount(2)
                .timestamp(Instant.now())
                .build();

        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq(KafkaTopics.ORDER_EVENTS), eq(orderId.toString()), any(OrderPlacedEvent.class)))
                .thenReturn(future);

        orderEventProducer.sendOrderPlaced(event);

        verify(kafkaTemplate).send(eq(KafkaTopics.ORDER_EVENTS), eq(orderId.toString()), eq(event));
    }

    @Test
    void sendOrderConfirmed_publishesToKafkaTopic() {
        OrderConfirmedEvent event = OrderConfirmedEvent.builder()
                .orderId(orderId)
                .userId(userId)
                .userEmail("shopper@example.com")
                .totalAmount(new BigDecimal("99.99"))
                .paymentId(UUID.randomUUID())
                .timestamp(Instant.now())
                .build();

        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq(KafkaTopics.ORDER_EVENTS), eq(orderId.toString()), any(OrderConfirmedEvent.class)))
                .thenReturn(future);

        orderEventProducer.sendOrderConfirmed(event);

        verify(kafkaTemplate).send(eq(KafkaTopics.ORDER_EVENTS), eq(orderId.toString()), eq(event));
    }

    @Test
    void sendOrderCancelled_publishesToKafkaTopic() {
        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(orderId)
                .userId(userId)
                .userEmail("shopper@example.com")
                .reason("Customer request")
                .timestamp(Instant.now())
                .build();

        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq(KafkaTopics.ORDER_EVENTS), eq(orderId.toString()), any(OrderCancelledEvent.class)))
                .thenReturn(future);

        orderEventProducer.sendOrderCancelled(event);

        verify(kafkaTemplate).send(eq(KafkaTopics.ORDER_EVENTS), eq(orderId.toString()), eq(event));
    }
}
