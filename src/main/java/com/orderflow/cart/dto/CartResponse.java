package com.orderflow.cart.dto;

import com.orderflow.cart.domain.Cart;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    private UUID id;
    private List<CartItemResponse> items;
    private int totalItems;
    private BigDecimal subtotal;
    private Instant updatedAt;

    public static CartResponse fromEntity(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(CartItemResponse::fromEntity)
                .toList();

        return CartResponse.builder()
                .id(cart.getId())
                .items(itemResponses)
                .totalItems(cart.calculateTotalItems())
                .subtotal(cart.calculateSubtotal())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }
}
