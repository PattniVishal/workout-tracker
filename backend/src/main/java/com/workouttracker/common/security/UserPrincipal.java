package com.workouttracker.common.security;

import com.workouttracker.auth.domain.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

/**
 * Authenticated user principal for Spring Security.
 * Password hash is retained for {@link org.springframework.security.core.userdetails.UserDetails}
 * compatibility and must never be exposed outside the security layer.
 */
public class UserPrincipal implements org.springframework.security.core.userdetails.UserDetails {

    private static final String ROLE_USER = "ROLE_USER";

    private final UUID id;
    private final String displayName;
    private final String email;
    private final String passwordHash;

    public UserPrincipal(UUID id, String displayName, String email, String passwordHash) {
        this.id = id;
        this.displayName = displayName;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public static UserPrincipal from(User user) {
        return new UserPrincipal(user.getId(), user.getDisplayName(), user.getEmail(), user.getPasswordHash());
    }

    public UUID getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public List<SimpleGrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(ROLE_USER));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
