package com.ecom.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank String label,
        @NotBlank String addressLine1,
        String addressLine2,
        @NotBlank String city,
        String state,
        @NotBlank String postalCode,
        @NotBlank @Size(min = 2, max = 2) String countryCode,
        boolean isDefault
) {}
