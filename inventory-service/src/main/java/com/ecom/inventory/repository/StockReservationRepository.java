package com.ecom.inventory.repository;

import com.ecom.inventory.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {

    List<StockReservation> findByOrderIdAndStatus(UUID orderId, String status);

    Optional<StockReservation> findByIdAndOrderId(UUID id, UUID orderId);

    // Find expired reservations for cleanup scheduler
    @Query("SELECT r FROM StockReservation r WHERE r.status = 'RESERVED' AND r.expiresAt < :now")
    List<StockReservation> findExpiredReservations(Instant now);

    @Modifying
    @Query("UPDATE StockReservation r SET r.status = :status WHERE r.orderId = :orderId AND r.status = 'RESERVED'")
    int updateStatusByOrderId(UUID orderId, String status);
}
