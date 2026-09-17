package com.orderflow.order.kafka;

import com.orderflow.common.kafka.KafkaTopics;
import com.orderflow.order.event.OrderCancelledEvent;
import com.orderflow.order.event.OrderConfirmedEvent;
import com.orderflow.order.event.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendOrderPlaced(OrderPlacedEvent event) {
        String key = event.getOrderId().toString();
        log.info("Publishing OrderPlacedEvent for order {}", key);
        kafkaTemplate.send(KafkaTopics.ORDER_EVENTS, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send OrderPlacedEvent for order {}: {}", key, ex.getMessage());
                    } else {
                        log.debug("Successfully published OrderPlacedEvent for order {}", key);
                    }
                });
    }

    public void sendOrderConfirmed(OrderConfirmedEvent event) {
        String key = event.getOrderId().toString();
        log.info("Publishing OrderConfirmedEvent for order {}", key);
        kafkaTemplate.send(KafkaTopics.ORDER_EVENTS, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send OrderConfirmedEvent for order {}: {}", key, ex.getMessage());
                    } else {
                        log.debug("Successfully published OrderConfirmedEvent for order {}", key);
                    }
                });
    }

    public void sendOrderCancelled(OrderCancelledEvent event) {
        String key = event.getOrderId().toString();
        log.info("Publishing OrderCancelledEvent for order {}", key);
        kafkaTemplate.send(KafkaTopics.ORDER_EVENTS, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send OrderCancelledEvent for order {}: {}", key, ex.getMessage());
                    } else {
                        log.debug("Successfully published OrderCancelledEvent for order {}", key);
                    }
                });
    }
}
