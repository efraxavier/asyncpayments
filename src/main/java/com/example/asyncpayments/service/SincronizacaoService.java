package com.example.asyncpayments.service;

import com.example.asyncpayments.entity.ContaAssincrona;
import com.example.asyncpayments.entity.ContaSincrona;
import com.example.asyncpayments.entity.Transacao;
import com.example.asyncpayments.entity.TipoTransacao;
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

    public void sincronizarContas() {
        List<ContaAssincrona> contasAssincronas = contaAssincronaRepository.findAll();

        for (ContaAssincrona contaAssincrona : contasAssincronas) {
            if (contaAssincrona.isBloqueada()) {
                continue; // Ignora contas bloqueadas
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

    ContaSincrona contaSincrona = contaSincronaRepository.findByUserId(contaAssincrona.getUser().getId());
    if (contaSincrona == null) {
        throw new IllegalArgumentException("Conta síncrona correspondente não encontrada.");
    }

    contaSincrona.setSaldo(contaSincrona.getSaldo() + contaAssincrona.getSaldo());
    contaAssincrona.setSaldo(0.0);
    contaAssincrona.setUltimaSincronizacao(OffsetDateTime.now(ZoneOffset.UTC));
    contaAssincrona.setBloqueada(false);

    contaSincronaRepository.save(contaSincrona);
    contaAssincronaRepository.save(contaAssincrona);
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
                contaAssincronaRepository.save(contaAssincrona); // Salva após bloquear
            } else {
                sincronizarConta(contaAssincrona.getId()); // Sincroniza e salva dentro do método
            }
        }
    }

    public void rollbackTransacoesNaoSincronizadas() {
        List<ContaAssincrona> contasAssincronas = contaAssincronaRepository.findAll();

        for (ContaAssincrona contaAssincrona : contasAssincronas) {
            if (contaAssincrona.getUltimaSincronizacao() != null &&
                contaAssincrona.getUltimaSincronizacao().isBefore(OffsetDateTime.now(ZoneOffset.UTC).minusHours(72))) {

                List<Transacao> transacoesPendentes = transacaoRepository.findBySincronizadaFalse()
                    .stream()
                    .filter(t -> t.getIdUsuarioOrigem().equals(contaAssincrona.getUser().getId()))
                    .toList();

                for (Transacao transacao : transacoesPendentes) {
                    transacao.setSincronizada(false); // Marca como não sincronizada
                    transacao.setDescricao("Rollback: Transação não sincronizada em 72h.");
                    transacao.setTipoTransacao(TipoTransacao.ASSINCRONA); // Defina o tipo de transação
                    transacaoRepository.save(transacao);
                }

                contaAssincrona.bloquear(); // Bloqueia a conta
                contaAssincronaRepository.save(contaAssincrona);
            }
        }
    }


    public void reprocessarTransacoesPendentes() {
        List<Transacao> transacoesPendentes = transacaoRepository.findBySincronizadaFalse();

        for (Transacao transacao : transacoesPendentes) {
            try {
                OffsetDateTime dataEnvio = transacao.getDataCriacao();
                OffsetDateTime dataAtual = OffsetDateTime.now(ZoneOffset.UTC);

                // Verifica se a transação foi enviada há mais de 72 horas
                if (dataEnvio != null && java.time.Duration.between(dataEnvio, dataAtual).toHours() > 72) {
                    transacao.setSincronizada(false);
                    transacao.setDescricao("Rollback: Transação não sincronizada em 72h.");
                    transacao.setTipoTransacao(TipoTransacao.ASSINCRONA); // Defina o tipo de transação
                    transacaoRepository.save(transacao);
                    continue;
                }

                // Reprocessa transações válidas
                transacao.setSincronizada(true);
                transacao.setDataAtualizacao(dataAtual);
                transacao.setTipoTransacao(TipoTransacao.ASSINCRONA); // Defina o tipo de transação
                transacaoRepository.save(transacao);
            } catch (Exception e) {
                transacao.setDescricao("Erro ao reprocessar: " + e.getMessage());
                transacaoRepository.save(transacao);
            }
        }
    }
}
