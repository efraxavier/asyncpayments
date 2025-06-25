package com.example.asyncpayments.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.example.asyncpayments.entity.Transacao;
import com.example.asyncpayments.entity.StatusTransacao;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class FilaTransacaoService {

    private static final Logger logger = LoggerFactory.getLogger(FilaTransacaoService.class);
    private final Map<Long, StatusTransacao> statusTransacoes = new ConcurrentHashMap<>(); 

    public void adicionarNaFila(Transacao transacao) {
        statusTransacoes.put(transacao.getId(), StatusTransacao.PENDENTE);
        logger.info("[FILA] Transação adicionada na fila: id={}", transacao.getId());
    }

    public StatusTransacao consultarStatus(Long idTransacao) {
        if (!statusTransacoes.containsKey(idTransacao)) {
            logger.warn("[FILA] Consulta de status: transação não encontrada: id={}", idTransacao);
            throw new IllegalArgumentException("Transação não encontrada.");
        }
        logger.info("[FILA] Consulta de status: id={} status={}", idTransacao, statusTransacoes.get(idTransacao));
        return statusTransacoes.get(idTransacao);
    }

    public void atualizarStatus(Long idTransacao, StatusTransacao novoStatus) {
        statusTransacoes.put(idTransacao, novoStatus);
        logger.info("[FILA] Status atualizado: id={} novoStatus={}", idTransacao, novoStatus);
    }
}