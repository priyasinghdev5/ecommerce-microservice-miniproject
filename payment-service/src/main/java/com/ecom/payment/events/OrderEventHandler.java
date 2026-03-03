package com.ecom.payment.events;

import com.ecom.common.events.OrderCreatedEvent;
import com.ecom.common.kafka.KafkaTopics;
import com.ecom.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes OrderCreatedEvent — triggers payment processing.
 * This is the Saga step after stock reservation succeeds.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventHandler {

    private final PaymentService paymentService;

    @KafkaListener(topics = KafkaTopics.ORDER_CREATED, groupId = "payment-service")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("OrderCreated received: orderId={} amount={}",
                event.orderId(), event.totalAmount());
        try {
            paymentService.processPayment(event);
        } catch (Exception e) {
            log.error("Payment processing failed for orderId={}: {}", event.orderId(), e.getMessage());
        }
    }
}
