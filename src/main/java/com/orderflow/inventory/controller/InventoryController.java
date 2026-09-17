package com.orderflow.inventory.controller;

import com.orderflow.common.api.ApiResponse;
import com.orderflow.inventory.dto.InventoryResponse;
import com.orderflow.inventory.dto.StockAdjustmentRequest;
import com.orderflow.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Stock levels and inventory adjustments")
@SecurityRequirement(name = "BearerAuth")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER')")
    @Operation(summary = "Get inventory stock levels for a product (ADMIN or INVENTORY_MANAGER)")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventory(@PathVariable UUID productId) {
        InventoryResponse response = inventoryService.getInventoryByProductId(productId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{productId}/adjust")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER')")
    @Operation(summary = "Adjust inventory stock levels (ADMIN or INVENTORY_MANAGER)")
    public ResponseEntity<ApiResponse<InventoryResponse>> adjustStock(
            @PathVariable UUID productId,
            @Valid @RequestBody StockAdjustmentRequest request) {
        InventoryResponse response = inventoryService.adjustStock(productId, request);
        return ResponseEntity.ok(ApiResponse.ok("Stock adjusted successfully", response));
    }
}
