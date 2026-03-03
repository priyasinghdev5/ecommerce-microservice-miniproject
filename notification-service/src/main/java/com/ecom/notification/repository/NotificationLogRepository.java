package com.ecom.notification.repository;

import com.ecom.notification.entity.NotificationLog;
import com.ecom.notification.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {
    List<NotificationLog> findByUserId(UUID userId);
    List<NotificationLog> findByStatus(NotificationStatus status);
}
