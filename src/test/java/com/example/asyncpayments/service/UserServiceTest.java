package com.example.asyncpayments.service;

import com.example.asyncpayments.entity.User;
import com.example.asyncpayments.entity.UserRole;
import com.example.asyncpayments.entity.ContaSincrona;
import com.example.asyncpayments.entity.ContaAssincrona;
import com.example.asyncpayments.repository.UserRepository;
import com.example.asyncpayments.repository.ContaSincronaRepository;
import com.example.asyncpayments.repository.ContaAssincronaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ContaSincronaRepository contaSincronaRepository;
    @Mock private ContaAssincronaRepository contaAssincronaRepository;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void criarUsuario_deveSalvarUsuarioComContas() {
        User user = User.builder()
                .email("test@email.com")
                .password("senha")
                .cpf("12345678900")
                .nome("Test")
                .sobrenome("User")
                .celular("11999999999")
                .role(UserRole.USER)
                .consentimentoDados(true)
                .kycValidado(true)
                .build();

        ContaSincrona contaSincrona = new ContaSincrona(100.0, user);
        ContaAssincrona contaAssincrona = new ContaAssincrona(0.0, user);

        when(userRepository.save(any(User.class))).thenReturn(user);
        when(contaSincronaRepository.save(any(ContaSincrona.class))).thenReturn(contaSincrona);
        when(contaAssincronaRepository.save(any(ContaAssincrona.class))).thenReturn(contaAssincrona);

        User salvo = userService.criarUsuario(
                "test@email.com", "senha", "12345678900", "Test", "User", "11999999999", UserRole.USER, true);

        assertNotNull(salvo);
        assertEquals("test@email.com", salvo.getEmail());
        verify(userRepository, atLeastOnce()).save(any(User.class));
        verify(contaSincronaRepository, atLeastOnce()).save(any(ContaSincrona.class));
        verify(contaAssincronaRepository, atLeastOnce()).save(any(ContaAssincrona.class));
    }

    @Test
    void countUsers_deveRetornarQuantidade() {
        when(userRepository.count()).thenReturn(5L);
        assertEquals(5L, userService.countUsers());
    }
}