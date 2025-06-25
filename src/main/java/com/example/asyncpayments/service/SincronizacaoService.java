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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
@Service
public class SincronizacaoService {

    private static final Logger logger = LoggerFactory.getLogger(SincronizacaoService.class);

    private final ContaAssincronaRepository contaAssincronaRepository;
    private final ContaSincronaRepository contaSincronaRepository;
    private final TransacaoRepository transacaoRepository;
    private final FilaTransacaoService filaTransacaoService;

    /**
     * Roda periodicamente para marcar como ROLLBACK transações pendentes vencidas (>72h).
     * O backend nunca fica offline; sincronização é sempre iniciada pelo app.
     */
    public void rollbackTransacoesNaoSincronizadas() {
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);
        List<Transacao> pendentes = transacaoRepository.findByStatus(StatusTransacao.PENDENTE);

        for (Transacao transacao : pendentes) {
            if (transacao.getDataCriacao() != null &&
                java.time.Duration.between(transacao.getDataCriacao(), agora).toHours() > 72) {
                transacao.setStatus(StatusTransacao.ROLLBACK);
                transacao.setDescricao("Rollback: Transação não sincronizada em 72h.");
                transacao.setDataAtualizacao(agora);
                transacaoRepository.save(transacao);

                // Devolve saldo para conta síncrona de origem
                ContaSincrona contaSincrona = contaSincronaRepository.findByUserId(transacao.getIdUsuarioOrigem());
                if (contaSincrona != null) {
                    contaSincrona.setSaldo(contaSincrona.getSaldo() + transacao.getValor());
                    contaSincronaRepository.save(contaSincrona);
                }
            }
        }
    }
}
