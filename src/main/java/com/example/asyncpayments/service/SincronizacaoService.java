package com.example.asyncpayments.service;

import com.example.asyncpayments.entity.ContaAssincrona;
import com.example.asyncpayments.entity.ContaSincrona;
import com.example.asyncpayments.repository.ContaAssincronaRepository;
import com.example.asyncpayments.repository.ContaSincronaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
                continue; // Ignorar contas bloqueadas
            }

            LocalDateTime agora = LocalDateTime.now();
            if (contaAssincrona.getUltimaSincronizacao().isBefore(agora.minusMinutes(5))) {
                // Bloquear conta se a última sincronização foi há mais de 12 horas
                contaAssincrona.bloquear();
                contaAssincronaRepository.save(contaAssincrona);
                continue;
            }

            // Sincronizar saldo com a conta síncrona
            ContaSincrona contaSincrona = contaSincronaRepository.findByUserId(contaAssincrona.getUser().getId());
            if (contaSincrona != null) {
                contaSincrona.setSaldo(contaSincrona.getSaldo() + contaAssincrona.getSaldo());
                contaAssincrona.setSaldo(0.0); // Zerar saldo da conta assíncrona após sincronização
                contaAssincrona.sincronizar();

                contaSincronaRepository.save(contaSincrona);
                contaAssincronaRepository.save(contaAssincrona);
            }
        }
    }

    /**
     * Sincroniza uma conta assíncrona específica pelo ID e a desbloqueia.
     */
    public void sincronizarConta(Long idContaAssincrona) {
    ContaAssincrona contaAssincrona = contaAssincronaRepository.findById(idContaAssincrona)
            .orElseThrow(() -> new IllegalArgumentException("Conta assíncrona não encontrada."));

    // Sincronizar saldo com a conta síncrona correspondente
    ContaSincrona contaSincrona = contaSincronaRepository.findByUserId(contaAssincrona.getUser().getId());
    if (contaSincrona == null) {
        throw new IllegalArgumentException("Conta síncrona correspondente não encontrada.");
    }

    // Transferir saldo da conta assíncrona para a conta síncrona
    contaSincrona.setSaldo(contaSincrona.getSaldo() + contaAssincrona.getSaldo());
    contaAssincrona.setSaldo(0.0); // Zerar saldo da conta assíncrona após sincronização

    // Atualizar a última sincronização e desbloquear a conta
    contaAssincrona.setUltimaSincronizacao(LocalDateTime.now());
    contaAssincrona.setBloqueada(false);

    // Salvar as alterações
    contaSincronaRepository.save(contaSincrona);
    contaAssincronaRepository.save(contaAssincrona);
    
    }

    public void verificarEBloquearContas() {
        List<ContaAssincrona> contasAssincronas = contaAssincronaRepository.findAll();
    
        for (ContaAssincrona contaAssincrona : contasAssincronas) {
            // Verificar se a conta já está bloqueada
            if (contaAssincrona.isBloqueada()) {
                continue; // Ignorar contas já bloqueadas
            }
    
            // Verificar se a conta tem saldo e se a última sincronização foi há mais de 12 horas
            if (contaAssincrona.getSaldo() > 0 &&
                contaAssincrona.getUltimaSincronizacao().isBefore(LocalDateTime.now().minusMinutes(3))) {
                contaAssincrona.setBloqueada(true);
                contaAssincronaRepository.save(contaAssincrona);
            }
        }
    }

}
