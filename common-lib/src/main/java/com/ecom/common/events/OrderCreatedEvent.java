package com.ecom.common.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Published by order-service to topic: order.created
 * Consumed by: inventory-service, payment-service, notification-service
 *
 * Triggers the Saga — first event in the checkout flow.
 */
public record OrderCreatedEvent(
        UUID eventId,          // unique event ID (for idempotency)
        UUID orderId,
        UUID userId,
        List<OrderItem> items,
        BigDecimal totalAmount,
        String currency,
        String shippingAddress,
        Instant createdAt
) {

    /**
     * Individual line item inside the order.
     * inventory-service uses productId + sku + quantity to reserve stock.
     */
    public record OrderItem(
            UUID productId,
            String sku,
            int quantity,
            BigDecimal unitPrice
    ) {}
}
