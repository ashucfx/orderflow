package com.orderflow.order.domain;

import com.orderflow.common.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderDomainTest {

    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setStatus(OrderStatus.PENDING);
    }

    @Test
    void transitionTo_validLifecycle_succeeds() {
        order.transitionTo(OrderStatus.CONFIRMED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        order.transitionTo(OrderStatus.PROCESSING);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING);

        order.transitionTo(OrderStatus.SHIPPED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);

        order.transitionTo(OrderStatus.DELIVERED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void transitionTo_cancellationFromPending_succeeds() {
        order.transitionTo(OrderStatus.CANCELLED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void transitionTo_cancellationFromConfirmed_succeeds() {
        order.transitionTo(OrderStatus.CONFIRMED);
        order.transitionTo(OrderStatus.CANCELLED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void transitionTo_invalidSkipState_throwsException() {
        assertThatThrownBy(() -> order.transitionTo(OrderStatus.DELIVERED))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Invalid order state transition");
    }

    @Test
    void transitionTo_fromTerminalDelivered_throwsException() {
        order.transitionTo(OrderStatus.CONFIRMED);
        order.transitionTo(OrderStatus.PROCESSING);
        order.transitionTo(OrderStatus.SHIPPED);
        order.transitionTo(OrderStatus.DELIVERED);

        assertThatThrownBy(() -> order.transitionTo(OrderStatus.CANCELLED))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void transitionTo_fromTerminalCancelled_throwsException() {
        order.transitionTo(OrderStatus.CANCELLED);

        assertThatThrownBy(() -> order.transitionTo(OrderStatus.CONFIRMED))
                .isInstanceOf(InvalidStateTransitionException.class);
    }
}
