package com.example.asyncpayments.service;

import com.example.asyncpayments.entity.ContaAssincrona;
import com.example.asyncpayments.entity.ContaSincrona;
import com.example.asyncpayments.entity.Transacao;
import com.example.asyncpayments.entity.User;
import com.example.asyncpayments.repository.ContaAssincronaRepository;
import com.example.asyncpayments.repository.ContaSincronaRepository;
import com.example.asyncpayments.repository.TransacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SincronizacaoServiceTest {

    @Mock private ContaAssincronaRepository contaAssincronaRepository;
    @Mock private ContaSincronaRepository contaSincronaRepository;
    @Mock private TransacaoRepository transacaoRepository;

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

    @Test
    void verificarEBloquearContas_contaDentroDoPrazo_deveSincronizar() {
        User user = User.builder().id(1L).build();
        ContaAssincrona contaAssincrona = new ContaAssincrona(50.0, user);
        contaAssincrona.setId(1L);
        contaAssincrona.setUltimaSincronizacao(OffsetDateTime.now(ZoneOffset.UTC).minusHours(50));
        contaAssincrona.setBloqueada(false);

        ContaSincrona contaSincrona = new ContaSincrona(100.0, user);

        when(contaAssincronaRepository.findAll()).thenReturn(List.of(contaAssincrona));
        when(contaAssincronaRepository.findById(1L)).thenReturn(Optional.of(contaAssincrona));
        when(contaSincronaRepository.findByUserId(1L)).thenReturn(contaSincrona);

        sincronizacaoService.verificarEBloquearContas();

        assertFalse(contaAssincrona.isBloqueada());
        verify(contaAssincronaRepository, times(1)).save(contaAssincrona);
    }

    @Test
    void verificarEBloquearContas_contaForaDoPrazo_deveBloquear() {
        ContaAssincrona contaAssincrona = new ContaAssincrona(50.0, new User());
        contaAssincrona.setUltimaSincronizacao(OffsetDateTime.now(ZoneOffset.UTC).minusHours(73));
        contaAssincrona.setBloqueada(false);

        when(contaAssincronaRepository.findAll()).thenReturn(List.of(contaAssincrona));

        sincronizacaoService.verificarEBloquearContas();

        assertTrue(contaAssincrona.isBloqueada());
        verify(contaAssincronaRepository).save(contaAssincrona);
    }

    @Test
    void sincronizarContas_contaBloqueada_deveIgnorar() {
        ContaAssincrona contaAssincrona = new ContaAssincrona(50.0, new User());
        contaAssincrona.setBloqueada(true);

        when(contaAssincronaRepository.findAll()).thenReturn(List.of(contaAssincrona));

        sincronizacaoService.sincronizarContas();

        verify(contaAssincronaRepository, never()).save(contaAssincrona);
    }

    @Test
    void rollbackTransacoesNaoSincronizadas_deveReverterTransacoesPendentes() {
        User user = User.builder().id(1L).build();
        ContaAssincrona contaAssincrona = new ContaAssincrona(50.0, user);
        contaAssincrona.setUltimaSincronizacao(OffsetDateTime.now(ZoneOffset.UTC).minusHours(73));
        contaAssincrona.setBloqueada(false);

        Transacao transacao1 = new Transacao();
        transacao1.setId(1L);
        transacao1.setIdUsuarioOrigem(1L);
        transacao1.setSincronizada(false);

        Transacao transacao2 = new Transacao();
        transacao2.setId(2L);
        transacao2.setIdUsuarioOrigem(1L);
        transacao2.setSincronizada(false);

        when(contaAssincronaRepository.findAll()).thenReturn(List.of(contaAssincrona));
        when(transacaoRepository.findBySincronizadaFalse()).thenReturn(List.of(transacao1, transacao2));

        sincronizacaoService.rollbackTransacoesNaoSincronizadas();

        verify(transacaoRepository, times(2)).save(any(Transacao.class));
        verify(contaAssincronaRepository).save(contaAssincrona);
        assertTrue(contaAssincrona.isBloqueada());
    }

    @Test
    void reprocessarTransacoesPendentes_deveSincronizarTransacoesDentroDoPrazo() {
        Transacao transacao = new Transacao();
        transacao.setId(1L);
        transacao.setSincronizada(false);
        transacao.setDataCriacao(OffsetDateTime.now().minusHours(50));

        when(transacaoRepository.findBySincronizadaFalse()).thenReturn(List.of(transacao));

        sincronizacaoService.reprocessarTransacoesPendentes();

        assertTrue(transacao.isSincronizada());
        verify(transacaoRepository).save(transacao);
    }

    @Test
    void reprocessarTransacoesPendentes_transacaoForaDoPrazo_deveNegar() {
        Transacao transacao = new Transacao();
        transacao.setId(1L);
        transacao.setSincronizada(false);
        transacao.setDataCriacao(OffsetDateTime.now().minusHours(80));

        when(transacaoRepository.findBySincronizadaFalse()).thenReturn(List.of(transacao));

        sincronizacaoService.reprocessarTransacoesPendentes();

        assertFalse(transacao.isSincronizada());
        assertEquals("Rollback: Transação não sincronizada em 72h.", transacao.getDescricao());
        verify(transacaoRepository).save(transacao);
    }
}