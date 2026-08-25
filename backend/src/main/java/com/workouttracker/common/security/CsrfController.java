package com.workouttracker.common.security;

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
public class CsrfController {

    @GetMapping("/csrf")
    public CsrfTokenResponse csrf(CsrfToken csrfToken) {
        return new CsrfTokenResponse(SecurityConfig.CSRF_HEADER_NAME, csrfToken.getToken());
    }

    public record CsrfTokenResponse(String headerName, String token) {
    }
}
