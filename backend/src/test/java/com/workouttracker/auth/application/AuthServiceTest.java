package com.workouttracker.auth.application;

import com.workouttracker.auth.api.RegisterRequest;
import com.workouttracker.auth.api.UserResponse;
import com.workouttracker.auth.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registersUserSuccessfully() {
        RegisterRequest request = new RegisterRequest("Vishal", "vishal@example.com", "secret-password");

        UserResponse response = authService.register(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.displayName()).isEqualTo("Vishal");
        assertThat(response.email()).isEqualTo("vishal@example.com");

        var saved = userRepository.findByEmail("vishal@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("secret-password", saved.getPasswordHash())).isTrue();
    }

    @Test
    void normalizesEmailBeforePersistence() {
        RegisterRequest request = new RegisterRequest("Vishal", "  User@Example.COM ", "secret-password");

        UserResponse response = authService.register(request);

        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(userRepository.existsByEmail("user@example.com")).isTrue();
    }

    @Test
    void rejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("Vishal", "duplicate@example.com", "secret-password");
        authService.register(request);

        RegisterRequest duplicate = new RegisterRequest("Other", "duplicate@example.com", "other-password");

        assertThatThrownBy(() -> authService.register(duplicate))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }

    @Test
    void doesNotExposePasswordOrHashInResponse() {
        UserResponse response = authService.register(
                new RegisterRequest("Vishal", "safe@example.com", "secret-password")
        );

        assertThat(response).hasNoNullFieldsOrPropertiesExcept();
        assertThat(response.toString()).doesNotContain("secret-password");
        assertThat(response.toString()).doesNotContain("$2");
    }

    @Test
    void encodesPasswordBeforePersistence() {
        authService.register(new RegisterRequest("Vishal", "encoded@example.com", "secret-password"));

        var saved = userRepository.findByEmail("encoded@example.com").orElseThrow();
        assertThat(saved.getPasswordHash()).isNotEqualTo("secret-password");
        assertThat(saved.getPasswordHash()).startsWith("$2");
    }
}
