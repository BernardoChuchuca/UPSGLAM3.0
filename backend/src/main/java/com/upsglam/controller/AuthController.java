package com.upsglam.controller;

import com.upsglam.dto.request.LoginRequest;
import com.upsglam.dto.request.RegisterRequest;
import com.upsglam.dto.response.AuthResponse;
import com.upsglam.dto.response.ProfileResponse;
import com.upsglam.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ProfileResponse> register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public Mono<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }
}
