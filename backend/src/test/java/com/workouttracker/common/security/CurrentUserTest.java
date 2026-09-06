package com.workouttracker.common.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserTest {

    private final CurrentUser currentUser = new CurrentUser();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsPrincipalWhenAuthenticated() {
        UserPrincipal principal = new UserPrincipal(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "Vishal",
                "user@example.com",
                "hashed-password"
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        Optional<UserPrincipal> resolved = currentUser.getPrincipal();

        assertThat(resolved).contains(principal);
        assertThat(currentUser.requireUserId())
                .isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    }

    @Test
    void requireUserIdFailsWhenUnauthenticated() {
        assertThat(currentUser.getPrincipal()).isEmpty();
        assertThatThrownBy(currentUser::requireUserId)
                .isInstanceOf(AuthenticationRequiredException.class);
    }
}
