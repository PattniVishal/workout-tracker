package com.workouttracker.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Standard API error response")
public record ApiErrorResponse(
        @Schema(description = "HTTP status code", example = "400") int status,
        @Schema(description = "Machine-readable error code", example = "VALIDATION_ERROR") String code,
        @Schema(description = "Human-readable error message") String message,
        @Schema(description = "Field-level validation errors, when applicable")
        List<FieldErrorResponse> fieldErrors
) {

    public static ApiErrorResponse of(int status, String code, String message) {
        return new ApiErrorResponse(status, code, message, List.of());
    }
}
