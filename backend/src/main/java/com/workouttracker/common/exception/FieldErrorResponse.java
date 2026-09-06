package com.workouttracker.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Field-level validation error")
public record FieldErrorResponse(
        @Schema(description = "Request field name", example = "email") String field,
        @Schema(description = "Validation message", example = "must not be blank") String message
) {
}
