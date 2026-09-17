package com.orderflow.cart.dto;

import com.orderflow.cart.domain.CartItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {

    private UUID id;
    private UUID productId;
    private String productName;
    private String sku;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal lineTotal;
    private Instant addedAt;

    public static CartItemResponse fromEntity(CartItem item) {
        BigDecimal unitPrice = item.getProduct().getUnitPrice();
        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

        return CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .sku(item.getProduct().getSku())
                .unitPrice(unitPrice)
                .quantity(item.getQuantity())
                .lineTotal(lineTotal)
                .addedAt(item.getAddedAt())
                .build();
    }
}
