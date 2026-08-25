package com.workouttracker.auth.api;

import com.workouttracker.auth.application.AuthService;
import com.workouttracker.common.security.SecuritySessionSupport;
import com.workouttracker.auth.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SecuritySessionSupport securitySessionSupport;

    public AuthController(AuthService authService, SecuritySessionSupport securitySessionSupport) {
        this.authService = authService;
        this.securitySessionSupport = securitySessionSupport;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public UserResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        User user = authService.authenticate(request);
        securitySessionSupport.establishSession(user, httpRequest, httpResponse);
        return authService.toUserResponse(user);
    }

    @GetMapping("/me")
    public UserResponse me() {
        return authService.getCurrentUserProfile();
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        securitySessionSupport.invalidateSession(httpRequest, httpResponse);
    }
}
