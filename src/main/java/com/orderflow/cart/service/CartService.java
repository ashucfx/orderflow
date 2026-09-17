package com.orderflow.cart.service;

import com.orderflow.cart.dto.AddToCartRequest;
import com.orderflow.cart.dto.CartResponse;
import com.orderflow.cart.dto.UpdateCartItemRequest;

import java.util.UUID;

public interface CartService {

    CartResponse getCart(String userEmail);

    CartResponse addItem(String userEmail, AddToCartRequest request);

    CartResponse updateItemQuantity(String userEmail, UUID itemId, UpdateCartItemRequest request);

    CartResponse removeItem(String userEmail, UUID itemId);

    CartResponse clearCart(String userEmail);
}
