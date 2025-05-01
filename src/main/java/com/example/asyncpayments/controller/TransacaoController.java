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

    @GetMapping("/todas")
    public ResponseEntity<List<Transacao>> listarTodasTransacoes() {
        List<Transacao> transacoes = transacaoService.listarTodasTransacoes();
        return ResponseEntity.ok(transacoes);
    }

    @PostMapping("/realizar")
    public ResponseEntity<?> realizarTransacao(@Valid @RequestBody TransacaoRequest request) {
        try {
            Transacao transacao = transacaoService.realizarTransacao(
                request.getIdUsuarioOrigem(),
                request.getIdUsuarioDestino(),
                request.getValor(),
                request.getMetodoConexao(),
                request.getGatewayPagamento()
            );
            return ResponseEntity.ok(transacao);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno: " + e.getMessage());
        }
    }
}