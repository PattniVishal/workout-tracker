package com.workouttracker.auth.application;

import com.workouttracker.auth.api.RegisterRequest;
import com.workouttracker.auth.api.UserResponse;
import com.workouttracker.common.security.AuthenticationRequiredException;
import com.workouttracker.common.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceMeTest {

    @Autowired
    private AuthService authService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsCurrentUserProfileFromSecurityContext() {
        UserResponse registered = authService.register(
                new RegisterRequest("Vishal", "me-service@example.com", "secret-password"));

        UserPrincipal principal = new UserPrincipal(
                registered.id(),
                registered.displayName(),
                registered.email(),
                "hashed-password"
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        UserResponse profile = authService.getCurrentUserProfile();

        assertThat(profile.id()).isEqualTo(registered.id());
        assertThat(profile.displayName()).isEqualTo("Vishal");
        assertThat(profile.email()).isEqualTo("me-service@example.com");
        assertThat(profile.toString()).doesNotContain("hashed-password");
    }

    @Test
    void getCurrentUserProfileRequiresAuthenticatedPrincipal() {
        assertThatThrownBy(authService::getCurrentUserProfile)
                .isInstanceOf(AuthenticationRequiredException.class);
    }
}
