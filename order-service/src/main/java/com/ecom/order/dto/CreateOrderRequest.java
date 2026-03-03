package com.ecom.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record CreateOrderRequest(
        @NotBlank String shippingAddress,
        @NotEmpty @Valid List<OrderItemRequest> items,
        @NotBlank String currency
) {
    public record OrderItemRequest(
            @NotNull java.util.UUID productId,
            @NotBlank String sku,
            @Min(1) int quantity,
            @NotNull @Positive BigDecimal unitPrice
    ) {}
}
