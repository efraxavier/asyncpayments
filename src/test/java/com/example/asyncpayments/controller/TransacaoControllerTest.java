package com.example.asyncpayments.controller;

import com.example.asyncpayments.dto.TransacaoRequest;
import com.example.asyncpayments.entity.Transacao;
import com.example.asyncpayments.service.TransacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransacaoControllerTest {

    @Mock
    private TransacaoService transacaoService;

    @InjectMocks
    private TransacaoController transacaoController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void listarTodasTransacoes_deveRetornarLista() {
        when(transacaoService.listarTodasTransacoes()).thenReturn(Collections.emptyList());
        ResponseEntity<List<Transacao>> response = transacaoController.listarTodasTransacoes();
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
    }

    @Test
    void buscarTransacaoPorId_deveRetornarTransacao() {
        Transacao t = new Transacao();
        when(transacaoService.buscarTransacaoPorId(1L)).thenReturn(Optional.of(t));
        ResponseEntity<Transacao> response = transacaoController.buscarTransacaoPorId(1L);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(t, response.getBody());
    }

    @Test
    void criarTransacao_deveRetornarTransacao() {
        TransacaoRequest req = mock(TransacaoRequest.class);
        Transacao t = new Transacao();
        when(transacaoService.realizarTransacao(any(), any(), any(), any(), any())).thenReturn(t);
        ResponseEntity<?> response = transacaoController.criarTransacao(req);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(t, response.getBody());
    }
}