package com.example.asyncpayments.controller;

import com.example.asyncpayments.repository.UserRepository;
import com.example.asyncpayments.service.SincronizacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/sincronizacao")
@RequiredArgsConstructor
public class SincronizacaoController {

    private final SincronizacaoService sincronizacaoService;
    private final UserRepository userRepository;

    @PostMapping("/manual")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> sincronizarContasManual() {
        try {
            sincronizacaoService.sincronizar();
            return ResponseEntity.ok("Sincronização de todas as contas realizada com sucesso.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro ao sincronizar contas: " + e.getMessage());
        }
    }

    @PostMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> sincronizarMinhaConta(Authentication authentication) {
        try {
            String email = authentication.getName();
            var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
            Long contaAssincronaId = user.getContaAssincrona().getId();
            sincronizacaoService.sincronizarPorId(contaAssincronaId);
            return ResponseEntity.ok("Sincronização realizada com sucesso para sua conta.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro ao sincronizar sua conta: " + e.getMessage());
        }
    }

    @PostMapping("/manual/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> sincronizarContaPorId(@PathVariable Long id) {
        try {
            sincronizacaoService.sincronizarPorId(id);
            return ResponseEntity.ok("Sincronização realizada com sucesso para a conta ID: " + id);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro ao sincronizar conta: " + e.getMessage());
        }
    }
}
