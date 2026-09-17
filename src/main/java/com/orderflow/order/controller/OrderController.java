package com.orderflow.order.controller;

import com.orderflow.auth.util.SecurityUtils;
import com.orderflow.common.api.ApiResponse;
import com.orderflow.common.api.PagedResponse;
import com.orderflow.order.dto.CheckoutRequest;
import com.orderflow.order.dto.OrderResponse;
import com.orderflow.order.dto.UpdateOrderStatusRequest;
import com.orderflow.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Transactional checkout and order lifecycle management")
@SecurityRequirement(name = "BearerAuth")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    @Operation(summary = "Checkout current shopping cart into a new order")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            @Valid @RequestBody(required = false) CheckoutRequest request) {
        String email = SecurityUtils.getCurrentUserEmail();
        OrderResponse order = orderService.checkout(email, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Order placed successfully", order));
    }

    @GetMapping("/my-orders")
    @Operation(summary = "Get current authenticated user's order history")
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> getMyOrders(
            @PageableDefault(size = 10) Pageable pageable) {
        String email = SecurityUtils.getCurrentUserEmail();
        PagedResponse<OrderResponse> orders = orderService.getMyOrders(email, pageable);
        return ResponseEntity.ok(ApiResponse.ok(orders));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order details by ID (owner or ADMIN)")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable UUID id) {
        String email = SecurityUtils.getCurrentUserEmail();
        OrderResponse order = orderService.getOrderById(email, id);
        return ResponseEntity.ok(ApiResponse.ok(order));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all orders with pagination (ADMIN only)")
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> getAllOrders(
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<OrderResponse> orders = orderService.getAllOrders(pageable);
        return ResponseEntity.ok(ApiResponse.ok(orders));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER')")
    @Operation(summary = "Update order status via state machine (ADMIN or INVENTORY_MANAGER)")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        OrderResponse order = orderService.updateOrderStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Order status updated", order));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order (owner or ADMIN)")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(@PathVariable UUID id) {
        String email = SecurityUtils.getCurrentUserEmail();
        OrderResponse order = orderService.cancelOrder(email, id);
        return ResponseEntity.ok(ApiResponse.ok("Order cancelled successfully", order));
    }
}
