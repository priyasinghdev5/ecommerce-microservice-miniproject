package com.ecom.notification.consumer;

import com.ecom.common.events.NotificationEvent;
import com.ecom.common.kafka.KafkaTopics;
import com.ecom.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for notification events.
 *
 * Uses @RetryableTopic for automatic retry with exponential backoff.
 * On exhaustion: message goes to DLQ (notification.events.dlq)
 * for manual inspection or replay.
 *
 * Retry schedule:
 *   Attempt 1: immediate
 *   Attempt 2: after 1 second
 *   Attempt 3: after 2 seconds
 *   Attempt 4: after 4 seconds → DLQ
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    @RetryableTopic(
        attempts = "4",
        backoff = @Backoff(delay = 1000, multiplier = 2),
        topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
        dltTopicSuffix = ".dlq"
    )
    @KafkaListener(topics = KafkaTopics.NOTIFICATION_EVENTS, groupId = "notification-service")
    public void consume(NotificationEvent event) {
        log.info("NotificationEvent received: userId={} channel={} template={}",
                event.userId(), event.channel(), event.templateName());
        notificationService.process(event);
    }

    /**
     * DLQ handler — receives messages that failed all retry attempts.
     * Log for manual review / alerting.
     */
    @KafkaListener(topics = KafkaTopics.NOTIFICATION_DLQ, groupId = "notification-dlq-handler")
    public void handleDlq(NotificationEvent event) {
        log.error("DEAD LETTER: Notification permanently failed after all retries. " +
                "userId={} channel={} template={} recipient={}",
                event.userId(), event.channel(), event.templateName(), event.recipient());
        // In production: alert PagerDuty / Slack, store for manual replay
    }
}
