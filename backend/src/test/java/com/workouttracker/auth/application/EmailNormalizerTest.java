package com.workouttracker.auth.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailNormalizerTest {

    @Test
    void normalizesEmailToLowercaseAndTrimsWhitespace() {
        assertThat(EmailNormalizer.normalize("  User@Example.COM "))
                .isEqualTo("user@example.com");
    }
}
