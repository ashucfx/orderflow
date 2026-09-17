package com.orderflow.order.service;

import com.orderflow.cart.domain.Cart;
import com.orderflow.cart.repository.CartRepository;
import com.orderflow.common.api.PagedResponse;
import com.orderflow.common.exception.ConflictException;
import com.orderflow.common.exception.InvalidStateTransitionException;
import com.orderflow.inventory.service.InventoryService;
import com.orderflow.order.domain.Order;
import com.orderflow.order.domain.OrderStatus;
import com.orderflow.order.dto.CheckoutRequest;
import com.orderflow.order.dto.OrderResponse;
import com.orderflow.order.dto.UpdateOrderStatusRequest;
import com.orderflow.order.repository.OrderRepository;
import com.orderflow.product.domain.Product;
import com.orderflow.user.domain.Role;
import com.orderflow.user.domain.User;
import com.orderflow.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InventoryService inventoryService;

    private OrderServiceImpl orderService;

    private User sampleUser;
    private Product sampleProduct;
    private Cart sampleCart;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderRepository, cartRepository, userRepository, inventoryService);

        Role customerRole = new Role();
        customerRole.setId((short) 1);
        customerRole.setName(Role.RoleName.CUSTOMER);

        sampleUser = new User();
        sampleUser.setId(UUID.randomUUID());
        sampleUser.setEmail("shopper@example.com");
        sampleUser.addRole(customerRole);

        sampleProduct = new Product();
        sampleProduct.setId(UUID.randomUUID());
        sampleProduct.setName("Mechanical Keyboard");
        sampleProduct.setSku("KB-MECH-01");
        sampleProduct.setUnitPrice(new BigDecimal("100.00"));
        sampleProduct.setActive(true);

        sampleCart = new Cart();
        sampleCart.setId(UUID.randomUUID());
        sampleCart.setUser(sampleUser);
    }

    @Test
    void checkout_success_reservesInventoryAndSnapshotsPrice() {
        sampleCart.addItem(sampleProduct, 2);

        when(userRepository.findByEmail("shopper@example.com")).thenReturn(Optional.of(sampleUser));
        when(cartRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(sampleCart));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });

        CheckoutRequest request = new CheckoutRequest("idemp-key-123");
        OrderResponse response = orderService.checkout("shopper@example.com", request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getUnitPrice()).isEqualByComparingTo(new BigDecimal("100.00"));

        // Verify inventory was reserved
        verify(inventoryService).reserveStock(sampleProduct.getId(), 2);
        // Verify cart was cleared
        assertThat(sampleCart.getItems()).isEmpty();
        verify(cartRepository).save(sampleCart);
    }

    @Test
    void checkout_emptyCart_throwsConflictException() {
        when(userRepository.findByEmail("shopper@example.com")).thenReturn(Optional.of(sampleUser));
        when(cartRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(sampleCart));

        CheckoutRequest request = new CheckoutRequest(null);

        assertThatThrownBy(() -> orderService.checkout("shopper@example.com", request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot checkout with an empty cart");
    }

    @Test
    void checkout_idempotencyReplay_returnsExistingOrder() {
        Order existingOrder = new Order();
        existingOrder.setId(UUID.randomUUID());
        existingOrder.setUser(sampleUser);
        existingOrder.setStatus(OrderStatus.PENDING);
        existingOrder.setTotalAmount(new BigDecimal("50.00"));
        existingOrder.setIdempotencyKey("replay-key");

        when(userRepository.findByEmail("shopper@example.com")).thenReturn(Optional.of(sampleUser));
        when(orderRepository.findByIdempotencyKey("replay-key")).thenReturn(Optional.of(existingOrder));

        CheckoutRequest request = new CheckoutRequest("replay-key");
        OrderResponse response = orderService.checkout("shopper@example.com", request);

        assertThat(response.getId()).isEqualTo(existingOrder.getId());
        verify(inventoryService, never()).reserveStock(any(), anyInt());
    }

    @Test
    void getMyOrders_returnsPagedOrders() {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setUser(sampleUser);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setTotalAmount(new BigDecimal("100.00"));

        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findByEmail("shopper@example.com")).thenReturn(Optional.of(sampleUser));
        when(orderRepository.findByUserId(sampleUser.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(order), pageable, 1));

        PagedResponse<OrderResponse> result = orderService.getMyOrders("shopper@example.com", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void updateOrderStatus_toConfirmed_confirmsInventory() {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setUser(sampleUser);
        order.setStatus(OrderStatus.PENDING);

        com.orderflow.order.domain.OrderItem item = new com.orderflow.order.domain.OrderItem();
        item.setProduct(sampleProduct);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("100.00"));
        item.setSubtotal(new BigDecimal("200.00"));
        order.addItem(item);

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.CONFIRMED);
        OrderResponse response = orderService.updateOrderStatus(order.getId(), request);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(inventoryService).confirmStock(sampleProduct.getId(), 2);
    }

    @Test
    void updateOrderStatus_toCancelled_releasesInventory() {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setUser(sampleUser);
        order.setStatus(OrderStatus.PENDING);

        com.orderflow.order.domain.OrderItem item = new com.orderflow.order.domain.OrderItem();
        item.setProduct(sampleProduct);
        item.setQuantity(3);
        item.setUnitPrice(new BigDecimal("100.00"));
        item.setSubtotal(new BigDecimal("300.00"));
        order.addItem(item);

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.CANCELLED);
        OrderResponse response = orderService.updateOrderStatus(order.getId(), request);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(inventoryService).releaseStock(sampleProduct.getId(), 3);
    }

    @Test
    void updateOrderStatus_invalidTransition_throwsException() {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setUser(sampleUser);
        order.setStatus(OrderStatus.DELIVERED);

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.CANCELLED);

        assertThatThrownBy(() -> orderService.updateOrderStatus(order.getId(), request))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void cancelOrder_success() {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setUser(sampleUser);
        order.setStatus(OrderStatus.PENDING);

        com.orderflow.order.domain.OrderItem item = new com.orderflow.order.domain.OrderItem();
        item.setProduct(sampleProduct);
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("100.00"));
        item.setSubtotal(new BigDecimal("100.00"));
        order.addItem(item);

        when(userRepository.findByEmail("shopper@example.com")).thenReturn(Optional.of(sampleUser));
        when(orderRepository.findByIdAndUserId(order.getId(), sampleUser.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.cancelOrder("shopper@example.com", order.getId());

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(inventoryService).releaseStock(sampleProduct.getId(), 1);
    }
}
