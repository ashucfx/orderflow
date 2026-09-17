package com.orderflow.inventory.dto;

import com.orderflow.inventory.domain.Inventory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {

    private UUID id;
    private UUID productId;
    private String productName;
    private String sku;
    private int availableQuantity;
    private int reservedQuantity;
    private int soldQuantity;
    private int totalQuantity;
    private Long version;
    private Instant updatedAt;

    public static InventoryResponse fromEntity(Inventory inventory) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .productId(inventory.getProduct().getId())
                .productName(inventory.getProduct().getName())
                .sku(inventory.getProduct().getSku())
                .availableQuantity(inventory.getAvailableQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .soldQuantity(inventory.getSoldQuantity())
                .totalQuantity(inventory.getAvailableQuantity() + inventory.getReservedQuantity() + inventory.getSoldQuantity())
                .version(inventory.getVersion())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }
}
