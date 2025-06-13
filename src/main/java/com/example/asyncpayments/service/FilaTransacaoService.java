package com.example.asyncpayments.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.example.asyncpayments.entity.Transacao;
import com.example.asyncpayments.entity.StatusTransacao;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FilaTransacaoService {

    private final Map<Long, StatusTransacao> statusTransacoes = new ConcurrentHashMap<>(); 

    public void adicionarNaFila(Transacao transacao) {
        statusTransacoes.put(transacao.getId(), StatusTransacao.PENDENTE);
        System.out.println("Transação adicionada na fila: " + transacao.getId());
    }

    public StatusTransacao consultarStatus(Long idTransacao) {
        if (!statusTransacoes.containsKey(idTransacao)) {
            throw new IllegalArgumentException("Transação não encontrada.");
        }
        return statusTransacoes.get(idTransacao);
    }

    public void atualizarStatus(Long idTransacao, StatusTransacao novoStatus) {
        statusTransacoes.put(idTransacao, novoStatus);
    }
}