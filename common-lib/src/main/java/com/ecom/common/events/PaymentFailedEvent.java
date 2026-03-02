package com.ecom.common.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Published by payment-service to topic: payment.failed
 * Consumed by:
 *   - inventory-service  → compensation: release reserved stock
 *   - order-service      → update order to PAYMENT_FAILED
 *   - notification-service → send failure email to user
 */
public record PaymentFailedEvent(
        UUID eventId,
        UUID orderId,
        UUID userId,
        BigDecimal amount,
        String currency,
        String failureReason,   // e.g. "Insufficient funds", "Card declined"
        String gatewayCode,     // raw gateway error code
        Instant failedAt
) {}
