package com.orderflow.integration;

import com.orderflow.cart.domain.Cart;
import com.orderflow.inventory.domain.Inventory;
import com.orderflow.order.domain.Order;
import com.orderflow.order.domain.OrderItem;
import com.orderflow.order.domain.OrderStatus;
import com.orderflow.order.event.OrderCancelledEvent;
import com.orderflow.order.event.OrderConfirmedEvent;
import com.orderflow.order.event.OrderPlacedEvent;
import com.orderflow.payment.domain.Payment;
import com.orderflow.payment.domain.PaymentStatus;
import com.orderflow.product.domain.Category;
import com.orderflow.product.domain.Product;
import com.orderflow.user.domain.Role;
import com.orderflow.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderLifecycleScenarioTest {

    private User user;
    private Product product;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        Role customerRole = new Role();
        customerRole.setId((short) 1);
        customerRole.setName(Role.RoleName.CUSTOMER);

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("jane.shopper@example.com");
        user.setFirstName("Jane");
        user.setLastName("Shopper");
        user.addRole(customerRole);

        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("Electronics");

        product = new Product();
        product.setId(UUID.randomUUID());
        product.setCategory(category);
        product.setName("Mechanical Keyboard");
        product.setSku("KEY-MECH-001");
        product.setUnitPrice(new BigDecimal("99.99"));
        product.setActive(true);

        inventory = new Inventory();
        inventory.setId(UUID.randomUUID());
        inventory.setProduct(product);
        inventory.setAvailableQuantity(50);
        inventory.setReservedQuantity(0);
        inventory.setSoldQuantity(0);
    }

    @Test
    @DisplayName("Complete Order Lifecycle: Cart -> Checkout -> Payment Settlement -> Event Emission")
    void fullOrderLifecycle_happyPath() {
        // Step 1: Cart management
        Cart cart = new Cart();
        cart.setId(UUID.randomUUID());
        cart.setUser(user);

        cart.addItem(product, 2);
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.calculateSubtotal()).isEqualTo(new BigDecimal("199.98"));

        // Step 2: Atomic checkout simulation
        // Invariant: Reserve inventory stock before confirming checkout
        inventory.reserve(2);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(48);
        assertThat(inventory.getReservedQuantity()).isEqualTo(2);

        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setIdempotencyKey("KEY-LIFECYCLE-001");

        OrderItem orderItem = new OrderItem();
        orderItem.setId(UUID.randomUUID());
        orderItem.setProduct(product);
        orderItem.setQuantity(2);
        orderItem.setUnitPrice(product.getUnitPrice()); // price snapshot
        orderItem.setSubtotal(product.getUnitPrice().multiply(BigDecimal.valueOf(2)));
        order.addItem(orderItem);
        order.setTotalAmount(orderItem.getSubtotal());

        // Cart is cleared after checkout
        cart.clear();
        assertThat(cart.getItems()).isEmpty();
        assertThat(cart.calculateSubtotal()).isEqualTo(BigDecimal.ZERO);

        // Step 3: Event publication check
        OrderPlacedEvent placedEvent = OrderPlacedEvent.builder()
                .orderId(order.getId())
                .userId(user.getId())
                .userEmail(user.getEmail())
                .totalAmount(order.getTotalAmount())
                .itemCount(order.getItems().size())
                .timestamp(Instant.now())
                .build();
        assertThat(placedEvent.getOrderId()).isEqualTo(order.getId());
        assertThat(placedEvent.getTotalAmount()).isEqualTo(new BigDecimal("199.98"));

        // Step 4: Payment processing & order confirmation
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.markSuccess();

        order.transitionTo(OrderStatus.CONFIRMED);
        inventory.confirm(2);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(48);
        assertThat(inventory.getReservedQuantity()).isEqualTo(0);
        assertThat(inventory.getSoldQuantity()).isEqualTo(2);

        OrderConfirmedEvent confirmedEvent = OrderConfirmedEvent.builder()
                .orderId(order.getId())
                .userId(user.getId())
                .userEmail(user.getEmail())
                .totalAmount(order.getTotalAmount())
                .paymentId(payment.getId())
                .timestamp(Instant.now())
                .build();
        assertThat(confirmedEvent.getPaymentId()).isEqualTo(payment.getId());
    }

    @Test
    @DisplayName("Order Cancellation & Stock Release: Cart -> Checkout -> Cancellation -> Stock Restored")
    void fullOrderLifecycle_cancellationPath() {
        // Step 1: Reserve stock on checkout
        inventory.reserve(5);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(45);
        assertThat(inventory.getReservedQuantity()).isEqualTo(5);

        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);

        // Step 2: Cancel order
        order.transitionTo(OrderStatus.CANCELLED);
        inventory.release(5);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(50); // restored!
        assertThat(inventory.getReservedQuantity()).isEqualTo(0);
        assertThat(inventory.getSoldQuantity()).isEqualTo(0);

        OrderCancelledEvent cancelledEvent = OrderCancelledEvent.builder()
                .orderId(order.getId())
                .userId(user.getId())
                .userEmail(user.getEmail())
                .reason("User cancelled order")
                .timestamp(Instant.now())
                .build();
        assertThat(cancelledEvent.getOrderId()).isEqualTo(order.getId());
    }
}
