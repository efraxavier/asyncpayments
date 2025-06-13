package com.example.asyncpayments.controller;

import com.example.asyncpayments.dto.TransacaoRequest;
import com.example.asyncpayments.dto.TransactionResponse;
import com.example.asyncpayments.entity.GatewayPagamento;
import com.example.asyncpayments.entity.MetodoConexao;
import com.example.asyncpayments.entity.StatusTransacao;
import com.example.asyncpayments.entity.TipoOperacao;
import com.example.asyncpayments.entity.Transacao;
import com.example.asyncpayments.repository.TransacaoRepository;
import com.example.asyncpayments.service.FilaTransacaoService;
import com.example.asyncpayments.service.TransacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransacaoControllerTest {

    @Mock
    private TransacaoService transacaoService;

    @Mock
    private TransacaoRepository transacaoRepository;

    @Mock
    private FilaTransacaoService filaTransacaoService; 
    
    @InjectMocks
    private TransacaoController transacaoController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void listarTodasTransacoes_deveRetornarListaVazia() {
        
        when(transacaoService.listarTodasTransacoes()).thenReturn(Collections.emptyList());

        
        ResponseEntity<List<TransactionResponse>> response = transacaoController.listarTodasTransacoes();

        
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void buscarTransacaoPorId_deveRetornarTransacaoResponse() {
        
        Transacao transacao = new Transacao();
        transacao.setId(1L);
        transacao.setStatus(StatusTransacao.SINCRONIZADA);
        when(transacaoRepository.findById(1L)).thenReturn(Optional.of(transacao));
        when(filaTransacaoService.consultarStatus(1L)).thenReturn(StatusTransacao.SINCRONIZADA);

        
        ResponseEntity<TransactionResponse> response = transacaoController.buscarTransacaoPorId(1L);

        
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(StatusTransacao.SINCRONIZADA.name(), response.getBody().getStatus());
    }

    @Test
    void buscarTransacaoPorId_deveRetornar404QuandoNaoEncontrada() {
        
        when(transacaoService.buscarTransacaoPorId(1L)).thenReturn(Optional.empty());

        
        ResponseEntity<TransactionResponse> response = transacaoController.buscarTransacaoPorId(1L);

        
        assertEquals(404, response.getStatusCode().value());
        assertNull(response.getBody());
    }

    @Test
    void criarTransacao_deveRetornarTransacaoCriada() {
        
        TransacaoRequest request = new TransacaoRequest();
        request.setIdUsuarioOrigem(1L);
        request.setIdUsuarioDestino(2L);
        request.setValor(100.0);
        request.setTipoOperacao(TipoOperacao.SINCRONA);
        request.setMetodoConexao(MetodoConexao.INTERNET);
        request.setGatewayPagamento(GatewayPagamento.PAGARME);
        request.setDescricao("Teste de transação");

        Transacao transacao = new Transacao();
        transacao.setId(1L);
        transacao.setStatus(StatusTransacao.PENDENTE);
        when(transacaoService.criarTransacao(any(), any(), any(), any(), any(), any(), any())).thenReturn(transacao);

        
        ResponseEntity<?> response = transacaoController.criarTransacao(request);

        
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof Transacao);
        assertEquals(transacao, response.getBody());
    }

    @Test
    void listarTodasTransacoes_deveRetornarListaDeTransacoes() {
        
        Transacao transacao = new Transacao();
        transacao.setId(1L);
        transacao.setStatus(StatusTransacao.SINCRONIZADA);
        when(transacaoService.listarTodasTransacoes()).thenReturn(List.of(transacao));
        when(transacaoService.mapToTransactionResponse(any(Transacao.class))).thenReturn(new TransactionResponse());

        
        ResponseEntity<List<TransactionResponse>> response = transacaoController.listarTodasTransacoes();

        
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }
}