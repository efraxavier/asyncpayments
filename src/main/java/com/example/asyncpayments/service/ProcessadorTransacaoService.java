package com.example.asyncpayments.service;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.example.asyncpayments.entity.Transacao;
import com.example.asyncpayments.entity.StatusTransacao; 
import com.example.asyncpayments.repository.TransacaoRepository;

@Service
@RequiredArgsConstructor
public class ProcessadorTransacaoService {

    private final TransacaoRepository transacaoRepository;

    @Scheduled(fixedRate = 60000) 
    public void processarTransacoesPendentes() {
        List<Transacao> transacoesPendentes = transacaoRepository.findByStatus(StatusTransacao.PENDENTE);

        for (Transacao transacao : transacoesPendentes) {
            try {
                transacao.setStatus(StatusTransacao.SINCRONIZADA);
                transacao.setDataAtualizacao(OffsetDateTime.now(ZoneOffset.UTC));
                transacaoRepository.save(transacao);
            } catch (Exception e) {
                transacao.setStatus(StatusTransacao.ERRO);
                transacao.setDescricao("Erro ao processar: " + e.getMessage());
                transacaoRepository.save(transacao);
            }
        }
    }
}