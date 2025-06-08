package com.example.asyncpayments.scheduler;

import com.example.asyncpayments.service.SincronizacaoService;
import com.example.asyncpayments.entity.ContaAssincrona;
import com.example.asyncpayments.repository.ContaAssincronaRepository;
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


    @Scheduled(fixedRate = 60000) // Executa a cada 1 minuto
    public void verificarEBloquearContas() {
        List<ContaAssincrona> contasAssincronas = contaAssincronaRepository.findAll();

        for (ContaAssincrona contaAssincrona : contasAssincronas) {
            if (contaAssincrona.isBloqueada()) {
                continue; 
            }

            if (contaAssincrona.getUltimaSincronizacao() != null &&
                contaAssincrona.getUltimaSincronizacao().isBefore(OffsetDateTime.now(ZoneOffset.UTC).minusHours(72))) {
                contaAssincrona.bloquear();
            } else {
                sincronizacaoService.sincronizarConta(contaAssincrona.getId());
            }

            contaAssincronaRepository.save(contaAssincrona);
        }
    }

    @Scheduled(fixedRate = 60000) // Executa a cada 1 minuto
    public void executarRollbackTransacoesNaoSincronizadas() {
        sincronizacaoService.rollbackTransacoesNaoSincronizadas();
    }

    @Scheduled(fixedRate = 60000) // Executa a cada 1 minuto
    public void reprocessarTransacoesPendentes() {
        sincronizacaoService.reprocessarTransacoesPendentes();
    }
}
