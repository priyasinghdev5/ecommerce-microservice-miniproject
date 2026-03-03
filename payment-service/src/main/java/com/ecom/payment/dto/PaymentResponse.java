package com.ecom.payment.dto;

import com.ecom.payment.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        String gatewayRef,
        Instant createdAt
) {}
