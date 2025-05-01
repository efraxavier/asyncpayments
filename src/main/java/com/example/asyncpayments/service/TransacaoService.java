package com.example.asyncpayments.service;

import com.example.asyncpayments.entity.Transacao;
import com.example.asyncpayments.entity.ContaAssincrona;
import com.example.asyncpayments.entity.ContaSincrona;
import com.example.asyncpayments.entity.TipoTransacao;
import com.example.asyncpayments.entity.GatewayPagamento;
import com.example.asyncpayments.entity.MetodoConexao;
import com.example.asyncpayments.repository.ContaSincronaRepository;
import com.example.asyncpayments.repository.ContaAssincronaRepository;
import com.example.asyncpayments.repository.TransacaoRepository;
import com.example.asyncpayments.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransacaoService {

    private final TransacaoRepository transacaoRepository;
    private final UserRepository userRepository;
    private final ContaSincronaRepository contaSincronaRepository;
    private final ContaAssincronaRepository contaAssincronaRepository;

    public Transacao criarTransacao(Transacao transacao, String emailUsuarioOrigem) {
        transacao.setDataCriacao(LocalDateTime.now());
        transacao.setSincronizada(false); // Por padrão, transações não são sincronizadas
        return transacaoRepository.save(transacao);
    }

    public Long buscarIdUsuarioPorEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"))
                .getId();
    }

    public List<Transacao> listarTransacoes(String emailUsuario) {
        Long idUsuario = buscarIdUsuarioPorEmail(emailUsuario);
        return transacaoRepository.findByIdUsuarioOrigem(idUsuario);
    }

    @Transactional
public Transacao realizarTransacao(Long idUsuarioOrigem, Long idUsuarioDestino, Double valor, MetodoConexao metodoConexao, GatewayPagamento gatewayPagamento) {
    if (idUsuarioOrigem == null || idUsuarioDestino == null) {
        throw new IllegalArgumentException("Os IDs dos usuários de origem e destino são obrigatórios.");
    }
    if (valor == null || valor <= 0) {
        throw new IllegalArgumentException("O valor da transação deve ser maior que zero.");
    }

    if (metodoConexao == MetodoConexao.INTERNET) {
        // Transações por internet devem ser feitas em contas síncronas
        ContaSincrona contaOrigem = contaSincronaRepository.findByUserId(idUsuarioOrigem);
        ContaSincrona contaDestino = contaSincronaRepository.findByUserId(idUsuarioDestino);

        if (contaOrigem == null || contaDestino == null) {
            throw new IllegalArgumentException("Conta de origem ou destino não encontrada.");
        }

        if (contaOrigem.getSaldo() < valor) {
            throw new IllegalArgumentException("Saldo insuficiente na conta de origem.");
        }

        contaOrigem.setSaldo(contaOrigem.getSaldo() - valor);
        contaDestino.setSaldo(contaDestino.getSaldo() + valor);

        contaSincronaRepository.save(contaOrigem);
        contaSincronaRepository.save(contaDestino);
    } else {
        // Transações por bluetooth, sms ou nfc devem ser feitas em contas assíncronas
        ContaAssincrona contaOrigem = contaAssincronaRepository.findByUserId(idUsuarioOrigem);
        ContaAssincrona contaDestino = contaAssincronaRepository.findByUserId(idUsuarioDestino);

        if (contaOrigem == null || contaDestino == null) {
            throw new IllegalArgumentException("Conta de origem ou destino não encontrada.");
        }

        if (contaOrigem.getSaldo() < valor) {
            throw new IllegalArgumentException("Saldo insuficiente na conta de origem.");
        }

        contaOrigem.setSaldo(contaOrigem.getSaldo() - valor);
        contaDestino.setSaldo(contaDestino.getSaldo() + valor);

        contaAssincronaRepository.save(contaOrigem);
        contaAssincronaRepository.save(contaDestino);
    }

    Transacao transacao = new Transacao();
    transacao.setIdUsuarioOrigem(idUsuarioOrigem);
    transacao.setIdUsuarioDestino(idUsuarioDestino);
    transacao.setValor(valor);
    transacao.setMetodoConexao(metodoConexao);
    transacao.setGatewayPagamento(gatewayPagamento);
    transacao.setSincronizada(metodoConexao == MetodoConexao.INTERNET);

    return transacaoRepository.save(transacao);
}

    public List<Transacao> listarTodasTransacoes() {
        return transacaoRepository.findAll(); // Busca todas as transações
    }
}