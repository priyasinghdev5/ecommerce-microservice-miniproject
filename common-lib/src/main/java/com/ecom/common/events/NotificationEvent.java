package com.ecom.common.events;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Published by order-service / payment-service to topic: notification.events
 * Consumed by: notification-service
 *
 * notification-service uses templateName to look up the Thymeleaf template
 * and renders it with the provided variables map.
 *
 * Template names match rows in notification_templates table:
 *   "order_confirmed", "payment_failed", "order_shipped", etc.
 */
public record NotificationEvent(
        UUID eventId,
        UUID userId,
        String channel,          // "EMAIL" or "SMS"
        String recipient,        // email address or phone number
        String templateName,     // matches notification_templates.name
        Map<String, Object> variables,  // injected into Thymeleaf template
        Instant createdAt
) {}
