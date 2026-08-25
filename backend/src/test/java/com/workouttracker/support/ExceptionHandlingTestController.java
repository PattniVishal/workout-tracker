package com.workouttracker.support;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class ExceptionHandlingTestController {

    record ValidationRequest(@NotBlank String email) {
    }

    @PostMapping("/validation")
    void validate(@Valid @RequestBody ValidationRequest request) {
    }

    @GetMapping("/unexpected")
    void unexpected() {
        throw new RuntimeException("test failure");
    }

    @GetMapping("/authentication-required")
    void authenticationRequired() {
        throw new com.workouttracker.common.security.AuthenticationRequiredException();
    }
}
