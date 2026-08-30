package com.workouttracker.auth.api;

import com.workouttracker.auth.application.AuthService;
import com.workouttracker.common.openapi.AuthenticatedOperation;
import com.workouttracker.common.security.SecuritySessionSupport;
import com.workouttracker.auth.domain.User;
import com.workouttracker.common.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Authentication", description = "Registration, login, session profile, and logout")
public class AuthController {

    private final AuthService authService;
    private final SecuritySessionSupport securitySessionSupport;

    public AuthController(AuthService authService, SecuritySessionSupport securitySessionSupport) {
        this.authService = authService;
        this.securitySessionSupport = securitySessionSupport;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @Operation(
            summary = "Register a new user",
            description = "Creates a new account. Does not establish a session; login separately after registration."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email already registered (`EMAIL_ALREADY_REGISTERED`)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(
            summary = "Login",
            description = "Authenticates credentials and establishes a server-side session cookie (`JSESSIONID`)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login succeeded and session established"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid email or password",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
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
    @AuthenticatedOperation
    @Operation(summary = "Get current user profile")
    @ApiResponse(responseCode = "200", description = "Authenticated user profile")
    public UserResponse me() {
        return authService.getCurrentUserProfile();
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @AuthenticatedOperation
    @Operation(
            summary = "Logout",
            description = "Invalidates the current session. Requires CSRF header on this unsafe request."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Session invalidated"),
            @ApiResponse(
                    responseCode = "403",
                    description = "CSRF token missing/invalid",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        securitySessionSupport.invalidateSession(httpRequest, httpResponse);
    }
}
