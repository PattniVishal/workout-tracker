package com.workouttracker.auth.application;

import com.workouttracker.auth.api.LoginRequest;
import com.workouttracker.auth.api.RegisterRequest;
import com.workouttracker.auth.api.UserResponse;
import com.workouttracker.auth.domain.User;
import com.workouttracker.auth.infrastructure.UserRepository;
import com.workouttracker.common.security.AuthenticationRequiredException;
import com.workouttracker.common.security.CurrentUser;
import com.workouttracker.common.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUser currentUser;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            CurrentUser currentUser
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUser = currentUser;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String normalizedEmail = EmailNormalizer.normalize(request.email());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException();
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(request.displayName().trim(), normalizedEmail, passwordHash);

        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyRegisteredException();
        }

        log.info("User registered userId={}", user.getId());
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public User authenticate(LoginRequest request) {
        String normalizedEmail = EmailNormalizer.normalize(request.email());
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        log.info("User login succeeded userId={}", user.getId());
        return user;
    }

    public UserResponse toUserResponse(User user) {
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        UserPrincipal principal = currentUser.getPrincipal()
                .orElseThrow(AuthenticationRequiredException::new);
        return toUserResponse(principal);
    }

    public UserResponse toUserResponse(UserPrincipal principal) {
        return new UserResponse(principal.getId(), principal.getDisplayName(), principal.getEmail());
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getDisplayName(), user.getEmail());
    }
}
