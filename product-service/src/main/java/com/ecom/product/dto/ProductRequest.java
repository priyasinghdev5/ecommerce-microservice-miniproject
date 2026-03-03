package com.ecom.product.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ProductRequest(
        @NotBlank String sku,
        @NotBlank String name,
        String description,
        String brand,
        List<String> categories,
        @NotNull @Positive BigDecimal price,
        BigDecimal compareAtPrice,
        List<String> imageUrls,
        boolean active,
        Map<String, Object> attributes
) {}
