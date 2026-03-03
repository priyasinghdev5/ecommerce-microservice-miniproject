package com.ecom.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    private int reservedQty;

    @Column(nullable = false)
    private int reorderPoint = 10;

    private String warehouseLoc;

    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() { this.updatedAt = Instant.now(); }

    public int getAvailableQty() {
        return totalQuantity - reservedQty;
    }

    public boolean isAvailable(int requestedQty) {
        return getAvailableQty() >= requestedQty;
    }
}
