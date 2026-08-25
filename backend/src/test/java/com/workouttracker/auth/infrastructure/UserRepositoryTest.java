package com.workouttracker.auth.infrastructure;

import com.workouttracker.auth.application.EmailNormalizer;
import com.workouttracker.auth.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void savesUser() {
        User user = new User("Vishal", EmailNormalizer.normalize("vishal@example.com"), "$2a$10$hash");

        User saved = userRepository.saveAndFlush(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getDisplayName()).isEqualTo("Vishal");
        assertThat(saved.getEmail()).isEqualTo("vishal@example.com");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void findsUserByNormalizedEmail() {
        String email = EmailNormalizer.normalize("User@Example.COM");
        userRepository.saveAndFlush(new User("Vishal", email, "$2a$10$hash"));

        assertThat(userRepository.findByEmail(email)).isPresent();
        assertThat(userRepository.findByEmail("user@example.com")).isPresent();
    }

    @Test
    void existsByEmailReflectsPersistedUser() {
        String email = EmailNormalizer.normalize("exists@example.com");
        userRepository.saveAndFlush(new User("Vishal", email, "$2a$10$hash"));

        assertThat(userRepository.existsByEmail(email)).isTrue();
        assertThat(userRepository.existsByEmail("missing@example.com")).isFalse();
    }

    @Test
    void rejectsDuplicateEmail() {
        String email = EmailNormalizer.normalize("duplicate@example.com");
        userRepository.saveAndFlush(new User("First", email, "$2a$10$hash"));

        User duplicate = new User("Second", email, "$2a$10$other");

        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
