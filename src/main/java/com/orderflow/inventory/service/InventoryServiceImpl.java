package com.orderflow.inventory.service;

import com.orderflow.common.exception.ResourceNotFoundException;
import com.orderflow.inventory.domain.Inventory;
import com.orderflow.inventory.dto.InventoryResponse;
import com.orderflow.inventory.dto.StockAdjustmentRequest;
import com.orderflow.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    @Override
    public InventoryResponse getInventoryByProductId(UUID productId) {
        Inventory inventory = findByProductId(productId);
        return InventoryResponse.fromEntity(inventory);
    }

    @Override
    @Transactional
    public InventoryResponse adjustStock(UUID productId, StockAdjustmentRequest request) {
        Inventory inventory = findByProductId(productId);
        inventory.adjust(request.getQuantity());
        Inventory saved = inventoryRepository.save(inventory);
        return InventoryResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void reserveStock(UUID productId, int quantity) {
        Inventory inventory = findByProductId(productId);
        inventory.reserve(quantity);
        inventoryRepository.save(inventory);
    }

    @Override
    @Transactional
    public void releaseStock(UUID productId, int quantity) {
        Inventory inventory = findByProductId(productId);
        inventory.release(quantity);
        inventoryRepository.save(inventory);
    }

    @Override
    @Transactional
    public void confirmStock(UUID productId, int quantity) {
        Inventory inventory = findByProductId(productId);
        inventory.confirm(quantity);
        inventoryRepository.save(inventory);
    }

    private Inventory findByProductId(UUID productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory for product", productId));
    }
}
