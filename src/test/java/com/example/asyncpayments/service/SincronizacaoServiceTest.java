package com.example.asyncpayments.service;

import com.example.asyncpayments.entity.ContaAssincrona;
import com.example.asyncpayments.entity.ContaSincrona;
import com.example.asyncpayments.entity.GatewayPagamento;
import com.example.asyncpayments.entity.MetodoConexao;
import com.example.asyncpayments.entity.StatusTransacao;
import com.example.asyncpayments.entity.TipoOperacao;
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
    void sincronizarConta_deveSincronizarSaldoEStatus() {
        User user = User.builder().id(1L).build();
        ContaAssincrona ca = new ContaAssincrona(50.0, user);
        ca.setId(1L);
        ContaSincrona cs = new ContaSincrona(100.0, user);

        when(contaAssincronaRepository.findById(1L)).thenReturn(Optional.of(ca));
        when(contaSincronaRepository.findByUserId(1L)).thenReturn(cs);

        sincronizacaoService.sincronizarConta(1L);

        verify(contaSincronaRepository).save(any(ContaSincrona.class));
        verify(contaAssincronaRepository).save(any(ContaAssincrona.class));
        assertEquals(0.0, ca.getSaldo());
        assertEquals(150.0, cs.getSaldo());
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
        Transacao transacao1 = new Transacao(1L, 1L, 2L, 100.0, TipoOperacao.ASSINCRONA, MetodoConexao.INTERNET, GatewayPagamento.PAGARME, StatusTransacao.PENDENTE, OffsetDateTime.now().minusHours(80), OffsetDateTime.now(), "Transação 1");
        Transacao transacao2 = new Transacao(2L, 1L, 2L, 200.0, TipoOperacao.ASSINCRONA, MetodoConexao.INTERNET, GatewayPagamento.PAGARME, StatusTransacao.PENDENTE, OffsetDateTime.now().minusHours(80), OffsetDateTime.now(), "Transação 2");

        when(transacaoRepository.findByStatus(StatusTransacao.PENDENTE)).thenReturn(List.of(transacao1, transacao2));

        sincronizacaoService.rollbackTransacoesNaoSincronizadas();

        assertEquals(StatusTransacao.ROLLBACK, transacao1.getStatus());
        assertEquals(StatusTransacao.ROLLBACK, transacao2.getStatus());
        verify(transacaoRepository, times(2)).save(any(Transacao.class));
    }

    @Test
    void reprocessarTransacoesPendentes_deveSincronizarTransacoesDentroDoPrazo() {
        Transacao transacao = new Transacao();
        transacao.setId(1L);
        transacao.setStatus(StatusTransacao.PENDENTE);
        transacao.setDataCriacao(OffsetDateTime.now().minusHours(50));

        when(transacaoRepository.findByStatus(StatusTransacao.PENDENTE)).thenReturn(List.of(transacao));

        sincronizacaoService.reprocessarTransacoesPendentes();

        assertEquals(StatusTransacao.SINCRONIZADA, transacao.getStatus());
        verify(transacaoRepository).save(transacao);
    }

    @Test
    void reprocessarTransacoesPendentes_transacaoForaDoPrazo_deveNegar() {
        Transacao transacao = new Transacao();
        transacao.setId(1L);
        transacao.setStatus(StatusTransacao.PENDENTE);
        transacao.setDataCriacao(OffsetDateTime.now().minusHours(80));

        when(transacaoRepository.findByStatus(StatusTransacao.PENDENTE)).thenReturn(List.of(transacao));

        sincronizacaoService.reprocessarTransacoesPendentes();

        assertEquals(StatusTransacao.ROLLBACK, transacao.getStatus());
        assertEquals("Rollback: Transação não sincronizada em 72h.", transacao.getDescricao());
        verify(transacaoRepository).save(transacao);
    }

    @Test
    void sincronizarPorId_deveSincronizarSaldoEStatus() {
        User user = User.builder().id(1L).build();
        ContaAssincrona ca = new ContaAssincrona(50.0, user);
        ca.setId(1L);
        ContaSincrona cs = new ContaSincrona(100.0, user);

        when(contaAssincronaRepository.findById(1L)).thenReturn(Optional.of(ca));
        when(contaSincronaRepository.findByUserId(1L)).thenReturn(cs);

        sincronizacaoService.sincronizarPorId(1L);

        verify(contaSincronaRepository).save(any(ContaSincrona.class));
        verify(contaAssincronaRepository).save(any(ContaAssincrona.class));
        assertEquals(0.0, ca.getSaldo());
        assertEquals(150.0, cs.getSaldo());
    }

    @Test
    void sincronizarPorId_contaAssincronaNaoEncontrada_deveLancarExcecao() {
        when(contaAssincronaRepository.findById(99L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
            sincronizacaoService.sincronizarPorId(99L)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("conta assíncrona não encontrada"));
    }

    @Test
    void sincronizarPorId_contaSincronaNaoEncontrada_deveLancarExcecao() {
        User user = User.builder().id(1L).build();
        ContaAssincrona ca = new ContaAssincrona(50.0, user);
        ca.setId(1L);
        when(contaAssincronaRepository.findById(1L)).thenReturn(Optional.of(ca));
        when(contaSincronaRepository.findByUserId(1L)).thenReturn(null);

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
            sincronizacaoService.sincronizarPorId(1L)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("conta síncrona correspondente não encontrada"));
    }

    @Test
    void sincronizar_contaBloqueada_deveIgnorar() {
        ContaAssincrona contaAssincrona = new ContaAssincrona(50.0, new User());
        contaAssincrona.setBloqueada(true);

        when(contaAssincronaRepository.findAll()).thenReturn(List.of(contaAssincrona));

        sincronizacaoService.sincronizar();

        verify(contaAssincronaRepository, never()).save(contaAssincrona);
    }
}