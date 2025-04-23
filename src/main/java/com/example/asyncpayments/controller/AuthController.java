package com.example.asyncpayments.controller;

import com.example.asyncpayments.dto.AuthRequest;
import com.example.asyncpayments.dto.AuthResponse;
import com.example.asyncpayments.dto.RegisterRequest;
import com.example.asyncpayments.entity.User;
import com.example.asyncpayments.repository.UserRepository;
import com.example.asyncpayments.service.JwtService;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtService jwtService;

    @Autowired
    private UserRepository repository;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        if (this.repository.findByEmail(request.email()).isPresent()) {
            return ResponseEntity.badRequest().body(new AuthResponse("Email already exists"));
        }

        String encryptedPassword = new BCryptPasswordEncoder().encode(request.password());
        User newUser = new User(request.email(), encryptedPassword, request.role());
        this.repository.save(newUser);
        return ResponseEntity.ok(new AuthResponse("User registered successfully"));
    }

    @Autowired
    private AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(request.email(), request.password());
        var auth = this.authenticationManager.authenticate(usernamePassword);

        // Gere o token JWT usando o UserDetails
        User user = repository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("User not found"));
        String token = jwtService.generateToken(user);

        return ResponseEntity.ok(new AuthResponse(token));
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Authenticated successfully!");
    }
}