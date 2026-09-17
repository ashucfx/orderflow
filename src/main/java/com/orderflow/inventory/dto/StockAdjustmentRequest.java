package com.orderflow.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustmentRequest {

    @NotNull(message = "Quantity change amount is required")
    private Integer quantity;

    @NotBlank(message = "Adjustment reason is required")
    @Size(max = 255, message = "Reason must not exceed 255 characters")
    private String reason;
}
