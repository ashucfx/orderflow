package com.orderflow.order.service;

import com.orderflow.cart.domain.Cart;
import com.orderflow.cart.domain.CartItem;
import com.orderflow.cart.repository.CartRepository;
import com.orderflow.common.api.PagedResponse;
import com.orderflow.common.exception.ConflictException;
import com.orderflow.common.exception.ResourceNotFoundException;
import com.orderflow.inventory.service.InventoryService;
import com.orderflow.order.domain.Order;
import com.orderflow.order.domain.OrderItem;
import com.orderflow.order.domain.OrderStatus;
import com.orderflow.order.dto.CheckoutRequest;
import com.orderflow.order.dto.OrderResponse;
import com.orderflow.order.dto.UpdateOrderStatusRequest;
import com.orderflow.order.repository.OrderRepository;
import com.orderflow.user.domain.Role;
import com.orderflow.user.domain.User;
import com.orderflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final com.orderflow.common.idempotency.service.IdempotencyService idempotencyService;

    @org.springframework.beans.factory.annotation.Value("${app.idempotency.ttl-seconds:86400}")
    private long idempotencyTtlSeconds = 86400;

    @Override
    @Transactional
    public OrderResponse checkout(String userEmail, CheckoutRequest request) {
        User user = findUserByEmail(userEmail);

        String idempotencyKey = (request != null && request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank())
                ? request.getIdempotencyKey().trim()
                : null;

        // Check for idempotency replay if key provided
        if (idempotencyKey != null) {
            Optional<Order> existing = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return OrderResponse.fromEntity(existing.get());
            }
        }

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ConflictException("No active shopping cart found"));

        if (cart.getItems().isEmpty()) {
            throw new ConflictException("Cannot checkout with an empty cart");
        }

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        if (idempotencyKey != null) {
            order.setIdempotencyKey(idempotencyKey);
        }

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            // Reserve inventory atomically
            inventoryService.reserveStock(cartItem.getProduct().getId(), cartItem.getQuantity());

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getProduct().getUnitPrice()); // Price snapshot

            BigDecimal lineSubtotal = cartItem.getProduct().getUnitPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            orderItem.setSubtotal(lineSubtotal);

            order.addItem(orderItem);
            totalAmount = totalAmount.add(lineSubtotal);
        }

        order.setTotalAmount(totalAmount);

        Order savedOrder;
        try {
            savedOrder = orderRepository.save(order);
            if (idempotencyKey != null) {
                try {
                    idempotencyService.saveRecord(idempotencyKey, 201, savedOrder.getId().toString(), idempotencyTtlSeconds);
                } catch (Exception ignored) {
                }
            }
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            if (idempotencyKey != null) {
                Optional<Order> existing = orderRepository.findByIdempotencyKey(idempotencyKey);
                if (existing.isPresent()) {
                    return OrderResponse.fromEntity(existing.get());
                }
            }
            throw ex;
        }

        // Clear cart items on successful order placement
        cart.clear();
        cartRepository.save(cart);

        return OrderResponse.fromEntity(savedOrder);
    }

    @Override
    public PagedResponse<OrderResponse> getMyOrders(String userEmail, Pageable pageable) {
        User user = findUserByEmail(userEmail);
        Page<OrderResponse> page = orderRepository.findByUserId(user.getId(), pageable)
                .map(OrderResponse::fromEntity);
        return PagedResponse.from(page);
    }

    @Override
    public OrderResponse getOrderById(String userEmail, UUID orderId) {
        User user = findUserByEmail(userEmail);
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName() == Role.RoleName.ADMIN);

        Order order = isAdmin
                ? orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId))
                : orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        return OrderResponse.fromEntity(order);
    }

    @Override
    public PagedResponse<OrderResponse> getAllOrders(Pageable pageable) {
        Page<OrderResponse> page = orderRepository.findAll(pageable)
                .map(OrderResponse::fromEntity);
        return PagedResponse.from(page);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        OrderStatus previousStatus = order.getStatus();
        OrderStatus nextStatus = request.getStatus();

        order.transitionTo(nextStatus);

        // State machine side effects on inventory
        if (nextStatus == OrderStatus.CANCELLED && previousStatus != OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                inventoryService.releaseStock(item.getProduct().getId(), item.getQuantity());
            }
        } else if (nextStatus == OrderStatus.CONFIRMED && previousStatus == OrderStatus.PENDING) {
            for (OrderItem item : order.getItems()) {
                inventoryService.confirmStock(item.getProduct().getId(), item.getQuantity());
            }
        }

        Order saved = orderRepository.save(order);
        return OrderResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(String userEmail, UUID orderId) {
        User user = findUserByEmail(userEmail);
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName() == Role.RoleName.ADMIN);

        Order order = isAdmin
                ? orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId))
                : orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        order.transitionTo(OrderStatus.CANCELLED);

        // Release reserved stock back to available inventory
        for (OrderItem item : order.getItems()) {
            inventoryService.releaseStock(item.getProduct().getId(), item.getQuantity());
        }

        Order saved = orderRepository.save(order);
        return OrderResponse.fromEntity(saved);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}
