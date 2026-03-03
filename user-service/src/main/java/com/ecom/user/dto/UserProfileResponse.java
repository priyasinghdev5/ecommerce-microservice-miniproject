package com.ecom.user.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        UUID authUserId,
        String firstName,
        String lastName,
        String fullName,
        String phone,
        String avatarUrl,
        LocalDate dateOfBirth,
        Instant createdAt
) {}
