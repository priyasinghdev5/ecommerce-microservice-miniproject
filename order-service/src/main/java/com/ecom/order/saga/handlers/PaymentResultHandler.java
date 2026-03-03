package com.ecom.order.saga.handlers;

import com.ecom.common.events.PaymentFailedEvent;
import com.ecom.common.events.PaymentSuccessEvent;
import com.ecom.common.kafka.KafkaTopics;
import com.ecom.order.entity.OrderStatus;
import com.ecom.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Saga step — listens for payment outcomes and updates order status.
 *
 * PaymentSuccessEvent → Order CONFIRMED
 * PaymentFailedEvent  → Order PAYMENT_FAILED
 *                       (inventory compensation handled by inventory-service itself)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentResultHandler {

    private final OrderService orderService;

    @KafkaListener(topics = KafkaTopics.PAYMENT_SUCCESS, groupId = "order-service-saga")
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        log.info("Payment success received: orderId={}", event.orderId());
        orderService.updateOrderStatus(event.orderId(), OrderStatus.CONFIRMED,
                "Payment confirmed via gateway ref: " + event.gatewayRef());
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_FAILED, groupId = "order-service-saga")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("Payment failed received: orderId={} reason={}", event.orderId(), event.failureReason());
        orderService.updateOrderStatus(event.orderId(), OrderStatus.PAYMENT_FAILED,
                "Payment failed: " + event.failureReason());
    }
}
