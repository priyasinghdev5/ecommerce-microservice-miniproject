package com.ecom.user.dto;

import java.util.UUID;

public record AddressResponse(
        UUID id,
        String label,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String countryCode,
        boolean isDefault
) {}
