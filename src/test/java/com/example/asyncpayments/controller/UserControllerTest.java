package com.example.asyncpayments.controller;

import com.example.asyncpayments.dto.ApiResponse;
import com.example.asyncpayments.dto.UserDTO;
import com.example.asyncpayments.entity.User;
import com.example.asyncpayments.repository.UserRepository;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserControllerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserController userController;

    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getMe_deveRetornarUsuarioAutenticado() {
        User user = new User();
        user.setEmail("me@email.com");
        when(authentication.getName()).thenReturn("me@email.com");
        when(userRepository.findByEmail("me@email.com")).thenReturn(Optional.of(user));

        ResponseEntity<UserDTO> response = userController.getMe(authentication);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("me@email.com", response.getBody().getEmail());
    }


    @Test
void getMe_usuarioNaoEncontrado_deveRetornar404() {
    when(authentication.getName()).thenReturn("notfound@email.com");
    when(userRepository.findByEmail("notfound@email.com")).thenReturn(Optional.empty());

    ResponseEntity<UserDTO> response = userController.getMe(authentication);

    assertEquals(404, response.getStatusCode().value());
    assertNull(response.getBody());
}

@Test
void listarUsuarios_deveRetornarLista() {
    User user = new User();
    user.setEmail("a@b.com");
    when(userRepository.findAll()).thenReturn(List.of(user));
    ResponseEntity<List<UserDTO>> response = userController.listarUsuarios();
    assertEquals(200, response.getStatusCode().value());
    assertFalse(response.getBody().isEmpty());
}

@Test
void buscarUsuarioPorId_usuarioNaoEncontrado_deveRetornar404() {
    when(userRepository.findById(1L)).thenReturn(Optional.empty());
    ResponseEntity<UserDTO> response = userController.buscarUsuarioPorId(1L);
    assertEquals(404, response.getStatusCode().value());
}
}