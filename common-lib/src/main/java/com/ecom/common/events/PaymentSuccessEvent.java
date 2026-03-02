package com.ecom.common.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Published by payment-service to topic: payment.success
 * Consumed by: order-service (update order to CONFIRMED), notification-service
 */
public record PaymentSuccessEvent(
        UUID eventId,
        UUID orderId,
        UUID paymentId,
        UUID userId,
        BigDecimal amount,
        String currency,
        String gatewayRef,     // Stripe charge ID, PayPal transaction ID, etc.
        Instant paidAt
) {}
