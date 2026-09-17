package com.orderflow.inventory.service;

import com.orderflow.inventory.dto.InventoryResponse;
import com.orderflow.inventory.dto.StockAdjustmentRequest;

import java.util.UUID;

public interface InventoryService {

    InventoryResponse getInventoryByProductId(UUID productId);

    InventoryResponse adjustStock(UUID productId, StockAdjustmentRequest request);

    void reserveStock(UUID productId, int quantity);

    void releaseStock(UUID productId, int quantity);

    void confirmStock(UUID productId, int quantity);
}
