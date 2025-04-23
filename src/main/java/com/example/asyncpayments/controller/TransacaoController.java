package com.example.asyncpayments.controller;

import com.example.asyncpayments.entity.Transacao;
import com.example.asyncpayments.service.TransacaoService;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<Transacao> realizarTransacaoSincrona(
            @RequestParam Long idUsuarioDestino,
            @RequestParam Double valor,
            Authentication authentication) {
        Long idUsuarioOrigem = Long.parseLong(authentication.getName());
        Transacao transacao = transacaoService.realizarTransacaoSincrona(idUsuarioOrigem, idUsuarioDestino, valor);
        return ResponseEntity.ok(transacao);
    }
}