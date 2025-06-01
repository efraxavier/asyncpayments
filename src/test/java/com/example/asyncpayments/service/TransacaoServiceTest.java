package com.example.asyncpayments.service;

import com.example.asyncpayments.entity.*;
import com.example.asyncpayments.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransacaoServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TransacaoRepository transacaoRepository;
    @Mock private ContaAssincronaRepository contaAssincronaRepository;
    @Mock private ContaSincronaRepository contaSincronaRepository;
    @Mock private BlockchainRegistroRepository blockchainRegistroRepository;

    @InjectMocks
    private TransacaoService transacaoService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
void realizarTransacao_deveSalvarTransacao() {
    Transacao transacao = new Transacao();
    transacao.setId(1L);
    when(transacaoRepository.save(any(Transacao.class))).thenReturn(transacao);

    ContaSincrona contaOrigem = new ContaSincrona();
    contaOrigem.setSaldo(1000.0);
    ContaSincrona contaDestino = new ContaSincrona();
    contaDestino.setSaldo(500.0);

    when(contaSincronaRepository.findByUserId(1L)).thenReturn(contaOrigem);
    when(contaSincronaRepository.findByUserId(2L)).thenReturn(contaDestino);

    Transacao salvo = transacaoService.realizarTransacao(
            1L, 2L, 100.0, GatewayPagamento.PAGARME, MetodoConexao.INTERNET);

    assertNotNull(salvo);
    verify(transacaoRepository, atLeastOnce()).save(any(Transacao.class));
}

    @Test
    void processarTransacaoOffline_transacaoApos72h_deveNegar() {
        Transacao transacao = new Transacao();
        transacao.setId(1L);
        transacao.setSincronizada(false);
        transacao.setDataCriacao(OffsetDateTime.now().minusHours(80));
        when(transacaoRepository.findById(1L)).thenReturn(Optional.of(transacao));

        Exception ex = assertThrows(IllegalStateException.class, () ->
                transacaoService.processarTransacaoOffline(1L, OffsetDateTime.now())
        );
        assertTrue(ex.getMessage().contains("após 72h"));
    }

    @Test
void realizarTransacao_contaOrigemNaoEncontrada_deveLancarExcecao() {
    when(contaSincronaRepository.findByUserId(1L)).thenReturn(null);
    when(contaSincronaRepository.findByUserId(2L)).thenReturn(new ContaSincrona());

    Exception ex = assertThrows(IllegalArgumentException.class, () ->
        transacaoService.realizarTransacao(1L, 2L, 100.0, GatewayPagamento.PAGARME, MetodoConexao.INTERNET)
    );
    assertTrue(ex.getMessage().toLowerCase().contains("conta de origem ou destino não encontrada"));
}
@Test
void listarTodasTransacoes_deveChamarFindAll() {
    when(transacaoRepository.findAll()).thenReturn(List.of(new Transacao()));
    assertFalse(transacaoService.listarTodasTransacoes().isEmpty());
    verify(transacaoRepository).findAll();
}
@Test
void realizarTransacao_valorInvalido_deveLancarExcecao() {
    ContaSincrona contaOrigem = new ContaSincrona();
    contaOrigem.setSaldo(1000.0);
    ContaSincrona contaDestino = new ContaSincrona();
    contaDestino.setSaldo(500.0);

    when(contaSincronaRepository.findByUserId(1L)).thenReturn(contaOrigem);
    when(contaSincronaRepository.findByUserId(2L)).thenReturn(contaDestino);

    Exception ex = assertThrows(IllegalArgumentException.class, () ->
        transacaoService.realizarTransacao(1L, 2L, 0.0, GatewayPagamento.PAGARME, MetodoConexao.INTERNET)
    );
assertEquals("O valor da transação deve ser maior que zero.", ex.getMessage());}

@Test
void realizarTransacao_saldoInsuficiente_deveLancarExcecao() {
    ContaSincrona contaOrigem = new ContaSincrona();
    contaOrigem.setSaldo(50.0);
    ContaSincrona contaDestino = new ContaSincrona();
    contaDestino.setSaldo(500.0);

    when(contaSincronaRepository.findByUserId(1L)).thenReturn(contaOrigem);
    when(contaSincronaRepository.findByUserId(2L)).thenReturn(contaDestino);

    Exception ex = assertThrows(IllegalArgumentException.class, () ->
        transacaoService.realizarTransacao(1L, 2L, 100.0, GatewayPagamento.PAGARME, MetodoConexao.INTERNET)
    );
    assertTrue(ex.getMessage().toLowerCase().contains("saldo insuficiente"));
}

@Test
void processarTransacaoOffline_transacaoDentroDoPrazo_deveSincronizar() {
    Transacao transacao = new Transacao();
    transacao.setId(1L);
    transacao.setIdUsuarioOrigem(1L);
    transacao.setIdUsuarioDestino(2L);
    transacao.setValor(100.0);
    transacao.setSincronizada(false);
    transacao.setDataCriacao(OffsetDateTime.now().minusHours(10));
    transacao.setTipoTransacao(TipoTransacao.SINCRONA);

    ContaSincrona contaOrigem = new ContaSincrona();
    contaOrigem.setSaldo(1000.0);
    ContaSincrona contaDestino = new ContaSincrona();
    contaDestino.setSaldo(500.0);

    when(transacaoRepository.findById(1L)).thenReturn(Optional.of(transacao));
    when(transacaoRepository.save(any(Transacao.class))).thenReturn(transacao);
    when(contaSincronaRepository.findByUserId(1L)).thenReturn(contaOrigem);
    when(contaSincronaRepository.findByUserId(2L)).thenReturn(contaDestino);


    ContaAssincrona contaAssincronaOrigem = new ContaAssincrona();
    contaAssincronaOrigem.setSaldo(1000.0);
    ContaAssincrona contaAssincronaDestino = new ContaAssincrona();
    contaAssincronaDestino.setSaldo(500.0); 
    when(contaAssincronaRepository.findByUserId(1L)).thenReturn(contaAssincronaOrigem);
    when(contaAssincronaRepository.findByUserId(2L)).thenReturn(contaAssincronaDestino);
    assertDoesNotThrow(() -> transacaoService.processarTransacaoOffline(1L, OffsetDateTime.now()));
    verify(transacaoRepository).save(any(Transacao.class));
}

@Test
void buscarTransacaoPorId_deveRetornarOptional() {
    Transacao t = new Transacao();
    when(transacaoRepository.findById(1L)).thenReturn(Optional.of(t));
    assertTrue(transacaoService.buscarTransacaoPorId(1L).isPresent());
}

@Test
void existeTransacao_deveRetornarTrue() {
    when(transacaoRepository.existsById(1L)).thenReturn(true);
    assertTrue(transacaoService.existeTransacao(1L));
}

@Test
void deletarTransacao_deveChamarDeleteById() {
    transacaoService.deletarTransacao(1L);
    verify(transacaoRepository).deleteById(1L);
}
@Test
void realizarTransacao_contaDestinoNaoEncontrada_deveLancarExcecao() {
    when(contaSincronaRepository.findByUserId(1L)).thenReturn(new ContaSincrona());
    when(contaSincronaRepository.findByUserId(2L)).thenReturn(null);

    Exception ex = assertThrows(IllegalArgumentException.class, () ->
        transacaoService.realizarTransacao(1L, 2L, 100.0, GatewayPagamento.PAGARME, MetodoConexao.INTERNET)
    );
    assertTrue(ex.getMessage().toLowerCase().contains("conta de origem ou destino não encontrada"));
}
}