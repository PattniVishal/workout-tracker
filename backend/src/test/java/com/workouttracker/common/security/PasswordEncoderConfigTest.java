package com.workouttracker.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PasswordEncoderConfigTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void encodesPasswordAsNonPlaintext() {
        String encoded = passwordEncoder.encode("secret-password");

        assertThat(encoded).isNotEqualTo("secret-password");
        assertThat(encoded).startsWith("$2");
    }

    @Test
    void verifiesMatchingPassword() {
        String encoded = passwordEncoder.encode("secret-password");

        assertThat(passwordEncoder.matches("secret-password", encoded)).isTrue();
        assertThat(passwordEncoder.matches("wrong-password", encoded)).isFalse();
    }
}
