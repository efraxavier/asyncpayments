package com.example.asyncpayments.service;

import com.example.asyncpayments.entity.ContaAssincrona;
import com.example.asyncpayments.entity.ContaSincrona;
import com.example.asyncpayments.repository.ContaAssincronaRepository;
import com.example.asyncpayments.repository.ContaSincronaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SincronizacaoService {

    private final ContaAssincronaRepository contaAssincronaRepository;
    private final ContaSincronaRepository contaSincronaRepository;

    public void sincronizarContas() {
        List<ContaAssincrona> contasAssincronas = contaAssincronaRepository.findAll();

        for (ContaAssincrona contaAssincrona : contasAssincronas) {
            if (contaAssincrona.isBloqueada()) {
                continue; 
            }

            OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);
            if (contaAssincrona.getUltimaSincronizacao().isBefore(agora.minusHours(72))) {
                contaAssincrona.bloquear();
                contaAssincronaRepository.save(contaAssincrona);
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
    
            if (contaAssincrona.getSaldo() > 0 &&
                contaAssincrona.getUltimaSincronizacao().isBefore(OffsetDateTime.now(ZoneOffset.UTC).minusHours(72))) {
                contaAssincrona.setBloqueada(true);
                contaAssincronaRepository.save(contaAssincrona);
            }
        }
    }

}
