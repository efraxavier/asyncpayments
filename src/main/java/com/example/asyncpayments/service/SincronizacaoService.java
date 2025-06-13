package com.example.asyncpayments.service;

import com.example.asyncpayments.entity.ContaAssincrona;
import com.example.asyncpayments.entity.ContaSincrona;
import com.example.asyncpayments.entity.StatusTransacao;
import com.example.asyncpayments.entity.Transacao;
import com.example.asyncpayments.entity.TipoOperacao;
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
                contaSincrona.setSaldo(contaSincrona.getSaldo() + contaAssincrona.getSaldo());
                contaAssincrona.setSaldo(0.0);
                contaAssincrona.sincronizar();

                contaSincronaRepository.save(contaSincrona);
                contaAssincronaRepository.save(contaAssincrona);
            }
        }
    }


    public void sincronizarConta(Long idContaAssincrona) {
    ContaAssincrona contaAssincrona = contaAssincronaRepository.findById(idContaAssincrona)
            .orElseThrow(() -> new IllegalArgumentException("Conta assíncrona não encontrada."));

    OffsetDateTime ultimaSincronizacao = contaAssincrona.getUltimaSincronizacao();
    if (ultimaSincronizacao != null &&
        java.time.Duration.between(ultimaSincronizacao, OffsetDateTime.now(ZoneOffset.UTC)).toHours() > 72) {
        contaAssincrona.bloquear();
        contaAssincronaRepository.save(contaAssincrona);
        throw new IllegalStateException("Sincronização manual fora do prazo. Conta bloqueada.");
    }

    ContaSincrona contaSincrona = contaSincronaRepository.findByUserId(contaAssincrona.getUser().getId());
    if (contaSincrona == null) {
        throw new IllegalArgumentException("Conta síncrona correspondente não encontrada.");
    }

    contaSincrona.setSaldo(contaSincrona.getSaldo() + contaAssincrona.getSaldo());
    contaAssincrona.setSaldo(0.0);
    contaAssincrona.setUltimaSincronizacao(OffsetDateTime.now(ZoneOffset.UTC));
    contaAssincronaRepository.save(contaAssincrona);
    contaSincronaRepository.save(contaSincrona);
}

    public void verificarEBloquearContas() {
        List<ContaAssincrona> contasAssincronas = contaAssincronaRepository.findAll();

        for (ContaAssincrona contaAssincrona : contasAssincronas) {
            if (contaAssincrona.isBloqueada()) {
                continue;
            }

            if (contaAssincrona.getUltimaSincronizacao() != null &&
                contaAssincrona.getUltimaSincronizacao().isBefore(OffsetDateTime.now(ZoneOffset.UTC).minusHours(72))) {
                contaAssincrona.bloquear();
                contaAssincronaRepository.save(contaAssincrona); 
            } else {
                sincronizarConta(contaAssincrona.getId()); 
            }
        }
    }

    /**
     * Sincroniza todas as transações pendentes, marcando como SINCRONIZADA se dentro do prazo,
     * ou ROLLBACK se fora do prazo.
     */
    public void sincronizarTransacoesPendentes() {
        List<Transacao> pendentes = transacaoRepository.findByStatus(StatusTransacao.PENDENTE);

        for (Transacao transacao : pendentes) {
            OffsetDateTime dataEnvio = transacao.getDataCriacao();
            OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);

            if (dataEnvio != null && java.time.Duration.between(dataEnvio, agora).toHours() > 72) {
                transacao.setStatus(StatusTransacao.ROLLBACK);
                transacao.setDescricao("Rollback: Transação não sincronizada em 72h.");
            } else {
                transacao.setStatus(StatusTransacao.SINCRONIZADA);
                transacao.setDataAtualizacao(agora);
            }
            transacaoRepository.save(transacao);
        }
    }

    /**
     * Reprocessa transações pendentes, atualizando status conforme o prazo.
     */
    public void reprocessarTransacoesPendentes() {
        List<Transacao> pendentes = transacaoRepository.findByStatus(StatusTransacao.PENDENTE);

        for (Transacao transacao : pendentes) {
            try {
                OffsetDateTime dataEnvio = transacao.getDataCriacao();
                OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);

                if (dataEnvio != null && java.time.Duration.between(dataEnvio, agora).toHours() > 72) {
                    transacao.setStatus(StatusTransacao.ROLLBACK);
                    transacao.setDescricao("Rollback: Transação não sincronizada em 72h.");
                } else {
                    transacao.setStatus(StatusTransacao.SINCRONIZADA);
                    transacao.setDataAtualizacao(agora);
                }
                transacaoRepository.save(transacao);
            } catch (Exception e) {
                transacao.setStatus(StatusTransacao.ERRO);
                transacao.setDescricao("Erro ao reprocessar: " + e.getMessage());
                transacaoRepository.save(transacao);
            }
        }
    }

    /**
     * Marca como ROLLBACK todas as transações pendentes fora do prazo.
     */
    public void rollbackTransacoesNaoSincronizadas() {
        List<Transacao> pendentes = transacaoRepository.findByStatus(StatusTransacao.PENDENTE);

        for (Transacao transacao : pendentes) {
            OffsetDateTime dataEnvio = transacao.getDataCriacao();
            OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);

            if (dataEnvio != null && java.time.Duration.between(dataEnvio, agora).toHours() > 72) {
                transacao.setStatus(StatusTransacao.ROLLBACK);
                transacao.setDescricao("Rollback: Transação não sincronizada em 72h.");
                transacaoRepository.save(transacao);
            }
        }
    }

    public OffsetDateTime buscarDataSincronizacao(Long contaAssincronaId) {
        return contaAssincronaRepository.findById(contaAssincronaId)
                .map(ContaAssincrona::getUltimaSincronizacao)
                .orElse(null);
    }
}
