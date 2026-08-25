package com.workouttracker.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 100) String displayName,
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank String password
) {
}
