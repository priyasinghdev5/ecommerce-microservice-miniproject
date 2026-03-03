package com.ecom.order.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Polls outbox_events every 500ms.
 * For each NEW/FAILED event:
 *   - Attempt Kafka publish
 *   - On success: mark SENT
 *   - On failure: increment retry_count, mark FAILED after 3 attempts
 *
 * This guarantees at-least-once delivery to Kafka.
 * Consumers must be idempotent (check eventId for duplicates).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private static final short MAX_RETRIES = 3;

    private final OutboxRepository outboxRepository;
    private final OutboxPublisher outboxPublisher;

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void processOutbox() {
        List<OutboxEvent> pending = outboxRepository.findPendingEvents(MAX_RETRIES);

        for (OutboxEvent event : pending) {
            try {
                outboxPublisher.publish(event);
                event.setStatus(OutboxEvent.OutboxStatus.SENT);
                event.setProcessedAt(Instant.now());
                log.info("Outbox published: type={} orderId={}", event.getEventType(), event.getAggregateId());
            } catch (Exception e) {
                short newCount = (short)(event.getRetryCount() + 1);
                event.setRetryCount(newCount);
                event.setErrorMessage(e.getMessage());
                if (newCount >= MAX_RETRIES) {
                    event.setStatus(OutboxEvent.OutboxStatus.FAILED);
                    log.error("Outbox permanently failed after {} retries: type={} id={}",
                            MAX_RETRIES, event.getEventType(), event.getId());
                } else {
                    log.warn("Outbox publish failed (attempt {}): {}", newCount, e.getMessage());
                }
            }
            outboxRepository.save(event);
        }
    }
}
