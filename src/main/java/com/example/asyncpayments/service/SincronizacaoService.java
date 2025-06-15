package com.example.asyncpayments.service;

import com.example.asyncpayments.entity.ContaAssincrona;
import com.example.asyncpayments.entity.ContaSincrona;
import com.example.asyncpayments.entity.StatusTransacao;
import com.example.asyncpayments.entity.Transacao;
import com.example.asyncpayments.entity.TipoOperacao;
import com.example.asyncpayments.entity.MetodoConexao;
import com.example.asyncpayments.entity.GatewayPagamento;
import com.example.asyncpayments.repository.ContaAssincronaRepository;
import com.example.asyncpayments.repository.ContaSincronaRepository;
import com.example.asyncpayments.repository.TransacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@RequiredArgsConstructor
@Service
public class SincronizacaoService {

    private final ContaAssincronaRepository contaAssincronaRepository;
    private final ContaSincronaRepository contaSincronaRepository;
    private final TransacaoRepository transacaoRepository;
    private final FilaTransacaoService filaTransacaoService;

    public void sincronizarContas() {
        List<ContaAssincrona> contasAssincronas = contaAssincronaRepository.findAll();

        for (ContaAssincrona contaAssincrona : contasAssincronas) {
            if (contaAssincrona.isBloqueada()) {
                continue;
            }

            ContaSincrona contaSincrona = contaSincronaRepository.findByUserId(contaAssincrona.getUser().getId());
            if (contaSincrona != null) {
                Double valor = contaAssincrona.getSaldo();
                if (valor == null || valor <= 0) continue;

                // Busca todas as transações de sincronização do usuário
                List<Transacao> sincronizacoes = transacaoRepository
                        .findByIdUsuarioOrigem(contaAssincrona.getUser().getId())
                        .stream()
                        .filter(t -> TipoOperacao.SINCRONIZACAO.equals(t.getTipoOperacao()))
                        .toList();

                boolean podeSincronizar = true;
                if (!sincronizacoes.isEmpty()) {
                    Transacao ultimaSincronizacao = sincronizacoes.stream()
                            .max((a, b) -> a.getDataCriacao().compareTo(b.getDataCriacao()))
                            .orElse(null);
                    if (ultimaSincronizacao != null && ultimaSincronizacao.getDataCriacao() != null) {
                        long horas = java.time.Duration.between(
                                ultimaSincronizacao.getDataCriacao(), OffsetDateTime.now(ZoneOffset.UTC)).toHours();
                        if (horas < 72) {
                            podeSincronizar = false;
                        }
                    }
                }

                if (podeSincronizar) {
                    contaSincrona.setSaldo(contaSincrona.getSaldo() + valor);
                    contaAssincrona.setSaldo(0.0);
                    contaAssincrona.sincronizar();

                    contaSincronaRepository.save(contaSincrona);
                    contaAssincronaRepository.save(contaAssincrona);

                    Transacao transacao = new Transacao();
                    transacao.setIdUsuarioOrigem(contaAssincrona.getUser().getId());
                    transacao.setIdUsuarioDestino(contaAssincrona.getUser().getId());
                    transacao.setValor(valor);
                    transacao.setTipoOperacao(TipoOperacao.SINCRONIZACAO);
                    transacao.setMetodoConexao(MetodoConexao.INTERNET);
                    transacao.setGatewayPagamento(GatewayPagamento.INTERNO);
                    transacao.setStatus(StatusTransacao.SINCRONIZADA);
                    transacao.setDescricao("Sincronização de saldo da conta assíncrona para síncrona");
                    transacao.setDataCriacao(OffsetDateTime.now(ZoneOffset.UTC));
                    transacao.setDataAtualizacao(OffsetDateTime.now(ZoneOffset.UTC));
                    var user = contaAssincrona.getUser();
                    transacao.setNomeUsuarioOrigem(user.getNome());
                    transacao.setEmailUsuarioOrigem(user.getEmail());
                    transacao.setCpfUsuarioOrigem(user.getCpf());
                    transacao.setNomeUsuarioDestino(user.getNome());
                    transacao.setEmailUsuarioDestino(user.getEmail());
                    transacao.setCpfUsuarioDestino(user.getCpf());

                    transacaoRepository.save(transacao);

                    filaTransacaoService.adicionarNaFila(transacao);
                    filaTransacaoService.atualizarStatus(transacao.getId(), StatusTransacao.SINCRONIZADA);
                }
            }
        }
    }

    public void sincronizarConta(Long idContaAssincrona) {
        ContaAssincrona contaAssincrona = contaAssincronaRepository.findById(idContaAssincrona)
                .orElseThrow(() -> new IllegalArgumentException("Conta assíncrona não encontrada."));

        ContaSincrona contaSincrona = contaSincronaRepository.findByUserId(contaAssincrona.getUser().getId());
        if (contaSincrona == null) {
            throw new IllegalArgumentException("Conta síncrona correspondente não encontrada.");
        }

        Double valor = contaAssincrona.getSaldo();
        if (valor == null || valor <= 0) {
            return;
        }

        contaSincrona.setSaldo(contaSincrona.getSaldo() + valor);
        contaAssincrona.setSaldo(0.0);
        contaAssincrona.setUltimaSincronizacao(OffsetDateTime.now(ZoneOffset.UTC));
        contaSincronaRepository.save(contaSincrona);
        contaAssincronaRepository.save(contaAssincrona);

        Transacao transacao = new Transacao();
        transacao.setIdUsuarioOrigem(contaAssincrona.getUser().getId());
        transacao.setIdUsuarioDestino(contaAssincrona.getUser().getId());
        transacao.setValor(valor);
        transacao.setTipoOperacao(TipoOperacao.SINCRONIZACAO);
        transacao.setMetodoConexao(MetodoConexao.INTERNET);
        transacao.setGatewayPagamento(GatewayPagamento.INTERNO);
        transacao.setStatus(StatusTransacao.SINCRONIZADA);
        transacao.setDescricao("Sincronização de saldo da conta assíncrona para síncrona");
        transacao.setDataCriacao(OffsetDateTime.now(ZoneOffset.UTC));
        transacao.setDataAtualizacao(OffsetDateTime.now(ZoneOffset.UTC));
        var user = contaAssincrona.getUser();
        transacao.setNomeUsuarioOrigem(user.getNome());
        transacao.setEmailUsuarioOrigem(user.getEmail());
        transacao.setCpfUsuarioOrigem(user.getCpf());
        transacao.setNomeUsuarioDestino(user.getNome());
        transacao.setEmailUsuarioDestino(user.getEmail());
        transacao.setCpfUsuarioDestino(user.getCpf());
        transacaoRepository.save(transacao);

        filaTransacaoService.adicionarNaFila(transacao);
        filaTransacaoService.atualizarStatus(transacao.getId(), StatusTransacao.SINCRONIZADA);
    }

    public void rollbackTransacoesNaoSincronizadas() {
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);
        List<Transacao> pendentes = transacaoRepository.findByStatus(StatusTransacao.PENDENTE);

        for (Transacao transacao : pendentes) {
            if (transacao.getDataCriacao() != null &&
                java.time.Duration.between(transacao.getDataCriacao(), agora).toHours() > 72) {
                transacao.setStatus(StatusTransacao.ROLLBACK);
                transacao.setDataAtualizacao(agora);
                transacaoRepository.save(transacao);
                filaTransacaoService.atualizarStatus(transacao.getId(), StatusTransacao.ROLLBACK);
            }
        }
    }

    public void reprocessarTransacoesPendentes() {
        List<Transacao> pendentes = transacaoRepository.findByStatus(StatusTransacao.PENDENTE);

        for (Transacao transacao : pendentes) {
            try {
                transacao.setStatus(StatusTransacao.SINCRONIZADA);
                filaTransacaoService.atualizarStatus(transacao.getId(), StatusTransacao.SINCRONIZADA);
            } catch (Exception e) {
                transacao.setStatus(StatusTransacao.ERRO);
                filaTransacaoService.atualizarStatus(transacao.getId(), StatusTransacao.ERRO);
            }
            transacao.setDataAtualizacao(OffsetDateTime.now(ZoneOffset.UTC));
            transacaoRepository.save(transacao);
        }
    }
}
