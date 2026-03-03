package com.ecom.product.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ProductResponse(
        String id,
        String sku,
        String name,
        String description,
        String brand,
        List<String> categories,
        BigDecimal price,
        BigDecimal compareAtPrice,
        List<String> imageUrls,
        boolean active,
        Map<String, Object> attributes,
        double averageRating,
        int reviewCount,
        Instant createdAt,
        Instant updatedAt
) {}
