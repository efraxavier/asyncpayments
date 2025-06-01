package com.example.asyncpayments.service;

import com.example.asyncpayments.entity.ContaAssincrona;
import com.example.asyncpayments.entity.ContaSincrona;
import com.example.asyncpayments.entity.User;
import com.example.asyncpayments.repository.ContaAssincronaRepository;
import com.example.asyncpayments.repository.ContaSincronaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SincronizacaoServiceTest {

    @Mock private ContaAssincronaRepository contaAssincronaRepository;
    @Mock private ContaSincronaRepository contaSincronaRepository;

    @InjectMocks
    private SincronizacaoService sincronizacaoService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void sincronizarConta_deveSincronizarSaldoEDesbloquear() {
        User user = User.builder().id(1L).build();
        ContaAssincrona ca = new ContaAssincrona(50.0, user);
        ca.setId(1L);
        ca.setBloqueada(true);
        ContaSincrona cs = new ContaSincrona(100.0, user);

        when(contaAssincronaRepository.findById(1L)).thenReturn(Optional.of(ca));
        when(contaSincronaRepository.findByUserId(1L)).thenReturn(cs);

        sincronizacaoService.sincronizarConta(1L);

        verify(contaSincronaRepository).save(any(ContaSincrona.class));
        verify(contaAssincronaRepository).save(any(ContaAssincrona.class));
        assertFalse(ca.isBloqueada());
    }

    @Test
void sincronizarConta_contaAssincronaNaoEncontrada_deveLancarExcecao() {
    when(contaAssincronaRepository.findById(99L)).thenReturn(Optional.empty());

    Exception ex = assertThrows(IllegalArgumentException.class, () ->
        sincronizacaoService.sincronizarConta(99L)
    );
    assertTrue(ex.getMessage().toLowerCase().contains("conta assíncrona não encontrada"));
}

@Test
void sincronizarConta_contaSincronaNaoEncontrada_deveLancarExcecao() {
    User user = User.builder().id(1L).build();
    ContaAssincrona ca = new ContaAssincrona(50.0, user);
    ca.setId(1L);
    when(contaAssincronaRepository.findById(1L)).thenReturn(Optional.of(ca));
    when(contaSincronaRepository.findByUserId(1L)).thenReturn(null);

    Exception ex = assertThrows(IllegalArgumentException.class, () ->
        sincronizacaoService.sincronizarConta(1L)
    );
    assertTrue(ex.getMessage().toLowerCase().contains("conta síncrona correspondente não encontrada"));
}
}