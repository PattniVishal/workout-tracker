package com.workouttracker.support;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class SecurityTestController {

    @GetMapping("/protected")
    void protectedEndpoint() {
    }

    @PostMapping("/protected")
    void protectedMutation() {
    }
}
