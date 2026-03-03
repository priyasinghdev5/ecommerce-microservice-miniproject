package com.ecom.order.dto;

import com.ecom.order.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID userId,
        OrderStatus status,
        BigDecimal totalAmount,
        String currency,
        String shippingAddress,
        List<OrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {
    public record OrderItemResponse(
            UUID id,
            UUID productId,
            String sku,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {}
}
