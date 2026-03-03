package com.ecom.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateInventoryRequest(
        @NotNull UUID productId,
        @NotBlank String sku,
        @Min(0) int initialQuantity,
        @Min(0) int reorderPoint,
        String warehouseLoc
) {}
