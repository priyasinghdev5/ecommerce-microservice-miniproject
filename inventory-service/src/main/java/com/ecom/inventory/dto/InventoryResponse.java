package com.ecom.inventory.dto;

import java.util.UUID;

public record InventoryResponse(
        UUID id,
        UUID productId,
        String sku,
        int totalQuantity,
        int reservedQty,
        int availableQty
) {}
