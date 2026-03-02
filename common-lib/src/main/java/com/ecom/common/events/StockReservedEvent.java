package com.ecom.common.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Published by inventory-service to topic: stock.reserved
 * Consumed by: payment-service — signals that stock is held, proceed with payment
 *
 * This is the "happy path" continuation in the Saga after OrderCreatedEvent.
 */
public record StockReservedEvent(
        UUID eventId,
        UUID orderId,
        List<ReservationDetail> reservations,
        Instant reservedAt
) {

    /**
     * One reservation entry per order line item.
     */
    public record ReservationDetail(
            UUID inventoryId,
            UUID reservationId,
            String sku,
            int quantityReserved
    ) {}
}
