package com.faezeh.commerce.order.exception;

import java.time.Instant;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Consistent API error response")
public record ApiErrorResponse(
        @Schema(description = "Time the error response was created", example = "2026-09-09T18:00:00Z")
        Instant timestamp,

        @Schema(description = "HTTP status code", example = "404")
        int status,

        @Schema(description = "HTTP status reason", example = "Not Found")
        String error,

        @Schema(description = "Human-readable error message", example = "Order not found with id: 99")
        String message,

        @Schema(description = "Request path", example = "/api/orders/99")
        String path,

        @Schema(description = "Validation errors keyed by field name")
        Map<String, String> fieldErrors
) {

    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(Instant.now(), status, error, message, path, Map.of());
    }

    public static ApiErrorResponse validation(
            int status,
            String error,
            String message,
            String path,
            Map<String, String> fieldErrors
    ) {
        return new ApiErrorResponse(Instant.now(), status, error, message, path, fieldErrors);
    }
}
