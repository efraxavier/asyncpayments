package com.example.asyncpayments.service;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.example.asyncpayments.entity.Transacao;
import com.example.asyncpayments.entity.ContaAssincrona;
import com.example.asyncpayments.repository.TransacaoRepository;
import com.example.asyncpayments.repository.ContaAssincronaRepository;

@Service
@RequiredArgsConstructor
public class ProcessadorTransacaoService {

    private final FilaTransacaoService filaTransacaoService;
    private final TransacaoRepository transacaoRepository;
    private final ContaAssincronaRepository contaAssincronaRepository;

    @Scheduled(fixedRate = 60000) // Executa a cada 1 minuto
    public void processarTransacoesPendentes() {
        List<Transacao> transacoesPendentes = transacaoRepository.findBySincronizadaFalse();

        for (Transacao transacao : transacoesPendentes) {
            try {
                validarTransacao(transacao);
                transacao.setSincronizada(true);
                transacao.setDataAtualizacao(OffsetDateTime.now(ZoneOffset.UTC));
                transacaoRepository.save(transacao);
                filaTransacaoService.atualizarStatus(transacao.getId(), "PROCESSADA");
            } catch (Exception e) {
                filaTransacaoService.atualizarStatus(transacao.getId(), "NEGADA: " + e.getMessage());
            }
        }
    }

    public void reprocessarTransacoesPendentes() {
        List<Transacao> transacoesPendentes = transacaoRepository.findBySincronizadaFalse();

        for (Transacao transacao : transacoesPendentes) {
            try {
                validarTransacao(transacao);
                transacao.setSincronizada(true);
                transacao.setDataAtualizacao(OffsetDateTime.now(ZoneOffset.UTC));
                transacaoRepository.save(transacao);
            } catch (Exception e) {
                transacao.setDescricao("Erro ao reprocessar: " + e.getMessage());
                transacaoRepository.save(transacao);
            }
        }
    }

    private void validarTransacao(Transacao transacao) {
        OffsetDateTime dataCriacao = transacao.getDataCriacao();
        if (dataCriacao.isBefore(OffsetDateTime.now(ZoneOffset.UTC).minusHours(72))) {
            throw new IllegalStateException("Transação enviada há mais de 72 horas.");
        }

        ContaAssincrona contaOrigem = contaAssincronaRepository.findByUserId(transacao.getIdUsuarioOrigem());
        if (contaOrigem != null && contaOrigem.isBloqueada()) {
            throw new IllegalStateException("Conta de origem está bloqueada.");
        }
    }
}