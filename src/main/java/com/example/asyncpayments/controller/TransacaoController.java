package com.example.asyncpayments.controller;

import com.example.asyncpayments.dto.TransacaoRequest;
import com.example.asyncpayments.entity.Transacao;
import com.example.asyncpayments.service.TransacaoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transacoes")
@RequiredArgsConstructor
public class TransacaoController {

    private final TransacaoService transacaoService;

    @PostMapping
    public ResponseEntity<Transacao> criarTransacao(@RequestBody Transacao transacao, Authentication authentication) {
        return ResponseEntity.ok(transacaoService.criarTransacao(transacao, authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<?> listarTransacoes(Authentication authentication) {
        return ResponseEntity.ok(transacaoService.listarTransacoes(authentication.getName()));
    }

    @PostMapping("/sincrona")
public ResponseEntity<?> realizarTransacaoSincrona(@Valid @RequestBody TransacaoRequest request) {
    try {
        Transacao transacao = transacaoService.realizarTransacaoSincrona(
            request.getIdUsuarioOrigem(),
            request.getIdUsuarioDestino(),
            request.getValor()
        );
        return ResponseEntity.ok(transacao);
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno: " + e.getMessage());
    }
}
}