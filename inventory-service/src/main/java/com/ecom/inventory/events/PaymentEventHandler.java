package com.ecom.inventory.events;

import com.ecom.common.events.PaymentFailedEvent;
import com.ecom.common.kafka.KafkaTopics;
import com.ecom.inventory.entity.StockReservation;
import com.ecom.inventory.repository.StockReservationRepository;
import com.ecom.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Consumes PaymentFailedEvent from payment-service.
 * Compensates by releasing all stock reserved for the failed order.
 * This is the Saga compensation step.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventHandler {

    private final StockReservationRepository reservationRepository;
    private final InventoryService inventoryService;

    @KafkaListener(topics = KafkaTopics.PAYMENT_FAILED, groupId = "inventory-service")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("PaymentFailed received — releasing stock for orderId={}", event.orderId());

        List<StockReservation> reservations =
                reservationRepository.findByOrderIdAndStatus(event.orderId(), "RESERVED");

        for (StockReservation reservation : reservations) {
            inventoryService.releaseStock(event.orderId(), reservation.getId());
        }

        log.info("Compensation complete: released {} reservations for orderId={}",
                reservations.size(), event.orderId());
    }
}
