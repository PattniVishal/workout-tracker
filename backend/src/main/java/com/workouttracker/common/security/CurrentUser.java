package com.workouttracker.common.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the authenticated user from the Spring Security context.
 * Services should use this rather than trusting user identifiers from request bodies.
 */
@Component
public class CurrentUser {

    public Optional<UserPrincipal> getPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        if (authentication.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal);
        }

        return Optional.empty();
    }

    public UUID requireUserId() {
        return getPrincipal()
                .map(UserPrincipal::getId)
                .orElseThrow(AuthenticationRequiredException::new);
    }
}
