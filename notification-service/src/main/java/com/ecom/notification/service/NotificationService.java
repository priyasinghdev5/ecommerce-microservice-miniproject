package com.ecom.notification.service;

import com.ecom.common.events.NotificationEvent;
import com.ecom.notification.entity.*;
import com.ecom.notification.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationLogRepository logRepository;
    private final NotificationTemplateRepository templateRepository;
    private final EmailService emailService;
    private final SmsService smsService;

    /**
     * Process a NotificationEvent from Kafka.
     * 1. Look up template by name
     * 2. Log to DB (status=SENDING)
     * 3. Send via email or SMS
     * 4. Update log (SENT or FAILED)
     */
    @Transactional
    public void process(NotificationEvent event) {
        log.info("Processing notification: userId={} channel={} template={}",
                event.userId(), event.channel(), event.templateName());

        // Find template
        NotificationTemplate template = templateRepository
                .findByNameAndActiveTrue(event.templateName())
                .orElseGet(() -> buildFallbackTemplate(event));

        // Create log entry
        NotificationLog notifLog = NotificationLog.builder()
                .userId(event.userId())
                .channel(event.channel())
                .recipient(event.recipient())
                .subject(template.getSubject())
                .body(template.getBody())
                .templateName(event.templateName())
                .status(NotificationStatus.SENDING)
                .attempts((short) 1)
                .build();
        notifLog = logRepository.save(notifLog);

        try {
            String refId = switch (event.channel()) {
                case "EMAIL" -> emailService.sendHtmlEmail(
                        event.recipient(),
                        template.getSubject() != null ? template.getSubject() : "Notification",
                        resolveEmailTemplate(event.templateName()),
                        event.variables() != null ? event.variables() : new HashMap<>()
                );
                case "SMS" -> smsService.sendSms(event.recipient(), template.getBody());
                default -> throw new RuntimeException("Unknown channel: " + event.channel());
            };

            notifLog.setStatus(NotificationStatus.SENT);
            notifLog.setReferenceId(refId);
            notifLog.setSentAt(Instant.now());
            log.info("Notification sent: id={} channel={} ref={}",
                    notifLog.getId(), event.channel(), refId);

        } catch (Exception e) {
            notifLog.setStatus(NotificationStatus.FAILED);
            notifLog.setErrorMessage(e.getMessage());
            log.error("Notification failed: channel={} to={} error={}",
                    event.channel(), event.recipient(), e.getMessage());
            throw new RuntimeException("Notification send failed: " + e.getMessage(), e);
        } finally {
            logRepository.save(notifLog);
        }
    }

    private String resolveEmailTemplate(String templateName) {
        // Maps template names to Thymeleaf template files in resources/templates/email/
        return "email/" + templateName;
    }

    private NotificationTemplate buildFallbackTemplate(NotificationEvent event) {
        // If no template found in DB, use a generic fallback
        log.warn("Template not found: {} — using fallback", event.templateName());
        NotificationTemplate fallback = new NotificationTemplate();
        fallback.setName(event.templateName());
        fallback.setChannel(event.channel());
        fallback.setSubject("Notification from eCommerce Platform");
        fallback.setBody("You have a new notification. Please check your account.");
        fallback.setActive(true);
        return fallback;
    }
}
