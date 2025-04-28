package com.example.asyncpayments.controller;

import com.example.asyncpayments.dto.AuthRequest;
import com.example.asyncpayments.dto.AuthResponse;
import com.example.asyncpayments.dto.RegisterRequest;
import com.example.asyncpayments.entity.User;
import com.example.asyncpayments.repository.UserRepository;
import com.example.asyncpayments.service.JwtService;
import com.example.asyncpayments.service.UserService;

import lombok.RequiredArgsConstructor;

import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
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
    private final UserService userService;

    @Autowired
    private UserRepository repository;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        if (this.repository.findByEmail(request.email()).isPresent()) {
            return ResponseEntity.badRequest().body(new AuthResponse("Email already exists"));
        }

        String encryptedPassword = new BCryptPasswordEncoder().encode(request.password());
        User newUser = userService.criarUsuario(request.email(), encryptedPassword);
        this.repository.save(newUser);
        return ResponseEntity.ok(new AuthResponse("User registered successfully"));
    }

    @GetMapping("/me/id")
    public ResponseEntity<Long> getAuthenticatedUserId(org.springframework.security.core.Authentication authentication) {
    String email = authentication.getName(); // Corrigido para usar getName()
    User user = repository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
    return ResponseEntity.ok(user.getId());
    }

    // Retorna o ID de outro usuário pelo e-mail
    @GetMapping("/user/id")
    public ResponseEntity<Long> getUserIdByEmail(@RequestParam String email) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        return ResponseEntity.ok(user.getId());
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
        String token = jwtService.generateToken(user, user.getId());

        return ResponseEntity.ok(new AuthResponse(token));
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Authenticated successfully!");
    }
}