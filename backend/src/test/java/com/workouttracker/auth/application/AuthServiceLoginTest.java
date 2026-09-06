package com.workouttracker.auth.application;

import com.workouttracker.auth.api.LoginRequest;
import com.workouttracker.auth.api.RegisterRequest;
import com.workouttracker.auth.domain.User;
import com.workouttracker.auth.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceLoginTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void authenticatesUserWithCorrectCredentials() {
        authService.register(new RegisterRequest("Vishal", "login@example.com", "secret-password"));

        User user = authService.authenticate(new LoginRequest("login@example.com", "secret-password"));

        assertThat(user.getEmail()).isEqualTo("login@example.com");
        assertThat(user.getDisplayName()).isEqualTo("Vishal");
    }

    @Test
    void normalizesEmailDuringLogin() {
        authService.register(new RegisterRequest("Vishal", "normalized@example.com", "secret-password"));

        User user = authService.authenticate(new LoginRequest("  Normalized@Example.COM ", "secret-password"));

        assertThat(user.getEmail()).isEqualTo("normalized@example.com");
    }

    @Test
    void rejectsUnknownEmail() {
        assertThatThrownBy(() -> authService.authenticate(new LoginRequest("missing@example.com", "secret-password")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password.");
    }

    @Test
    void rejectsIncorrectPassword() {
        authService.register(new RegisterRequest("Vishal", "wrong-password@example.com", "secret-password"));

        assertThatThrownBy(() -> authService.authenticate(
                new LoginRequest("wrong-password@example.com", "incorrect-password")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password.");
    }

    @Test
    void usesSameFailureMessageForUnknownEmailAndIncorrectPassword() {
        InvalidCredentialsException unknownEmail = catchInvalidCredentials(
                new LoginRequest("missing@example.com", "secret-password"));
        authService.register(new RegisterRequest("Vishal", "known@example.com", "secret-password"));
        InvalidCredentialsException wrongPassword = catchInvalidCredentials(
                new LoginRequest("known@example.com", "wrong-password"));

        assertThat(unknownEmail.getMessage()).isEqualTo(wrongPassword.getMessage());
    }

    private InvalidCredentialsException catchInvalidCredentials(LoginRequest request) {
        try {
            authService.authenticate(request);
            throw new AssertionError("Expected InvalidCredentialsException");
        } catch (InvalidCredentialsException ex) {
            return ex;
        }
    }
}
