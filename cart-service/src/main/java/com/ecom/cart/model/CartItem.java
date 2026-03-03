package com.ecom.cart.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Stored as JSON in Redis Hash.
 * Key:   cart:{userId}
 * Field: {productId}
 * Value: CartItem JSON
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CartItem {
    private String productId;
    private String sku;
    private String name;
    private int quantity;
    private BigDecimal unitPrice;
    private String imageUrl;
    private Instant addedAt;
}
