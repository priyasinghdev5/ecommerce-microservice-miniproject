package com.ecom.cart.dto;

import com.ecom.cart.model.CartItem;
import java.math.BigDecimal;
import java.util.List;

public record CartSummary(
        String userId,
        List<CartItem> items,
        int totalItems,
        BigDecimal totalAmount
) {}
