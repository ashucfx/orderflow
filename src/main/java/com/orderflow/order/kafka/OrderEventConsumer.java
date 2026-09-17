package com.orderflow.order.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.audit.service.AuditLogService;
import com.orderflow.common.kafka.KafkaTopics;
import com.orderflow.order.event.OrderCancelledEvent;
import com.orderflow.order.event.OrderConfirmedEvent;
import com.orderflow.order.event.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@KafkaListener(topics = KafkaTopics.ORDER_EVENTS, groupId = "orderflow-audit-group")
public class OrderEventConsumer {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @KafkaHandler
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("Received OrderPlacedEvent for order: {}", event.getOrderId());
        try {
            String metadata = objectMapper.writeValueAsString(event);
            auditLogService.logEvent(
                    event.getUserId(),
                    "ORDER_PLACED",
                    "ORDER",
                    event.getOrderId().toString(),
                    metadata
            );
        } catch (Exception e) {
            log.error("Failed to audit OrderPlacedEvent: {}", e.getMessage());
        }
    }

    @KafkaHandler
    public void handleOrderConfirmed(OrderConfirmedEvent event) {
        log.info("Received OrderConfirmedEvent for order: {}", event.getOrderId());
        try {
            String metadata = objectMapper.writeValueAsString(event);
            auditLogService.logEvent(
                    event.getUserId(),
                    "ORDER_CONFIRMED",
                    "ORDER",
                    event.getOrderId().toString(),
                    metadata
            );
        } catch (Exception e) {
            log.error("Failed to audit OrderConfirmedEvent: {}", e.getMessage());
        }
    }

    @KafkaHandler
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("Received OrderCancelledEvent for order: {}", event.getOrderId());
        try {
            String metadata = objectMapper.writeValueAsString(event);
            auditLogService.logEvent(
                    event.getUserId(),
                    "ORDER_CANCELLED",
                    "ORDER",
                    event.getOrderId().toString(),
                    metadata
            );
        } catch (Exception e) {
            log.error("Failed to audit OrderCancelledEvent: {}", e.getMessage());
        }
    }

    @KafkaHandler(isDefault = true)
    public void handleUnknown(Object event) {
        log.warn("Received unknown event payload: {}", event);
    }
}
