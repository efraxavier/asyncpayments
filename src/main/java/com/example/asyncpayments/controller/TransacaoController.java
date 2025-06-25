package com.example.asyncpayments.controller;

import com.example.asyncpayments.dto.TransacaoRequest;
import com.example.asyncpayments.dto.TransactionResponse;
import com.example.asyncpayments.entity.GatewayPagamento;
import com.example.asyncpayments.entity.MetodoConexao;
import com.example.asyncpayments.entity.StatusTransacao;
import com.example.asyncpayments.entity.TipoOperacao;
import com.example.asyncpayments.entity.Transacao;
import com.example.asyncpayments.repository.TransacaoRepository;
import com.example.asyncpayments.repository.UserRepository;
import com.example.asyncpayments.service.TransacaoService;
import com.example.asyncpayments.service.FilaTransacaoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/transacoes")
@RequiredArgsConstructor
public class TransacaoController {

    private static final Logger logger = LoggerFactory.getLogger(TransacaoController.class);

    private final TransacaoService transacaoService;
    private final UserRepository userRepository;       
    private final FilaTransacaoService filaTransacaoService;
    private final TransacaoRepository transacaoRepository;


    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<List<TransactionResponse>> buscarTransacoesFiltradas(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) Long idUsuarioOrigem,
            @RequestParam(required = false) Long idUsuarioDestino,
            @RequestParam(required = false) Double valor,
            @RequestParam(required = false) TipoOperacao tipoOperacao,
            @RequestParam(required = false) MetodoConexao metodoConexao,
            @RequestParam(required = false) GatewayPagamento gatewayPagamento,
            @RequestParam(required = false) StatusTransacao status,
            @RequestParam(required = false) String descricao,
            @RequestParam(required = false) String nomeUsuarioOrigem,
            @RequestParam(required = false) String emailUsuarioOrigem,
            @RequestParam(required = false) String cpfUsuarioOrigem,
            @RequestParam(required = false) String nomeUsuarioDestino,
            @RequestParam(required = false) String emailUsuarioDestino,
            @RequestParam(required = false) String cpfUsuarioDestino,
            @RequestParam(required = false) String dataCriacaoInicio,
            @RequestParam(required = false) String dataCriacaoFim,
            @RequestParam(required = false) String dataAtualizacaoInicio,
            @RequestParam(required = false) String dataAtualizacaoFim
    ) {
        List<Transacao> transacoes = transacaoService.buscarTransacoesFiltradas(
            id, idUsuarioOrigem, idUsuarioDestino, valor, tipoOperacao, metodoConexao, gatewayPagamento, status,
            descricao, nomeUsuarioOrigem, emailUsuarioOrigem, cpfUsuarioOrigem,
            nomeUsuarioDestino, emailUsuarioDestino, cpfUsuarioDestino,
            dataCriacaoInicio, dataCriacaoFim, dataAtualizacaoInicio, dataAtualizacaoFim
        );
        List<TransactionResponse> responseList = transacoes.stream()
                .map(transacaoService::mapToTransactionResponse)
                .toList();
        return ResponseEntity.ok(responseList);
    }


    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TransactionResponse> buscarTransacaoPorId(@PathVariable Long id) {
        Optional<Transacao> transacaoOpt = transacaoRepository.findById(id);

        if (transacaoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        Transacao transacao = transacaoOpt.get();
        StatusTransacao status = filaTransacaoService.consultarStatus(id);

        
        TransactionResponse response = new TransactionResponse();
        response.setId(transacao.getId());
        response.setStatus(status.name());
        response.setNomeUsuarioOrigem("Nome do Usuário Origem"); 
        response.setEmailUsuarioOrigem("Email do Usuário Origem"); 
        response.setCpfUsuarioOrigem("CPF do Usuário Origem"); 
        response.setDataCriacao(transacao.getDataCriacao());
        response.setDataAtualizacao(transacao.getDataAtualizacao());

        return ResponseEntity.ok(response);
    }


    @PostMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<?> criarTransacao(@Valid @RequestBody TransacaoRequest request) {
        try {
            Transacao transacao = transacaoService.criarTransacao(
                    request.getIdUsuarioOrigem(),
                    request.getIdUsuarioDestino(),
                    request.getValor(),
                    request.getTipoOperacao(),
                    request.getMetodoConexao(),
                    request.getGatewayPagamento(),
                    request.getDescricao()
            );
            logger.info("[API][RESPONSE] Transação criada com sucesso: id={}, tipoOperacao={}", transacao.getId(), transacao.getTipoOperacao());
            return ResponseEntity.ok(transacao);
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("[API][RESPONSE][4xx] Erro de negócio ao criar transação: tipoOperacao={}, origem={}, destino={}, valor={}, motivo={}",
                request.getTipoOperacao(), request.getIdUsuarioOrigem(), request.getIdUsuarioDestino(), request.getValor(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("[API][RESPONSE][5xx] Erro interno ao criar transação: tipoOperacao={}, origem={}, destino={}, valor={}, motivo={}",
                request.getTipoOperacao(), request.getIdUsuarioOrigem(), request.getIdUsuarioDestino(), request.getValor(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erro interno: " + e.getMessage()));
        }
    }


    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Transacao> atualizarTransacao(@PathVariable Long id, @RequestBody TransacaoRequest request) {

        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> excluirTransacao(@PathVariable Long id) {
        if (transacaoService.existeTransacao(id)) {
            transacaoService.deletarTransacao(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }


    @PostMapping("/sincronizar-offline")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> sincronizarTransacoesOffline() {
        transacaoService.sincronizarTransacoesOffline();
        return ResponseEntity.ok("Transações offline sincronizadas com sucesso.");
    }

    @GetMapping("/recebidas")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<List<Transacao>> listarTransacoesRecebidas(Authentication authentication) {
        String email = authentication.getName();
        var userOpt = userRepository.findByEmail(email); 
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long userId = userOpt.get().getId();
        List<Transacao> recebidas = transacaoService.listarTodasTransacoes()
            .stream()
            .filter(t -> t.getIdUsuarioDestino().equals(userId))
            .toList();
        return ResponseEntity.ok(recebidas);
    }

    @GetMapping("/enviadas")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<List<Transacao>> listarTransacoesEnviadas(Authentication authentication) {
        String email = authentication.getName();
        List<Transacao> transacoes = transacaoService.listarTransacoesEnviadas(email);
        return ResponseEntity.ok(transacoes);
    }

    @PostMapping("/adicionar-fundos")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<?> adicionarFundos(Authentication authentication, @RequestBody TransacaoRequest request) {
        String email = authentication.getName();
        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long userId = userOpt.get().getId();
        try {
            Transacao transacao = transacaoService.criarTransacao(
                userId, 
                userId, 
                request.getValor(),
                TipoOperacao.INTERNA,
                MetodoConexao.ASYNC,
                GatewayPagamento.INTERNO,
                "Adição de fundos da conta síncrona para assincrona"
            );
            return ResponseEntity.ok(transacao);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<String> consultarStatus(@PathVariable Long id) {
        Optional<Transacao> transacao = transacaoRepository.findById(id);

        if (transacao.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Transação não encontrada.");
        }

        StatusTransacao status = filaTransacaoService.consultarStatus(id);
        return ResponseEntity.ok("Status da transação " + id + ": " + status.name());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> atualizarStatusTransacao(@PathVariable Long id, @RequestParam StatusTransacao novoStatus) {
        transacaoService.atualizarStatusTransacao(id, novoStatus);
        return ResponseEntity.noContent().build();
    }
}