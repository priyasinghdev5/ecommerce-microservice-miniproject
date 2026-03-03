package com.ecom.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_templates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;          // order_confirmed, payment_failed, order_shipped

    @Column(nullable = false)
    private String channel;       // EMAIL, SMS

    private String subject;       // for EMAIL only

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;          // Thymeleaf template or SMS text

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
