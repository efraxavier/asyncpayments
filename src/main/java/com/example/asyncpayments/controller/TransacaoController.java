package com.example.asyncpayments.controller;

import com.example.asyncpayments.dto.TransacaoRequest;
import com.example.asyncpayments.entity.Transacao;
import com.example.asyncpayments.repository.UserRepository;
import com.example.asyncpayments.service.TransacaoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transacoes")
@RequiredArgsConstructor
public class TransacaoController {

    private final TransacaoService transacaoService;
    private final UserRepository userRepository; // Adicione esta linha


    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Transacao>> listarTodasTransacoes() {
        return ResponseEntity.ok(transacaoService.listarTodasTransacoes());
    }


    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Transacao> buscarTransacaoPorId(@PathVariable Long id) {
        return transacaoService.buscarTransacaoPorId(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }


    @PostMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<?> criarTransacao(@Valid @RequestBody TransacaoRequest request) {
        try {
            Transacao transacao = transacaoService.realizarTransacao(
                request.getIdUsuarioOrigem(),
                request.getIdUsuarioDestino(),
                request.getValor(),
                request.getGatewayPagamento(),
                request.getMetodoConexao(), // pode ser INTERNET ou ASYNC
                request.getDescricao()
            );
            return ResponseEntity.ok(transacao);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno: " + e.getMessage());
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
    public ResponseEntity<?> sincronizarOffline(@RequestBody List<Long> idsTransacoes) {
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);
        for (Long id : idsTransacoes) {
            try {
                transacaoService.processarTransacaoOffline(id, agora);
            } catch (Exception e) {

            }
        }
        return ResponseEntity.ok("Transações offline processadas.");
    }

    @GetMapping("/recebidas")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<List<Transacao>> listarTransacoesRecebidas(Authentication authentication) {
        String email = authentication.getName();
        var userOpt = userRepository.findByEmail(email); // Use o repositório diretamente
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
}