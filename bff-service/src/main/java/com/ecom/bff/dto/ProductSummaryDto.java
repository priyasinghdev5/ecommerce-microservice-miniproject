package com.ecom.bff.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductSummaryDto(String id, String sku, String name, String brand,
        BigDecimal price, BigDecimal compareAtPrice, List<String> imageUrls,
        List<String> categories, double averageRating) {}
