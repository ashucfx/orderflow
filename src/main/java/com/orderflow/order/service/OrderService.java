package com.orderflow.order.service;

import com.orderflow.common.api.PagedResponse;
import com.orderflow.order.dto.CheckoutRequest;
import com.orderflow.order.dto.OrderResponse;
import com.orderflow.order.dto.UpdateOrderStatusRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {

    OrderResponse checkout(String userEmail, CheckoutRequest request);

    PagedResponse<OrderResponse> getMyOrders(String userEmail, Pageable pageable);

    OrderResponse getOrderById(String userEmail, UUID orderId);

    PagedResponse<OrderResponse> getAllOrders(Pageable pageable);

    OrderResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request);

    OrderResponse cancelOrder(String userEmail, UUID orderId);
}
