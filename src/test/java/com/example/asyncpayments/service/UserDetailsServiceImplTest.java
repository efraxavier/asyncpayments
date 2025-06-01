package com.example.asyncpayments.service;

import com.example.asyncpayments.entity.User;
import com.example.asyncpayments.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserDetailsServiceImplTest {

    @Mock private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void loadUserByUsername_usuarioExiste() {
        User user = User.builder().email("a@b.com").build();
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        assertEquals(user, userDetailsService.loadUserByUsername("a@b.com"));
    }

    @Test
    void loadUserByUsername_usuarioNaoExiste() {
        when(userRepository.findByEmail("notfound@email.com")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("notfound@email.com"));
    }
}