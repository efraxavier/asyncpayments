package com.example.asyncpayments.service;

import com.example.asyncpayments.entity.*;
import com.example.asyncpayments.repository.TransacaoRepository;
import com.example.asyncpayments.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransacaoService {

    private final TransacaoRepository transacaoRepository;
    private final UserRepository userRepository;

    public Transacao criarTransacao(Transacao transacao, String emailUsuario) {
        // Lógica para associar a transação ao usuário autenticado
        return transacaoRepository.save(transacao);
    }

    public Long buscarIdUsuarioPorEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"))
                .getId();
    }

    public List<Transacao> listarTransacoes(String emailUsuario) {
        // Lógica para buscar transações do usuário autenticado
        return transacaoRepository.findAll();
    }

    @Transactional
    public Transacao realizarTransacaoSincrona(Long idUsuarioOrigem, Long idUsuarioDestino, Double valor) {
        // Buscar usuários
        User usuarioOrigem = userRepository.findById(idUsuarioOrigem)
                .orElseThrow(() -> new IllegalArgumentException("Usuário de origem não encontrado"));
        User usuarioDestino = userRepository.findById(idUsuarioDestino)
                .orElseThrow(() -> new IllegalArgumentException("Usuário de destino não encontrado"));

        // Validar contas
        ContaSincrona contaOrigem = usuarioOrigem.getContaSincrona();
        ContaSincrona contaDestino = usuarioDestino.getContaSincrona();
        if (contaOrigem == null || contaDestino == null) {
            throw new IllegalStateException("Uma ou ambas as contas não existem");
        }

        // Validar saldo suficiente
        if (contaOrigem.getSaldo() < valor) {
            throw new IllegalArgumentException("Saldo insuficiente na conta de origem");
        }

        // Realizar transferência
        contaOrigem.setSaldo(contaOrigem.getSaldo() - valor);
        contaDestino.setSaldo(contaDestino.getSaldo() + valor);

        // Criar transação
        Transacao transacao = new Transacao();
        transacao.setValor(valor);
        transacao.setStatus(StatusTransacao.CONCLUIDA);
        transacao.setConta(contaOrigem);

        return transacaoRepository.save(transacao);
    }
}