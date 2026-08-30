package com.workouttracker.common.security;

import com.workouttracker.common.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the current CSRF token for the SPA.
 * The session cookie remains HTTP-only; only the CSRF token is returned to JavaScript.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Registration, login, session profile, and logout")
public class CsrfController {

    @GetMapping("/csrf")
    @SecurityRequirements
    @Operation(
            summary = "Get CSRF token",
            description = "Returns the CSRF header name and token value for unsafe API requests."
    )
    @ApiResponse(responseCode = "200", description = "CSRF token information")
    public CsrfTokenResponse csrf(CsrfToken csrfToken) {
        return new CsrfTokenResponse(SecurityConfig.CSRF_HEADER_NAME, csrfToken.getToken());
    }

    @Schema(description = "CSRF token details for unsafe HTTP methods")
    public record CsrfTokenResponse(
            @Schema(description = "Request header name for the CSRF token", example = "X-XSRF-TOKEN")
            String headerName,
            @Schema(description = "CSRF token value to send in the header on unsafe requests")
            String token
    ) {
    }
}
