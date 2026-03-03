package com.ecom.cart.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record AddItemRequest(
        @NotBlank String productId,
        @NotBlank String sku,
        @NotBlank String name,
        @Min(1) int quantity,
        @NotNull @Positive BigDecimal unitPrice,
        String imageUrl
) {}
