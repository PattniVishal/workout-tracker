package com.workouttracker.auth.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Login request")
public record LoginRequest(
        @Schema(description = "Email address", example = "user@example.com", maxLength = 320)
        @NotBlank @Email @Size(max = 320) String email,
        @Schema(description = "Plain-text password", writeOnly = true)
        @NotBlank String password
) {
}
