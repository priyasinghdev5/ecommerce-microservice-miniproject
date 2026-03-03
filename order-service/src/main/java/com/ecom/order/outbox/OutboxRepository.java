package com.ecom.order.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Fetches only NEW or FAILED events with retries remaining.
     * Uses partial index for performance — scans only pending rows.
     */
    @Query("SELECT o FROM OutboxEvent o WHERE o.status IN ('NEW','FAILED') AND o.retryCount < :maxRetries ORDER BY o.createdAt ASC")
    List<OutboxEvent> findPendingEvents(short maxRetries);
}
