package com.example.asyncpayments.controller;

import com.example.asyncpayments.dto.TransacaoRequest;
import com.example.asyncpayments.entity.Transacao;
import com.example.asyncpayments.service.TransacaoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transacoes")
@RequiredArgsConstructor
public class TransacaoController {

    private final TransacaoService transacaoService;

    /**
     * Lista todas as transações.
     * 
     * @return Lista de transações.
     */
    @GetMapping("/todas")
    public ResponseEntity<List<Transacao>> listarTodasTransacoes() {
        List<Transacao> transacoes = transacaoService.listarTodasTransacoes();
        return ResponseEntity.ok(transacoes);
    }

    /**
     * Realiza uma transação entre contas.
     * 
     * @param request Objeto contendo os dados da transação.
     * @return Transação realizada ou erro.
     */
    @PostMapping("/realizar")
    public ResponseEntity<?> realizarTransacao(@Valid @RequestBody TransacaoRequest request) {
    try {
        Transacao transacao = transacaoService.realizarTransacao(
            request.getIdUsuarioOrigem(),
            request.getIdUsuarioDestino(),
            request.getValor(),
            request.getGatewayPagamento(),
            request.getMetodoConexao()
        );
        return ResponseEntity.ok(transacao);
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    } catch (IllegalStateException e) {
        if (e.getMessage().contains("bloqueada")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage() + ". Sincronize a conta para desbloqueá-la.");
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno: " + e.getMessage());
    }
}

/**
 * Realiza uma transação da conta síncrona para a conta assíncrona do mesmo usuário.
 * 
 * @param idUsuario ID do usuário.
 * @param request Objeto contendo o valor da transação.
 * @return Transação realizada ou erro.
 */
@PostMapping("/adicionar-assincrona/{idUsuario}")
public ResponseEntity<?> transferirSincronaParaAssincrona(@PathVariable Long idUsuario, @RequestBody @Valid TransacaoRequest request) {
    try {
        Transacao transacao = transacaoService.transferirSincronaParaAssincrona(idUsuario, request.getValor());
        return ResponseEntity.ok(transacao);
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    } catch (IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno: " + e.getMessage());
    }
}
}