package com.ecom.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import java.time.LocalDate;

public record UserProfileRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        String phone,
        String avatarUrl,
        @Past LocalDate dateOfBirth
) {}
