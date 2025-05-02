package com.example.asyncpayments.service;

import com.example.asyncpayments.entity.ContaAssincrona;
import com.example.asyncpayments.entity.ContaSincrona;
import com.example.asyncpayments.entity.MetodoConexao;
import com.example.asyncpayments.entity.Transacao;
import com.example.asyncpayments.entity.GatewayPagamento;
import com.example.asyncpayments.repository.ContaAssincronaRepository;
import com.example.asyncpayments.repository.ContaSincronaRepository;
import com.example.asyncpayments.repository.TransacaoRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
@RequiredArgsConstructor
public class TransacaoService {

    private final TransacaoRepository transacaoRepository;
    private final ContaAssincronaRepository contaAssincronaRepository;
    private final ContaSincronaRepository contaSincronaRepository;

    /**
     * Realiza uma transação entre contas.
     * 
     * @param idUsuarioOrigem ID do usuário de origem.
     * @param idUsuarioDestino ID do usuário de destino.
     * @param valor Valor da transação.
     * @param gatewayPagamento Gateway de pagamento utilizado.
     * @param metodoConexao Método de conexão utilizado.
     * @return Transação realizada.
     */
    @Transactional
    public Transacao realizarTransacao(Long idUsuarioOrigem, Long idUsuarioDestino, Double valor, GatewayPagamento gatewayPagamento, MetodoConexao metodoConexao) {
        validarParametrosTransacao(idUsuarioOrigem, idUsuarioDestino, valor);

        if (metodoConexao == MetodoConexao.INTERNET) {
            // Transações por INTERNET devem ser realizadas entre contas síncronas
            realizarTransacaoSincrona(idUsuarioOrigem, idUsuarioDestino, valor);
        } else {
            // Transações por SMS, NFC e BLUETOOTH devem ser realizadas entre contas assíncronas
            realizarTransacaoAssincrona(idUsuarioOrigem, idUsuarioDestino, valor);
        }

        // Criar e salvar a transação
        Transacao transacao = new Transacao();
        transacao.setIdUsuarioOrigem(idUsuarioOrigem);
        transacao.setIdUsuarioDestino(idUsuarioDestino);
        transacao.setValor(valor);
        transacao.setGatewayPagamento(gatewayPagamento);
        transacao.setMetodoConexao(metodoConexao);

        return transacaoRepository.save(transacao);
    }

    private void realizarTransacaoSincrona(Long idUsuarioOrigem, Long idUsuarioDestino, Double valor) {
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
    }

    private void realizarTransacaoAssincrona(Long idUsuarioOrigem, Long idUsuarioDestino, Double valor) {
        ContaAssincrona contaOrigem = contaAssincronaRepository.findByUserId(idUsuarioOrigem);
        ContaAssincrona contaDestino = contaAssincronaRepository.findByUserId(idUsuarioDestino);

        if (contaOrigem == null || contaDestino == null) {
            throw new IllegalArgumentException("Conta de origem ou destino não encontrada.");
        }

        if (contaOrigem.isBloqueada()) {
            throw new IllegalStateException("A conta de origem está bloqueada e não pode realizar transações.");
        }

        if (contaDestino.isBloqueada()) {
            throw new IllegalStateException("A conta de destino está bloqueada e não pode receber transações.");
        }

        if (contaOrigem.getSaldo() < valor) {
            throw new IllegalArgumentException("Saldo insuficiente na conta de origem.");
        }

        contaOrigem.setSaldo(contaOrigem.getSaldo() - valor);
        contaDestino.setSaldo(contaDestino.getSaldo() + valor);

        contaAssincronaRepository.save(contaOrigem);
        contaAssincronaRepository.save(contaDestino);
    }

    /**
     * Valida os parâmetros da transação.
     * 
     * @param idUsuarioOrigem ID do usuário de origem.
     * @param idUsuarioDestino ID do usuário de destino.
     * @param valor Valor da transação.
     */
    private void validarParametrosTransacao(Long idUsuarioOrigem, Long idUsuarioDestino, Double valor) {
        if (idUsuarioOrigem == null || idUsuarioDestino == null) {
            throw new IllegalArgumentException("Os IDs dos usuários de origem e destino são obrigatórios.");
        }
        if (valor == null || valor <= 0) {
            throw new IllegalArgumentException("O valor da transação deve ser maior que zero.");
        }
    }

    /**
     * Atualiza os saldos das contas de origem e destino.
     * 
     * @param idUsuarioOrigem ID do usuário de origem.
     * @param idUsuarioDestino ID do usuário de destino.
     * @param valor Valor da transação.
     */
    private void atualizarSaldos(Long idUsuarioOrigem, Long idUsuarioDestino, Double valor) {
        ContaSincrona contaOrigem = contaSincronaRepository.findByUserId(idUsuarioOrigem);
        ContaSincrona contaDestino = contaSincronaRepository.findByUserId(idUsuarioDestino);

        if (contaOrigem == null || contaDestino == null) {
            throw new IllegalArgumentException("Contas de origem e destino devem existir.");
        }

        if (contaOrigem.getSaldo() < valor) {
            throw new IllegalStateException("Saldo insuficiente na conta de origem.");
        }

        contaOrigem.setSaldo(contaOrigem.getSaldo() - valor);
        contaDestino.setSaldo(contaDestino.getSaldo() + valor);

        contaSincronaRepository.save(contaOrigem);
        contaSincronaRepository.save(contaDestino);
    }

    public List<Transacao> listarTodasTransacoes() {
        return transacaoRepository.findAll();
    }
    

    @Transactional
    public void sincronizarConta(Long idContaAssincrona) {
    ContaAssincrona contaAssincrona = contaAssincronaRepository.findById(idContaAssincrona)
            .orElseThrow(() -> new IllegalArgumentException("Conta assíncrona não encontrada."));

    // Sincronizar saldo com a conta síncrona correspondente
    ContaSincrona contaSincrona = contaSincronaRepository.findByUserId(contaAssincrona.getUser().getId());
    if (contaSincrona == null) {
        throw new IllegalArgumentException("Conta síncrona correspondente não encontrada.");
    }

    contaSincrona.setSaldo(contaSincrona.getSaldo() + contaAssincrona.getSaldo());
    contaAssincrona.setSaldo(0.0); // Zerar saldo da conta assíncrona após sincronização
    contaAssincrona.setUltimaSincronizacao(LocalDateTime.now());
    contaAssincrona.setBloqueada(false); // Desbloquear a conta

    contaSincronaRepository.save(contaSincrona);
    contaAssincronaRepository.save(contaAssincrona);
}

@Transactional
public Transacao transferirSincronaParaAssincrona(Long idUsuario, Double valor) {
    if (valor == null || valor <= 0) {
        throw new IllegalArgumentException("O valor da transação deve ser maior que zero.");
    }

    // Buscar a conta síncrona do usuário
    ContaSincrona contaSincrona = contaSincronaRepository.findByUserId(idUsuario);
    if (contaSincrona == null) {
        throw new IllegalArgumentException("Conta síncrona não encontrada para o usuário.");
    }

    // Verificar saldo suficiente na conta síncrona
    if (contaSincrona.getSaldo() < valor) {
        throw new IllegalArgumentException("Saldo insuficiente na conta síncrona.");
    }

    // Buscar a conta assíncrona do usuário
    ContaAssincrona contaAssincrona = contaAssincronaRepository.findByUserId(idUsuario);
    if (contaAssincrona == null) {
        throw new IllegalArgumentException("Conta assíncrona não encontrada para o usuário.");
    }

    // Verificar se a conta assíncrona está bloqueada
    if (contaAssincrona.isBloqueada()) {
        throw new IllegalStateException("A conta assíncrona está bloqueada. Sincronize a conta para desbloqueá-la.");
    }

    // Realizar a transferência
    contaSincrona.setSaldo(contaSincrona.getSaldo() - valor);
    contaAssincrona.setSaldo(contaAssincrona.getSaldo() + valor);

    // Salvar as alterações
    contaSincronaRepository.save(contaSincrona);
    contaAssincronaRepository.save(contaAssincrona);

    // Criar e salvar a transação
    Transacao transacao = new Transacao();
    transacao.setIdUsuarioOrigem(idUsuario);
    transacao.setIdUsuarioDestino(idUsuario);
    transacao.setValor(valor); // Certifique-se de que o valor recebido está sendo usado aqui
    transacao.setGatewayPagamento(GatewayPagamento.INTERNO); // Define como transação interna
    transacao.setMetodoConexao(MetodoConexao.INTERNET); // Define como transação síncrona

    return transacaoRepository.save(transacao);
}
}