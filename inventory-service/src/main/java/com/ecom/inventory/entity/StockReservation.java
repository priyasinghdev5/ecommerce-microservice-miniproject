package com.ecom.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock_reservations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private String status = "RESERVED";   // RESERVED, RELEASED, CONFIRMED

    @Column(nullable = false)
    private Instant expiresAt = Instant.now().plusSeconds(1800); // 30 min

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
