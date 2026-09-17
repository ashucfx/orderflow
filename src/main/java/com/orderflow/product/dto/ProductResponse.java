package com.orderflow.product.dto;

import com.orderflow.product.domain.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse implements Serializable {

    private UUID id;
    private UUID categoryId;
    private String categoryName;
    private String name;
    private String description;
    private String sku;
    private BigDecimal unitPrice;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public static ProductResponse fromEntity(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .name(product.getName())
                .description(product.getDescription())
                .sku(product.getSku())
                .unitPrice(product.getUnitPrice())
                .active(product.isActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
