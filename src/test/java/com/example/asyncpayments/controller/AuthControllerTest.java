package com.example.asyncpayments.controller;

import com.example.asyncpayments.dto.AuthRequest;
import com.example.asyncpayments.dto.AuthResponse;
import com.example.asyncpayments.dto.RegisterRequest;
import com.example.asyncpayments.entity.User;
import com.example.asyncpayments.repository.UserRepository;
import com.example.asyncpayments.service.AuthService;
import com.example.asyncpayments.service.JwtService;
import com.example.asyncpayments.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    @Mock private JwtService jwtService;
    @Mock private UserService userService;
    @Mock private AuthService authService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void register_deveRetornarToken() {
        RegisterRequest req = new RegisterRequest("a@b.com", "123", "12345678900", "Nome", "Sobrenome", "123", "USER", true);
        AuthResponse resp = new AuthResponse("token");
        when(authService.register(req)).thenReturn(resp);

        ResponseEntity<AuthResponse> response = authController.register(req);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("token", response.getBody().token());
    }

    @Test
    void login_deveRetornarToken() {
        AuthRequest req = new AuthRequest("a@b.com", "123");
        User user = new User();
        user.setEmail("a@b.com");
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user, user.getId())).thenReturn("token");

        ResponseEntity<AuthResponse> response = authController.login(req);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("token", response.getBody().token());
    }

    @Test
    void login_usuarioNaoEncontrado_deveRetornar404() {
    AuthRequest req = new AuthRequest("notfound@email.com", "123");
    when(userRepository.findByEmail("notfound@email.com")).thenReturn(Optional.empty());

    ResponseEntity<AuthResponse> response = authController.login(req);

    assertEquals(404, response.getStatusCodeValue());
    assertNull(response.getBody());
}
}