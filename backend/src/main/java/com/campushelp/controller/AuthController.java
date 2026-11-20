package com.campushelp.controller;

import com.campushelp.dto.AuthResponse;
import com.campushelp.dto.LoginRequest;
import com.campushelp.dto.RegisterRequest;
import com.campushelp.entity.User;
import com.campushelp.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal User user) {
        return Map.of("id", user.getId(), "name", user.getName(),
                "email", user.getEmail(), "role", user.getRole().name());
    }
}
