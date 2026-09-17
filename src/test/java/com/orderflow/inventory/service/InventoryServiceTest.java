package com.orderflow.inventory.service;

import com.orderflow.common.exception.InsufficientInventoryException;
import com.orderflow.common.exception.ResourceNotFoundException;
import com.orderflow.inventory.domain.Inventory;
import com.orderflow.inventory.dto.InventoryResponse;
import com.orderflow.inventory.dto.StockAdjustmentRequest;
import com.orderflow.inventory.repository.InventoryRepository;
import com.orderflow.product.domain.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    private InventoryServiceImpl inventoryService;

    private Product sampleProduct;
    private Inventory sampleInventory;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryServiceImpl(inventoryRepository);

        sampleProduct = new Product();
        sampleProduct.setId(UUID.randomUUID());
        sampleProduct.setName("Laptop");
        sampleProduct.setSku("LAPTOP-01");

        sampleInventory = new Inventory();
        sampleInventory.setId(UUID.randomUUID());
        sampleInventory.setProduct(sampleProduct);
        sampleInventory.setAvailableQuantity(50);
        sampleInventory.setReservedQuantity(10);
        sampleInventory.setSoldQuantity(5);
        sampleInventory.setVersion(1L);
    }

    @Test
    void getInventoryByProductId_whenFound_returnsResponse() {
        UUID productId = sampleProduct.getId();
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(sampleInventory));

        InventoryResponse response = inventoryService.getInventoryByProductId(productId);

        assertThat(response.getProductId()).isEqualTo(productId);
        assertThat(response.getAvailableQuantity()).isEqualTo(50);
        assertThat(response.getReservedQuantity()).isEqualTo(10);
        assertThat(response.getSoldQuantity()).isEqualTo(5);
        assertThat(response.getTotalQuantity()).isEqualTo(65);
    }

    @Test
    void getInventoryByProductId_whenNotFound_throwsResourceNotFoundException() {
        UUID productId = UUID.randomUUID();
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getInventoryByProductId(productId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void adjustStock_positive_success() {
        UUID productId = sampleProduct.getId();
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(sampleInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockAdjustmentRequest request = new StockAdjustmentRequest(25, "Restock shipment received");
        InventoryResponse response = inventoryService.adjustStock(productId, request);

        assertThat(response.getAvailableQuantity()).isEqualTo(75);
        verify(inventoryRepository).save(sampleInventory);
    }

    @Test
    void adjustStock_negative_success() {
        UUID productId = sampleProduct.getId();
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(sampleInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockAdjustmentRequest request = new StockAdjustmentRequest(-20, "Damaged stock removed");
        InventoryResponse response = inventoryService.adjustStock(productId, request);

        assertThat(response.getAvailableQuantity()).isEqualTo(30);
        verify(inventoryRepository).save(sampleInventory);
    }

    @Test
    void adjustStock_excessiveDeduction_throwsInsufficientInventoryException() {
        UUID productId = sampleProduct.getId();
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(sampleInventory));

        StockAdjustmentRequest request = new StockAdjustmentRequest(-100, "Invalid deduction");

        assertThatThrownBy(() -> inventoryService.adjustStock(productId, request))
                .isInstanceOf(InsufficientInventoryException.class);
    }

    @Test
    void reserveStock_success() {
        UUID productId = sampleProduct.getId();
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(sampleInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        inventoryService.reserveStock(productId, 10);

        assertThat(sampleInventory.getAvailableQuantity()).isEqualTo(40);
        assertThat(sampleInventory.getReservedQuantity()).isEqualTo(20);
        verify(inventoryRepository).save(sampleInventory);
    }

    @Test
    void releaseStock_success() {
        UUID productId = sampleProduct.getId();
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(sampleInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        inventoryService.releaseStock(productId, 5);

        assertThat(sampleInventory.getReservedQuantity()).isEqualTo(5);
        assertThat(sampleInventory.getAvailableQuantity()).isEqualTo(55);
        verify(inventoryRepository).save(sampleInventory);
    }

    @Test
    void confirmStock_success() {
        UUID productId = sampleProduct.getId();
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(sampleInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        inventoryService.confirmStock(productId, 5);

        assertThat(sampleInventory.getReservedQuantity()).isEqualTo(5);
        assertThat(sampleInventory.getSoldQuantity()).isEqualTo(10);
        verify(inventoryRepository).save(sampleInventory);
    }
}
