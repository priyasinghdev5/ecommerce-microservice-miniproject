package com.ecom.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard API envelope used by every service.
 *
 * Success:  ApiResponse.ok(data)
 * Error:    ApiResponse.error(ErrorResponse.of(...))
 *
 * Example response:
 * {
 *   "success": true,
 *   "message": "Order created",
 *   "data": { ... }
 * }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        ErrorResponse error
) {

    /** 200 / 201 — data only */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, null, data, null);
    }

    /** 200 / 201 — with message */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    /** 200 — message only (e.g. DELETE success) */
    public static <T> ApiResponse<T> ok(String message) {
        return new ApiResponse<>(true, message, null, null);
    }

    /** 4xx / 5xx */
    public static <T> ApiResponse<T> error(ErrorResponse error) {
        return new ApiResponse<>(false, null, null, error);
    }
}
