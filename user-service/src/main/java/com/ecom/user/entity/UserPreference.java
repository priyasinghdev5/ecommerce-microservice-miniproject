package com.ecom.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_preferences")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserPreference {

    @Id
    private UUID userId;   // same PK as user_profiles.id

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserProfile userProfile;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(nullable = false)
    private String language = "en";

    @Column(nullable = false)
    private boolean notificationsEmail = true;

    @Column(nullable = false)
    private boolean notificationsSms = false;

    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() { this.updatedAt = Instant.now(); }
}
