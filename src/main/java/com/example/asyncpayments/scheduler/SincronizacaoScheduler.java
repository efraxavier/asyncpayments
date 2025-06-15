package com.example.asyncpayments.scheduler;

import com.example.asyncpayments.service.SincronizacaoService;
import com.example.asyncpayments.entity.ContaAssincrona;
import com.example.asyncpayments.entity.TipoOperacao;
import com.example.asyncpayments.entity.Transacao;
import com.example.asyncpayments.repository.ContaAssincronaRepository;
import com.example.asyncpayments.repository.TransacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SincronizacaoScheduler {

    private final SincronizacaoService sincronizacaoService;
    private final ContaAssincronaRepository contaAssincronaRepository;
    private final TransacaoRepository transacaoRepository;

    @Scheduled(fixedRate = 60000)
    public void verificarEBloquearContas() {
        for (ContaAssincrona contaAssincrona : contaAssincronaRepository.findAll()) {
            if (contaAssincrona.isBloqueada()) {
                continue;
            }

            OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);

            List<Transacao> sincronizacoes = transacaoRepository
                    .findByIdUsuarioOrigem(contaAssincrona.getUser().getId())
                    .stream()
                    .filter(t -> TipoOperacao.SINCRONIZACAO.equals(t.getTipoOperacao()))
                    .toList();

            Transacao ultimaSincronizacao = sincronizacoes.stream()
                    .max((a, b) -> a.getDataCriacao().compareTo(b.getDataCriacao()))
                    .orElse(null);

            OffsetDateTime dataUltimaTransacao = ultimaSincronizacao != null ? ultimaSincronizacao.getDataCriacao() : null;
            OffsetDateTime dataUltimaConta = contaAssincrona.getUltimaSincronizacao();

            boolean precisaSincronizar = dataUltimaTransacao == null ||
                    java.time.Duration.between(dataUltimaTransacao, agora).toHours() >= 72;

            boolean precisaBloquear = dataUltimaConta != null &&
                    java.time.Duration.between(dataUltimaConta, agora).toHours() > 72;

            if (precisaBloquear) {
                contaAssincrona.bloquear();
            } else if (precisaSincronizar) {
                sincronizacaoService.sincronizarPorId(contaAssincrona.getId());
            }

            contaAssincronaRepository.save(contaAssincrona);
        }
    }

    @Scheduled(fixedRate = 60000)
    public void executarRollbackTransacoesNaoSincronizadas() {
        sincronizacaoService.rollbackTransacoesNaoSincronizadas();
    }

    @Scheduled(fixedRate = 60000)
    public void reprocessarTransacoesPendentes() {
        sincronizacaoService.reprocessarTransacoesPendentes();
    }
}
