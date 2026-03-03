package com.ecom.bff.dto;
import java.math.BigDecimal;
import java.util.List;

public record CheckoutViewModel(
        List<CartItemDto> cartItems,
        int totalItems,
        BigDecimal totalAmount,
        UserProfileDto userProfile,
        List<AddressDto> addresses) {
    public record AddressDto(String id, String label, String addressLine1,
            String city, String postalCode, boolean isDefault) {}
}
