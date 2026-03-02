package com.ecom.common.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by inventory-service to topic: stock.released
 * Consumed by: order-service — confirms compensation is complete
 *
 * This is the compensation/rollback event published after PaymentFailedEvent.
 * inventory-service deletes reservations and frees the stock.
 */
public record StockReleasedEvent(
        UUID eventId,
        UUID orderId,
        String reason,           // e.g. "PAYMENT_FAILED", "ORDER_CANCELLED"
        Instant releasedAt
) {}
