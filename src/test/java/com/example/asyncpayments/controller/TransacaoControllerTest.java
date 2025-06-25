package com.example.asyncpayments.controller;

import com.example.asyncpayments.dto.TransacaoRequest;
import com.example.asyncpayments.dto.TransactionResponse;
import com.example.asyncpayments.entity.GatewayPagamento;
import com.example.asyncpayments.entity.MetodoConexao;
import com.example.asyncpayments.entity.StatusTransacao;
import com.example.asyncpayments.entity.TipoOperacao;
import com.example.asyncpayments.entity.Transacao;
import com.example.asyncpayments.entity.User;
import com.example.asyncpayments.repository.TransacaoRepository;
import com.example.asyncpayments.repository.UserRepository;
import com.example.asyncpayments.service.FilaTransacaoService;
import com.example.asyncpayments.service.TransacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransacaoController.class)
class TransacaoControllerTest {

    @Autowired
    private TransacaoController transacaoController;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransacaoService transacaoService;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private FilaTransacaoService filaTransacaoService;
    @MockBean
    private TransacaoRepository transacaoRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void listarTodasTransacoes_deveRetornarListaVazia() {
        when(transacaoService.buscarTransacoesFiltradas(
            any(), any(), any(), any(), any(), any(), any(), any(),
            any(), any(), any(), any(), any(), any(), any(),
            any(), any(), any(), any()
        )).thenReturn(Collections.emptyList());

        ResponseEntity<List<TransactionResponse>> response = transacaoController.buscarTransacoesFiltradas(
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null,
            null, null, null, null
        );

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
        when(transacaoService.buscarTransacoesFiltradas(
            any(), any(), any(), any(), any(), any(), any(), any(),
            any(), any(), any(), any(), any(), any(), any(),
            any(), any(), any(), any()
        )).thenReturn(List.of(transacao));
        when(transacaoService.mapToTransactionResponse(any(Transacao.class))).thenReturn(new TransactionResponse());

        ResponseEntity<List<TransactionResponse>> response = transacaoController.buscarTransacoesFiltradas(
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null,
            null, null, null, null
        );

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void deveListarTransacoesComFiltros() throws Exception {
        when(transacaoService.buscarTransacoesFiltradas(
                any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any()
        )).thenReturn(List.of(new Transacao()));

        mockMvc.perform(get("/transacoes")
                .param("status", "SINCRONIZADA")
                .param("idUsuarioOrigem", "1")
                .header("Authorization", "Bearer token"))
            .andExpect(status().isOk());
    }

    @Test
    void deveCriarTransacao() throws Exception {
        Transacao transacao = new Transacao();
        when(transacaoService.criarTransacao(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(transacao);

        String payload = """
        {
            "idUsuarioOrigem": 1,
            "idUsuarioDestino": 2,
            "valor": 50.0,
            "metodoConexao": "INTERNET",
            "gatewayPagamento": "STRIPE",
            "tipoOperacao": "SINCRONA",
            "descricao": "Pagamento"
        }
        """;

        mockMvc.perform(post("/transacoes")
                .contentType("application/json")
                .content(payload)
                .header("Authorization", "Bearer token"))
            .andExpect(status().isOk());
    }

    @Test
    void deveAdicionarFundos() throws Exception {
        Transacao transacao = new Transacao();
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(new User()));
        when(transacaoService.criarTransacao(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(transacao);

        String payload = """
        { "valor": 10.0 }
        """;

        mockMvc.perform(post("/transacoes/adicionar-fundos")
                .contentType("application/json")
                .content(payload)
                .header("Authorization", "Bearer token"))
            .andExpect(status().isOk());
    }

    @Test
    void deveRetornarStatusTransacao() throws Exception {
        when(transacaoRepository.findById(1L)).thenReturn(Optional.of(new Transacao()));
        when(filaTransacaoService.consultarStatus(1L)).thenReturn(StatusTransacao.SINCRONIZADA);

        mockMvc.perform(get("/transacoes/1/status"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("SINCRONIZADA")));
    }

    // Adicione outros testes para recebidas, enviadas, atualizar status, deletar, etc.
}