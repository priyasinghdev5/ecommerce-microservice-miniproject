package com.ecom.bff.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserProfileDto(UUID id, String firstName, String lastName,
        String fullName, String phone, String avatarUrl) {
    public static UserProfileDto empty() {
        return new UserProfileDto(null, "Guest", "", "Guest User", null, null);
    }
}
