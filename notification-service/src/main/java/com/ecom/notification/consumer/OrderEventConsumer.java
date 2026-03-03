package com.ecom.notification.consumer;

import com.ecom.common.events.*;
import com.ecom.common.kafka.KafkaTopics;
import com.ecom.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Directly consumes order/payment domain events and converts them
 * into NotificationEvents for processing.
 *
 * This allows notification-service to react to domain events
 * without requiring every service to manually publish NotificationEvents.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = KafkaTopics.PAYMENT_SUCCESS, groupId = "notification-order-events")
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        log.info("Creating order confirmation notification for orderId={}", event.orderId());
        Map<String, Object> vars = new HashMap<>();
        vars.put("orderId", event.orderId());
        vars.put("amount", event.amount());
        vars.put("currency", event.currency());
        vars.put("gatewayRef", event.gatewayRef());

        NotificationEvent notification = new NotificationEvent(
                UUID.randomUUID(), event.userId(), "EMAIL",
                "customer@example.com",   // In production: fetch from user-service
                "order_confirmed", vars, java.time.Instant.now()
        );
        notificationService.process(notification);
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_FAILED, groupId = "notification-order-events")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("Creating payment failed notification for orderId={}", event.orderId());
        Map<String, Object> vars = new HashMap<>();
        vars.put("orderId", event.orderId());
        vars.put("amount", event.amount());
        vars.put("reason", event.failureReason());

        NotificationEvent notification = new NotificationEvent(
                UUID.randomUUID(), event.userId(), "EMAIL",
                "customer@example.com",
                "payment_failed", vars, java.time.Instant.now()
        );
        notificationService.process(notification);
    }
}
