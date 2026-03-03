package com.ecom.user.dto;

public record PreferenceRequest(
        String currency,
        String language,
        boolean notificationsEmail,
        boolean notificationsSms
) {}
