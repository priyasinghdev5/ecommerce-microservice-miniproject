package com.ecom.product.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MongoDB document — the write model (source of truth).
 * Supports dynamic attributes (color, size, material) via Map.
 */
@Document(collection = "products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String sku;

    private String name;
    private String description;
    private String brand;

    @Indexed
    private List<String> categories = new ArrayList<>();

    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private List<String> imageUrls = new ArrayList<>();
    private boolean active = true;

    // Dynamic attributes — varies by category
    // e.g. { "color": "red", "size": "XL", "material": "cotton" }
    private Map<String, Object> attributes;

    private List<Variant> variants = new ArrayList<>();
    private double averageRating = 0.0;
    private int reviewCount = 0;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Variant {
        private String variantId;
        private String sku;
        private Map<String, String> options;  // { "color": "blue", "size": "M" }
        private BigDecimal price;
        private int stock;
    }
}
