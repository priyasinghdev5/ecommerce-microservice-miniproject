package com.ecom.order.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publish(OutboxEvent event) {
        kafkaTemplate.send(event.getTopic(), event.getAggregateId().toString(), event.getPayload())
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        throw new RuntimeException("Kafka publish failed: " + ex.getMessage());
                    }
                    log.debug("Published to topic={} key={}", event.getTopic(), event.getAggregateId());
                });
    }
}
