package com.example.asyncpayments.service;

import com.example.asyncpayments.dto.AuthRequest;
import com.example.asyncpayments.dto.AuthResponse;
import com.example.asyncpayments.dto.RegisterRequest;
import com.example.asyncpayments.entity.User;
import com.example.asyncpayments.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Handles user registration and returns a JWT token.
     */
    public AuthResponse register(RegisterRequest request) {
        // Create a new user
        var user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .build();

        // Save the user in the database
        userRepository.save(user);

        // Generate a JWT token with the user's ID
        var jwtToken = jwtService.generateToken(user, user.getId());
        return new AuthResponse(jwtToken);
    }

    /**
     * Handles user login and returns a JWT token.
     */
    public AuthResponse login(AuthRequest request) {
        // Authenticate the user
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        // Find the user in the database
        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Generate a JWT token with the user's ID
        var jwtToken = jwtService.generateToken(user, user.getId());
        return new AuthResponse(jwtToken);
    }
}