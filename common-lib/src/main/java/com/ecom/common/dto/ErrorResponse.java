package com.ecom.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * Standard error body, always wrapped inside ApiResponse.error().
 *
 * Example:
 * {
 *   "success": false,
 *   "error": {
 *     "code": "ORDER_NOT_FOUND",
 *     "message": "Order with id abc123 not found",
 *     "path": "/api/orders/abc123",
 *     "timestamp": "2024-01-01T10:00:00Z",
 *     "fieldErrors": [
 *       { "field": "quantity", "message": "must be greater than 0" }
 *     ]
 *   }
 * }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        String message,
        String path,
        Instant timestamp,
        List<FieldError> fieldErrors
) {

    /** Generic error — code + message */
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, null, Instant.now(), null);
    }

    /** With request path */
    public static ErrorResponse of(String code, String message, String path) {
        return new ErrorResponse(code, message, path, Instant.now(), null);
    }

    /** Validation errors — includes per-field details */
    public static ErrorResponse validation(String path, List<FieldError> fieldErrors) {
        return new ErrorResponse(
                "VALIDATION_FAILED",
                "Request validation failed",
                path,
                Instant.now(),
                fieldErrors
        );
    }

    /**
     * Individual field-level validation error.
     * e.g. { "field": "email", "message": "must be a valid email" }
     */
    public record FieldError(String field, String message) {}
}
