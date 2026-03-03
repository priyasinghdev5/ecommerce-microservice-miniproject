package com.ecom.bff.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderSummaryDto(UUID id, String status, BigDecimal totalAmount,
        String currency, Instant createdAt) {}
