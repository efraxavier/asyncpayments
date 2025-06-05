package com.example.asyncpayments.controller;

import com.example.asyncpayments.dto.AuthRequest;
import com.example.asyncpayments.dto.AuthResponse;
import com.example.asyncpayments.dto.RegisterRequest;
import com.example.asyncpayments.entity.User;
import com.example.asyncpayments.repository.UserRepository;
import com.example.asyncpayments.service.AuthService;
import com.example.asyncpayments.service.JwtService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtService jwtService;
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository repository;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
    var usernamePassword = new UsernamePasswordAuthenticationToken(request.email(), request.password());
    authenticationManager.authenticate(usernamePassword);

    User user = repository.findByEmail(request.email()).orElse(null);
    if (user == null) {
        return ResponseEntity.notFound().build();
    }
    String token = jwtService.generateToken(user, user.getId());
    return ResponseEntity.ok(new AuthResponse(token));
}

    @GetMapping("/user/id")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Long> getUserIdByEmail(@RequestParam String email) {
        User user = repository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user.getId());
    }

}