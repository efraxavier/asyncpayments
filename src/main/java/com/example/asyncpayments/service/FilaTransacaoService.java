package com.example.asyncpayments.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.example.asyncpayments.entity.Transacao;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FilaTransacaoService {

    private final Map<Long, String> statusTransacoes = new ConcurrentHashMap<>(); // Gerencia os status das transações

    public void adicionarNaFila(Transacao transacao) {
        statusTransacoes.put(transacao.getId(), "PENDENTE");
        System.out.println("Transação adicionada na fila: " + transacao.getId());
    }

    public String consultarStatus(Long idTransacao) {
        return statusTransacoes.getOrDefault(idTransacao, "NÃO ENCONTRADA");
    }

    public void atualizarStatus(Long idTransacao, String novoStatus) {
        statusTransacoes.put(idTransacao, novoStatus);
    }
}