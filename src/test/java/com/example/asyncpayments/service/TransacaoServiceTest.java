package com.example.asyncpayments.service;

import com.example.asyncpayments.dto.TransactionResponse;
import com.example.asyncpayments.entity.*;
import com.example.asyncpayments.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TransacaoServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TransacaoRepository transacaoRepository;
    @Mock
    private ContaAssincronaRepository contaAssincronaRepository;
    @Mock
    private ContaSincronaRepository contaSincronaRepository;
    @Mock
    private BlockchainRegistroRepository blockchainRegistroRepository;
    @Mock
    private BlockchainService blockchainService;
    @Mock
    private FilaTransacaoService filaTransacaoService;
    @Mock
    private SincronizacaoService sincronizacaoService;
    @Mock
    private TransactionResponse transactionResponse;
    @Mock
    private TransacaoAltoValorRepository transacaoAltoValorRepository; 

    @InjectMocks
    private TransacaoService transacaoService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
void realizarTransacao_deveSalvarTransacao() {
    Transacao transacao = new Transacao();
    transacao.setId(123L); 
    when(transacaoRepository.save(any(Transacao.class))).thenReturn(transacao);

    ContaSincrona contaOrigem = new ContaSincrona();
    contaOrigem.setSaldo(1000.0);
    ContaSincrona contaDestino = new ContaSincrona();
    contaDestino.setSaldo(500.0);

    when(contaSincronaRepository.findByUserId(1L)).thenReturn(contaOrigem);
    when(contaSincronaRepository.findByUserId(2L)).thenReturn(contaDestino);

    Transacao salvo = transacaoService.realizarTransacao(
            1L, 2L, 100.0, GatewayPagamento.PAGARME, MetodoConexao.INTERNET, null);

    assertNotNull(salvo);
    verify(transacaoRepository, atLeastOnce()).save(any(Transacao.class));
}

    @Test
void processarTransacaoOffline_transacaoApos72h_deveNegar() {
    Transacao transacao = new Transacao();
    transacao.setId(1L);
    transacao.setStatus(StatusTransacao.PENDENTE);
    transacao.setDataCriacao(OffsetDateTime.now().minusHours(80));
    when(transacaoRepository.findById(1L)).thenReturn(Optional.of(transacao));

    Exception ex = assertThrows(IllegalStateException.class, () ->
            transacaoService.processarTransacaoOffline(1L, OffsetDateTime.now())
    );
    assertEquals(StatusTransacao.ROLLBACK, transacao.getStatus());
    assertTrue(ex.getMessage().contains("após 72h"));
}

    @Test
void realizarTransacao_contaOrigemNaoEncontrada_deveLancarExcecao() {
    when(contaSincronaRepository.findByUserId(1L)).thenReturn(null);
    when(contaSincronaRepository.findByUserId(2L)).thenReturn(new ContaSincrona());

    Exception ex = assertThrows(IllegalArgumentException.class, () ->
        transacaoService.realizarTransacao(1L, 2L, 100.0, GatewayPagamento.PAGARME, MetodoConexao.INTERNET, null)
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
        transacaoService.realizarTransacao(1L, 2L, 0.0, GatewayPagamento.PAGARME, MetodoConexao.INTERNET, null)
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
        transacaoService.realizarTransacao(1L, 2L, 100.0, GatewayPagamento.PAGARME, MetodoConexao.INTERNET, null)
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
    transacao.setStatus(StatusTransacao.PENDENTE);
    transacao.setDataCriacao(OffsetDateTime.now().minusHours(10));

    ContaAssincrona contaOrigem = new ContaAssincrona();
    contaOrigem.setSaldo(500.0);
    ContaAssincrona contaDestino = new ContaAssincrona();
    contaDestino.setSaldo(300.0);

    when(transacaoRepository.findById(1L)).thenReturn(Optional.of(transacao));
    when(contaAssincronaRepository.findByUserId(1L)).thenReturn(contaOrigem);
    when(contaAssincronaRepository.findByUserId(2L)).thenReturn(contaDestino);

    assertDoesNotThrow(() -> transacaoService.processarTransacaoOffline(1L, OffsetDateTime.now()));
    assertEquals(StatusTransacao.SINCRONIZADA, transacao.getStatus());
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
        transacaoService.realizarTransacao(1L, 2L, 100.0, GatewayPagamento.PAGARME, MetodoConexao.INTERNET, null)
    );
    assertTrue(ex.getMessage().toLowerCase().contains("conta de origem ou destino não encontrada"));
}
@Test
void realizarTransacaoAssincrona_deveRegistrarTransacaoNoBlockchain() {
    ContaAssincrona contaOrigem = new ContaAssincrona();
    contaOrigem.setSaldo(1000.0);
    ContaAssincrona contaDestino = new ContaAssincrona();
    contaDestino.setSaldo(500.0);

    when(contaAssincronaRepository.findByUserId(1L)).thenReturn(contaOrigem);
    when(contaAssincronaRepository.findByUserId(2L)).thenReturn(contaDestino);

    transacaoService.realizarTransacaoAssincrona(1L, 2L, 100.0);

    verify(blockchainService).registrarTransacao(any(BlockchainRegistro.class));
}

@Test
void processarTransacaoOffline_deveRegistrarTransacaoNoBlockchain() {
    
    Transacao transacao = new Transacao();
    transacao.setId(1L);
    transacao.setIdUsuarioOrigem(1L);
    transacao.setIdUsuarioDestino(2L);
    transacao.setValor(100.0);
    transacao.setStatus(StatusTransacao.PENDENTE); 
    transacao.setDataCriacao(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1));

    ContaAssincrona contaOrigem = new ContaAssincrona();
    contaOrigem.setSaldo(500.0);
    ContaAssincrona contaDestino = new ContaAssincrona();
    contaDestino.setSaldo(300.0);

    
    when(transacaoRepository.findById(1L)).thenReturn(Optional.of(transacao));
    when(contaAssincronaRepository.findByUserId(1L)).thenReturn(contaOrigem);
    when(contaAssincronaRepository.findByUserId(2L)).thenReturn(contaDestino);

    
    transacaoService.processarTransacaoOffline(1L, OffsetDateTime.now(ZoneOffset.UTC));

    
    verify(blockchainService).registrarTransacao(any(BlockchainRegistro.class));
    verify(transacaoRepository).save(any(Transacao.class));
}

@Test
void realizarTransacao_limiteDiarioExcedido_deveNegar() {
    OffsetDateTime startDate = OffsetDateTime.now().minusDays(1);
    OffsetDateTime endDate = OffsetDateTime.now();

    when(transacaoRepository.findByIdUsuarioOrigemAndDataCriacaoBetween(
        anyLong(), any(OffsetDateTime.class), any(OffsetDateTime.class)))
    .thenReturn(List.of(
        new Transacao(1L, 1L, 2L, 800.0, TipoOperacao.SINCRONA, MetodoConexao.INTERNET, GatewayPagamento.PAGARME, StatusTransacao.PENDENTE, OffsetDateTime.now(), OffsetDateTime.now(), "Transação 1"),
        new Transacao(2L, 1L, 2L, 300.0, TipoOperacao.SINCRONA, MetodoConexao.INTERNET, GatewayPagamento.PAGARME, StatusTransacao.PENDENTE, OffsetDateTime.now(), OffsetDateTime.now(), "Transação 2")
    ));

    
    ContaSincrona contaOrigem = new ContaSincrona();
    contaOrigem.setSaldo(2000.0);
    ContaSincrona contaDestino = new ContaSincrona();
    contaDestino.setSaldo(1000.0);

    when(contaSincronaRepository.findByUserId(1L)).thenReturn(contaOrigem);
    when(contaSincronaRepository.findByUserId(2L)).thenReturn(contaDestino);

    Exception ex = assertThrows(IllegalStateException.class, () ->
        transacaoService.realizarTransacao(1L, 2L, 500.0, GatewayPagamento.PAGARME, MetodoConexao.INTERNET, null)
    );
    assertTrue(ex.getMessage().contains("Limite diário excedido"));
}

@Test
void realizarTransacaoOffline_valorAcimaDe500_deveNegar() {
    Exception ex = assertThrows(IllegalArgumentException.class, () ->
        transacaoService.realizarTransacaoAssincrona(1L, 2L, 600.0)
    );
    assertTrue(ex.getMessage().contains("Limite de R$500 por transação offline"));
}
@Test
void realizarTransacao_valorAcimaDe10000_deveNotificarBACEN() {
    ContaSincrona contaOrigem = new ContaSincrona();
    contaOrigem.setSaldo(20000.0);
    ContaSincrona contaDestino = new ContaSincrona();
    contaDestino.setSaldo(5000.0);

    when(contaSincronaRepository.findByUserId(1L)).thenReturn(contaOrigem);
    when(contaSincronaRepository.findByUserId(2L)).thenReturn(contaDestino);

    Transacao transacao = new Transacao();
    transacao.setId(123L); 
    when(transacaoRepository.save(any(Transacao.class))).thenReturn(transacao);

    transacaoService.realizarTransacao(1L, 2L, 15000.0, GatewayPagamento.PAGARME, MetodoConexao.INTERNET, null);

    verify(blockchainService).registrarTransacao(any(BlockchainRegistro.class));
}
}