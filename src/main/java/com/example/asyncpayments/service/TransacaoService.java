package com.example.asyncpayments.service;

import com.example.asyncpayments.entity.BlockchainRegistro;
import com.example.asyncpayments.entity.ContaAssincrona;
import com.example.asyncpayments.entity.ContaSincrona;
import com.example.asyncpayments.entity.MetodoConexao;
import com.example.asyncpayments.entity.Transacao;
import com.example.asyncpayments.entity.GatewayPagamento;
import com.example.asyncpayments.repository.BlockchainRegistroRepository;
import com.example.asyncpayments.repository.ContaAssincronaRepository;
import com.example.asyncpayments.repository.ContaSincronaRepository;
import com.example.asyncpayments.repository.TransacaoRepository;
import com.example.asyncpayments.repository.UserRepository;
import com.example.asyncpayments.entity.User;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

/**
 * Serviço responsável pelas operações de transação entre contas.
 * 
 * Regras de Negócio e Requisitos implementados:
 * - RN03: Limite diário de R$ 1.000 para transferências entre contas do mesmo usuário.
 * - RN05: Limite de R$ 500 por transação offline.
 * - RN08: Notificação ao BACEN para transações acima de R$10.000.
 * - RN09: Validação KYC para transações acima de R$500.
 * - RF01/RF04: Transferências entre contas síncrona e assíncrona do mesmo usuário.
 * - RF02: Integração com gateways online e offline.
 */
@Service
@RequiredArgsConstructor
public class TransacaoService {

    private static final Logger logger = LoggerFactory.getLogger(TransacaoService.class);

    private final UserRepository userRepository;
    private final TransacaoRepository transacaoRepository;
    private final ContaAssincronaRepository contaAssincronaRepository;
    private final ContaSincronaRepository contaSincronaRepository;
    private final BlockchainRegistroRepository blockchainRegistroRepository;
    /**
     * Realiza uma transação entre contas, considerando regras de negócio para cada tipo de operação.
     * 
     * @param idUsuarioOrigem ID do usuário de origem.
     * @param idUsuarioDestino ID do usuário de destino.
     * @param valor Valor da transação.
     * @param gatewayPagamento Gateway de pagamento utilizado.
     * @param metodoConexao Método de conexão utilizado.
     * @param descricao Descrição da transação.
     * @return Transação realizada.
     */
    @Transactional
    public Transacao realizarTransacao(Long idUsuarioOrigem, Long idUsuarioDestino, Double valor, GatewayPagamento gatewayPagamento, MetodoConexao metodoConexao, String descricao) {
        validarParametrosTransacao(idUsuarioOrigem, idUsuarioDestino, valor);

        if (valor != null && valor > 10000.0) {
            logger.warn("NOTIFICAÇÃO BACEN: Transação acima de R$10.000 detectada. Origem: {}, Destino: {}, Valor: {}. Notificação será enviada ao BACEN.",
                idUsuarioOrigem, idUsuarioDestino, valor);
        }

        if (metodoConexao == MetodoConexao.INTERNET || metodoConexao == MetodoConexao.ASYNC) {
            if (!(gatewayPagamento == GatewayPagamento.STRIPE ||
                  gatewayPagamento == GatewayPagamento.PAGARME ||
                  gatewayPagamento == GatewayPagamento.MERCADO_PAGO ||
                  gatewayPagamento == GatewayPagamento.INTERNO)) {
                throw new IllegalArgumentException("Para transações via INTERNET ou ASYNC, só são permitidos os gateways: STRIPE, PAGARME, MERCADO_PAGO e INTERNO.");
            }
            if (gatewayPagamento == GatewayPagamento.INTERNO && idUsuarioOrigem.equals(idUsuarioDestino)) {
                // Transferência interna: debita da conta síncrona e credita na assíncrona
                ContaSincrona contaSincrona = contaSincronaRepository.findByUserId(idUsuarioOrigem);
                ContaAssincrona contaAssincrona = contaAssincronaRepository.findByUserId(idUsuarioDestino);

                if (contaSincrona == null || contaAssincrona == null) {
                    throw new IllegalArgumentException("Conta não encontrada para o usuário.");
                }
                if (contaSincrona.getSaldo() == null || contaSincrona.getSaldo() < valor) {
                    throw new IllegalArgumentException("Saldo insuficiente na conta síncrona.");
                }

                logger.info("Antes da transação: saldo sincrona={}, saldo assincrona={}", contaSincrona.getSaldo(), contaAssincrona.getSaldo());

                contaSincrona.setSaldo(contaSincrona.getSaldo() - valor);
                contaAssincrona.setSaldo(contaAssincrona.getSaldo() + valor);

                contaSincronaRepository.save(contaSincrona);
                contaAssincronaRepository.save(contaAssincrona);

                logger.info("Depois da transação: saldo sincrona={}, saldo assincrona={}", contaSincrona.getSaldo(), contaAssincrona.getSaldo());
            } else {
                // Transferência entre contas síncronas
                realizarTransacaoSincrona(idUsuarioOrigem, idUsuarioDestino, valor);
            }
        } else if (metodoConexao == MetodoConexao.SMS ||
                   metodoConexao == MetodoConexao.NFC ||
                   metodoConexao == MetodoConexao.BLUETOOTH) {
            if (!(gatewayPagamento == GatewayPagamento.DREX ||
                  gatewayPagamento == GatewayPagamento.PAGSEGURO ||
                  gatewayPagamento == GatewayPagamento.PAYCERTIFY)) {
                throw new IllegalArgumentException("Para transações offline (SMS, NFC, BLUETOOTH), só são permitidos os gateways: DREX, PAGSEGURO e PAYCERTIFY.");
            }
            realizarTransacaoAssincrona(idUsuarioOrigem, idUsuarioDestino, valor);

        } else {
            throw new IllegalArgumentException("Método de conexão inválido.");
        }

        Transacao transacao = new Transacao();
        transacao.setIdUsuarioOrigem(idUsuarioOrigem);
        transacao.setIdUsuarioDestino(idUsuarioDestino);
        transacao.setValor(valor);
        transacao.setGatewayPagamento(gatewayPagamento);
        transacao.setMetodoConexao(metodoConexao);
        transacao.setDescricao(descricao);
        transacao.setDataCriacao(OffsetDateTime.now(ZoneOffset.UTC));

        return transacaoRepository.save(transacao);
    }

    /**
     * Realiza uma transação síncrona (online) entre contas.
     * 
     * @param idUsuarioOrigem ID do usuário de origem.
     * @param idUsuarioDestino ID do usuário de destino.
     * @param valor Valor da transação.
     */
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

    /**
     * Realiza uma transação assíncrona (offline) entre contas.
     * 
     * Regras:
     * - RN05: Limite de R$ 500 por transação offline.
     * - RN09: Validação KYC para transações acima de R$500.
     * - RN08: Notificação ao BACEN para transações acima de R$10.000.
     */
    private void realizarTransacaoAssincrona(Long idUsuarioOrigem, Long idUsuarioDestino, Double valor) {

        if (valor > 500.0) {
            throw new IllegalArgumentException("Limite de R$500 por transação offline.");
        }


        if (valor > 500.0) {
            User userOrigem = userRepository.findById(idUsuarioOrigem)
                .orElseThrow(() -> new IllegalArgumentException("Usuário de origem não encontrado."));
            if (!userOrigem.isKycValidado()) {
                throw new IllegalStateException("Usuário precisa passar pelo KYC para transações acima de R$500.");
            }
        }


        if (valor != null && valor > 10000.0) {
            logger.warn("NOTIFICAÇÃO BACEN: Transação offline acima de R$10.000 detectada. Origem: {}, Destino: {}, Valor: {}. Notificação será enviada ao BACEN.",
                idUsuarioOrigem, idUsuarioDestino, valor);
        }

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

        registrarTransacaoOfflineEmBlockchain(idUsuarioOrigem, idUsuarioDestino, valor);
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
     * RN03: Limite diário de R$ 1.000 para transferências entre contas do mesmo usuário.
     * Permite transferir saldo da conta síncrona para a assíncrona do mesmo usuário,
     * respeitando o limite diário.
     */
    @Transactional
    public Transacao transferirSincronaParaAssincrona(Long idUsuario, Double valor) {
        if (valor == null || valor <= 0) {
            throw new IllegalArgumentException("O valor da transação deve ser maior que zero.");
        }

        double totalHoje = getTotalTransferidoHoje(idUsuario);
        if (totalHoje + valor > 1000.0) {
            throw new IllegalArgumentException("Limite diário de R$1.000 para transferências entre contas atingido.");
        }

        ContaSincrona contaSincrona = contaSincronaRepository.findByUserId(idUsuario);
        if (contaSincrona == null) {
            throw new IllegalArgumentException("Conta síncrona não encontrada para o usuário.");
        }

        if (contaSincrona.getSaldo() < valor) {
            throw new IllegalArgumentException("Saldo insuficiente na conta síncrona.");
        }

        ContaAssincrona contaAssincrona = contaAssincronaRepository.findByUserId(idUsuario);
        if (contaAssincrona == null) {
            throw new IllegalArgumentException("Conta assíncrona não encontrada para o usuário.");
        }

        if (contaAssincrona.isBloqueada()) {
            throw new IllegalStateException("A conta assíncrona está bloqueada. Sincronize a conta para desbloqueá-la.");
        }

        contaSincrona.setSaldo(contaSincrona.getSaldo() - valor);
        contaAssincrona.setSaldo(contaAssincrona.getSaldo() + valor);

        contaSincronaRepository.save(contaSincrona);
        contaAssincronaRepository.save(contaAssincrona);

        Transacao transacao = new Transacao();
        transacao.setIdUsuarioOrigem(idUsuario);
        transacao.setIdUsuarioDestino(idUsuario);
        transacao.setValor(valor);
        transacao.setGatewayPagamento(GatewayPagamento.INTERNO);
        transacao.setMetodoConexao(MetodoConexao.ASYNC); // ou INTERNET, conforme o caso
        transacao.setDataCriacao(OffsetDateTime.now(ZoneOffset.UTC));

        return transacaoRepository.save(transacao);
    }

    /**
     * Calcula o total transferido hoje entre contas do mesmo usuário (para RN03).
     */
    private double getTotalTransferidoHoje(Long idUsuario) {
        OffsetDateTime inicioDia = OffsetDateTime.now(ZoneOffset.UTC).toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime fimDia = inicioDia.plusDays(1);

        return transacaoRepository.findByIdUsuarioOrigem(idUsuario).stream()
            .filter(t -> 
                (t.getMetodoConexao() == MetodoConexao.INTERNET || t.getMetodoConexao() == MetodoConexao.ASYNC)
                && t.getGatewayPagamento() == GatewayPagamento.INTERNO)
            .filter(t -> t.getDataCriacao() != null && !t.getDataCriacao().isBefore(inicioDia) && t.getDataCriacao().isBefore(fimDia))
            .mapToDouble(Transacao::getValor)
            .sum();
    }

    /**
     * Lista todas as transações.
     */
    public List<Transacao> listarTodasTransacoes() {
        return transacaoRepository.findAll();
    }

    /**
     * Sincroniza a conta assíncrona com a conta síncrona correspondente.
     * Atualiza saldos e desbloqueia a conta assíncrona.
     */
    @Transactional
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

    private void registrarTransacaoOfflineEmBlockchain(Long idUsuarioOrigem, Long idUsuarioDestino, Double valor) {
        String hash = gerarHashSimples(idUsuarioOrigem, idUsuarioDestino, valor, OffsetDateTime.now(ZoneOffset.UTC));
        BlockchainRegistro registro = BlockchainRegistro.builder()
                .idUsuarioOrigem(idUsuarioOrigem)
                .idUsuarioDestino(idUsuarioDestino)
                .valor(valor)
                .dataRegistro(OffsetDateTime.now(ZoneOffset.UTC))
                .hashTransacao(hash)
                .build();
        blockchainRegistroRepository.save(registro);
    }

    private String gerarHashSimples(Long origem, Long destino, Double valor, OffsetDateTime data) {
        String raw = origem + "-" + destino + "-" + valor + "-" + data.toEpochSecond();
        return Integer.toHexString(raw.hashCode());
    }

    /**
     * Busca uma transação pelo ID.
     */
    public Optional<Transacao> buscarTransacaoPorId(Long id) {
        return transacaoRepository.findById(id);
    }

    /**
     * Verifica se uma transação existe pelo ID.
     */
    public boolean existeTransacao(Long id) {
        return transacaoRepository.existsById(id);
    }

    /**
     * Deleta uma transação pelo ID.
     */
    public void deletarTransacao(Long id) {
        transacaoRepository.deleteById(id);
    }

    /**
     * Processa uma transação offline, realizando as operações necessárias de débito e crédito.
     * 
     * @param idTransacao ID da transação a ser processada.
     * @param dataRecebida Data e hora em que a transação foi recebida (para verificar prazo de 72h).
     * @return Transação processada.
     */
    @Transactional
    public Transacao processarTransacaoOffline(Long idTransacao, OffsetDateTime dataRecebida) {
        Transacao transacao = transacaoRepository.findById(idTransacao)
            .orElseThrow(() -> new IllegalArgumentException("Transação não encontrada."));

        if (transacao.isSincronizada()) {
            throw new IllegalStateException("Transação já processada.");
        }


        OffsetDateTime dataEnvio = transacao.getDataCriacao();
        if (dataEnvio != null && dataRecebida != null) {
            long horas = java.time.Duration.between(dataEnvio, dataRecebida).toHours();
            if (horas > 72) {
                transacao.setSincronizada(false);
                transacao.setDataAtualizacao(OffsetDateTime.now(ZoneOffset.UTC));
                transacaoRepository.save(transacao);
                throw new IllegalStateException("Transação offline negada: enviada após 72h.");
            }
        }


        realizarTransacaoAssincrona(
            transacao.getIdUsuarioOrigem(),
            transacao.getIdUsuarioDestino(),
            transacao.getValor()
        );

        transacao.setSincronizada(true);
        transacao.setDataAtualizacao(OffsetDateTime.now(ZoneOffset.UTC));
        return transacaoRepository.save(transacao);
    }
}