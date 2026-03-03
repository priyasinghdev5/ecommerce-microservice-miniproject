package com.ecom.bff.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CartItemDto(String productId, String sku, String name,
        int quantity, BigDecimal unitPrice, String imageUrl) {}
