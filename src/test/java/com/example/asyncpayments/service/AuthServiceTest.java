package com.example.asyncpayments.service;

import com.example.asyncpayments.dto.AuthRequest;
import com.example.asyncpayments.dto.AuthResponse;
import com.example.asyncpayments.dto.RegisterRequest;
import com.example.asyncpayments.entity.User;
import com.example.asyncpayments.entity.UserRole;
import com.example.asyncpayments.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserService userService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void register_deveCadastrarUsuarioENaoPermitirDuplicado() {
        RegisterRequest req = new RegisterRequest("a@b.com", "pw", "123", "Nome", "Sobrenome", "999", "USER", true);

        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.empty());
        when(userRepository.findByCpf("123")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pw")).thenReturn("pwenc");
        User user = User.builder().email("a@b.com").build();
        when(userService.criarUsuario(any(), any(), any(), any(), any(), any(), any(), anyBoolean())).thenReturn(user);
        when(jwtService.generateToken(any(), any())).thenReturn("token");

        AuthResponse resp = authService.register(req);
        assertNotNull(resp);
        assertEquals("token", resp.token());


        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> authService.register(req));
    }

    @Test
    void login_deveAutenticarUsuario() {
        AuthRequest req = new AuthRequest("a@b.com", "pw");
        User user = User.builder().email("a@b.com").build();
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(), any())).thenReturn("token");

        AuthResponse resp = authService.login(req);
        assertNotNull(resp);
        assertEquals("token", resp.token());
    }

    @Test
    void login_usuarioNaoEncontrado_deveLancarExcecao() {
        AuthRequest req = new AuthRequest("notfound@email.com", "pw");
        when(userRepository.findByEmail("notfound@email.com")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> authService.login(req));
    }
}