package com.example.asyncpayments.controller;

import com.example.asyncpayments.service.SincronizacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sincronizacao")
@RequiredArgsConstructor
public class SincronizacaoController {

    private final SincronizacaoService sincronizacaoService;

    /**
     * Sincroniza todas as contas manualmente.
     */
    @PostMapping("/manual")
    public ResponseEntity<?> sincronizarContasManual() {
        try {
            sincronizacaoService.sincronizarContas();
            return ResponseEntity.ok("Sincronização de todas as contas realizada com sucesso.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro ao sincronizar contas: " + e.getMessage());
        }
    }

    /**
     * Sincroniza uma conta específica pelo ID e a desbloqueia.
     */
    @PostMapping("/manual/{id}")
    public ResponseEntity<?> sincronizarContaPorId(@PathVariable Long id) {
        try {
            sincronizacaoService.sincronizarConta(id);
            return ResponseEntity.ok("Sincronização realizada com sucesso para a conta ID: " + id);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro ao sincronizar conta: " + e.getMessage());
        }
    }
}
