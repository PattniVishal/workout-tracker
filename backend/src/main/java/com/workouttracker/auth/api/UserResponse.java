package com.workouttracker.auth.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Authenticated user profile")
public record UserResponse(
        @Schema(description = "User ID") UUID id,
        @Schema(description = "Display name", example = "Vishal") String displayName,
        @Schema(description = "Email address", example = "user@example.com") String email
) {
}
