package com.ecom.payment.service;

import com.ecom.common.events.*;
import com.ecom.common.kafka.KafkaTopics;
import com.ecom.payment.dto.PaymentResponse;
import com.ecom.payment.entity.*;
import com.ecom.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Process payment for an order.
     * Called by the Kafka consumer when OrderCreatedEvent is received.
     *
     * Uses a MOCK gateway — replace with Stripe/Razorpay in production.
     * Publishes PaymentSuccessEvent or PaymentFailedEvent to Kafka.
     */
    @Transactional
    public void processPayment(OrderCreatedEvent event) {

        // Idempotency check — skip if already processed
        if (paymentRepository.existsByOrderId(event.orderId())) {
            log.warn("Payment already processed for orderId={}", event.orderId());
            return;
        }

        Payment payment = Payment.builder()
                .orderId(event.orderId())
                .userId(event.userId())
                .amount(event.totalAmount())
                .currency(event.currency())
                .status(PaymentStatus.PROCESSING)
                .paymentMethod("CARD")
                .gateway("MOCK_GATEWAY")
                .build();
        payment = paymentRepository.save(payment);

        // Mock gateway call — simulate 90% success rate
        boolean success = mockGatewayCharge(payment);

        if (success) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setGatewayRef("MOCK_TXN_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            paymentRepository.save(payment);

            PaymentSuccessEvent successEvent = new PaymentSuccessEvent(
                    UUID.randomUUID(), event.orderId(), payment.getId(),
                    event.userId(), event.totalAmount(), event.currency(),
                    payment.getGatewayRef(), Instant.now());
            kafkaTemplate.send(KafkaTopics.PAYMENT_SUCCESS, event.orderId().toString(), successEvent);
            log.info("Payment SUCCESS: orderId={} txn={}", event.orderId(), payment.getGatewayRef());

        } else {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);

            PaymentFailedEvent failEvent = new PaymentFailedEvent(
                    UUID.randomUUID(), event.orderId(), event.userId(),
                    event.totalAmount(), event.currency(),
                    "Insufficient funds", "GATEWAY_DECLINED", Instant.now());
            kafkaTemplate.send(KafkaTopics.PAYMENT_FAILED, event.orderId().toString(), failEvent);
            log.warn("Payment FAILED: orderId={}", event.orderId());
        }
    }

    public PaymentResponse getByOrderId(UUID orderId) {
        Payment p = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for order: " + orderId));
        return toResponse(p);
    }

    // ── Mock gateway ───────────────────────────────────────────────

    private boolean mockGatewayCharge(Payment payment) {
        // 90% success rate for testing
        // Replace with real Stripe/Razorpay SDK call in production
        log.info("Mock gateway charging: orderId={} amount={} {}",
                payment.getOrderId(), payment.getAmount(), payment.getCurrency());
        return Math.random() > 0.1;
    }

    private PaymentResponse toResponse(Payment p) {
        return new PaymentResponse(p.getId(), p.getOrderId(), p.getStatus(),
                p.getAmount(), p.getCurrency(), p.getPaymentMethod(),
                p.getGatewayRef(), p.getCreatedAt());
    }
}
