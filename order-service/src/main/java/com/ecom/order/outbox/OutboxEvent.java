package com.ecom.order.outbox;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Transactional Outbox Pattern.
 * Written in the SAME transaction as the order — guarantees
 * the event is never lost even if the app crashes before Kafka publish.
 *
 * OutboxScheduler polls this table every 500ms and publishes to Kafka.
 */
@Entity
@Table(name = "outbox_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OutboxEvent {

    public enum OutboxStatus { NEW, SENT, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String aggregateType;   // e.g. "ORDER"

    @Column(nullable = false)
    private UUID aggregateId;       // e.g. orderId

    @Column(nullable = false)
    private String eventType;       // e.g. "OrderCreatedEvent"

    @Column(nullable = false)
    private String topic;           // Kafka topic name

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;         // JSON-serialized event

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status = OutboxStatus.NEW;

    @Column(nullable = false)
    private short retryCount = 0;

    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant processedAt;
}
