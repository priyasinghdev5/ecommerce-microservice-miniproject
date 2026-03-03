package com.ecom.inventory.dto;

public record StockResponse(
        String sku,
        int availableQty,
        int totalQuantity,
        int reservedQty
) {}
