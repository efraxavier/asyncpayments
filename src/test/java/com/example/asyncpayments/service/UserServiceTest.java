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
        // Configuração dos dados de entrada
        String email = "user@mail.com";
        String senha = "123456";
        String cpf = "12345678901";
        String nome = "Nome";
        String sobrenome = "Sobrenome";
        String celular = "11999999999";
        UserRole role = UserRole.USER;
        boolean consentimentoDados = true;

        // Mock do usuário
        User usuarioMock = User.builder()
                .email(email)
                .password(senha)
                .cpf(cpf)
                .nome(nome)
                .sobrenome(sobrenome)
                .celular(celular)
                .role(role)
                .kycValidado(true)
                .consentimentoDados(consentimentoDados)
                .build();

        // Mock das contas
        ContaSincrona contaSincronaMock = new ContaSincrona(100.0, usuarioMock);
        ContaAssincrona contaAssincronaMock = new ContaAssincrona(0.0, usuarioMock);

        // Configuração dos mocks
        when(userRepository.save(any(User.class))).thenReturn(usuarioMock);
        when(contaSincronaRepository.save(any(ContaSincrona.class))).thenReturn(contaSincronaMock);
        when(contaAssincronaRepository.save(any(ContaAssincrona.class))).thenReturn(contaAssincronaMock);

        // Execução do método
        User resultado = userService.criarUsuario(email, senha, cpf, nome, sobrenome, celular, role, consentimentoDados);

        // Validações
        assertNotNull(resultado);
        assertNotNull(resultado.getContaSincrona());
        assertNotNull(resultado.getContaAssincrona());
        verify(userRepository, times(2)).save(any(User.class)); // Salvo duas vezes: inicial e após associar contas
        verify(contaSincronaRepository, times(1)).save(any(ContaSincrona.class));
        verify(contaAssincronaRepository, times(1)).save(any(ContaAssincrona.class));
    }

    @Test
    void countUsers_deveRetornarQuantidade() {
        when(userRepository.count()).thenReturn(5L);
        assertEquals(5L, userService.countUsers());
    }
}