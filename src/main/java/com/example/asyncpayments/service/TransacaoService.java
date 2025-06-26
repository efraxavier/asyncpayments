package com.example.asyncpayments.service;

import com.example.asyncpayments.dto.TransactionResponse;
import com.example.asyncpayments.entity.*;
import com.example.asyncpayments.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransacaoService {

    private static final Logger logger = LoggerFactory.getLogger(TransacaoService.class);

    private final UserRepository userRepository;
    private final TransacaoRepository transacaoRepository;
    private final ContaAssincronaRepository contaAssincronaRepository;
    private final ContaSincronaRepository contaSincronaRepository;
    private final BlockchainRegistroRepository blockchainRegistroRepository;
    private final BlockchainService blockchainService;
    private final FilaTransacaoService filaTransacaoService;
    private final SincronizacaoService sincronizacaoService;
    private final TransactionResponse transactionResponse;
    private final TransacaoAltoValorRepository transacaoAltoValorRepository;

    /**
     * Realiza uma transação entre contas, considerando regras de negócio para cada tipo de operação.
     */
    @Transactional
    public Transacao realizarTransacao(Long idUsuarioOrigem, Long idUsuarioDestino, Double valor, GatewayPagamento gatewayPagamento, MetodoConexao metodoConexao, String descricao) {
        logger.info("[TRANSACAO] Iniciando transação: origem={} destino={} valor={} gateway={} metodo={}", idUsuarioOrigem, idUsuarioDestino, valor, gatewayPagamento, metodoConexao);

        validarParametrosTransacao(idUsuarioOrigem, idUsuarioDestino, valor);

        if (valor <= 10000.0) {
            validarLimiteDiario(idUsuarioOrigem, valor);
        }

        ContaSincrona contaOrigem = contaSincronaRepository.findByUserId(idUsuarioOrigem);
        ContaSincrona contaDestino = contaSincronaRepository.findByUserId(idUsuarioDestino);

        if (contaOrigem == null || contaDestino == null) {
            logger.error("[VALIDACAO] Conta de origem ou destino não encontrada: origem={} destino={}", idUsuarioOrigem, idUsuarioDestino);
            throw new IllegalArgumentException("Conta de origem ou destino não encontrada.");
        }

        if (contaOrigem.getSaldo() < valor) {
            logger.warn("[VALIDACAO] Saldo insuficiente: usuario={} saldo={} valorTentativa={}", idUsuarioOrigem, contaOrigem.getSaldo(), valor);
            throw new IllegalArgumentException("Saldo insuficiente na conta de origem.");
        }

        // Log para KYC (exemplo de regra de negócio)
        User userOrigem = userRepository.findById(idUsuarioOrigem).orElse(null);
        if (valor > 500 && userOrigem != null && !userOrigem.isKycValidado()) {
            logger.warn("[VALIDACAO] KYC obrigatório não validado: usuario={} valor={}", idUsuarioOrigem, valor);
            throw new IllegalArgumentException("KYC obrigatório para transações acima de R$500.");
        }

        contaOrigem.setSaldo(contaOrigem.getSaldo() - valor);
        contaDestino.setSaldo(contaDestino.getSaldo() + valor);

        contaSincronaRepository.save(contaOrigem);
        contaSincronaRepository.save(contaDestino);

        logger.info("[TRANSACAO] Saldo atualizado: origem={} novoSaldo={} destino={} novoSaldo={}", idUsuarioOrigem, contaOrigem.getSaldo(), idUsuarioDestino, contaDestino.getSaldo());

        Transacao transacao = new Transacao();
        transacao.setIdUsuarioOrigem(idUsuarioOrigem);
        transacao.setIdUsuarioDestino(idUsuarioDestino);
        transacao.setValor(valor);
        transacao.setGatewayPagamento(gatewayPagamento);
        transacao.setMetodoConexao(metodoConexao);
        transacao.setDescricao(descricao);
        transacao.setDataCriacao(OffsetDateTime.now(ZoneOffset.UTC));

        Transacao transacaoSalva = transacaoRepository.save(transacao);

        logger.info("[BLOCKCHAIN] Registrando transação no blockchain: transacaoId={}", transacaoSalva.getId());
        BlockchainRegistro registro = BlockchainRegistro.builder()
                .idUsuarioOrigem(idUsuarioOrigem)
                .idUsuarioDestino(idUsuarioDestino)
                .valor(valor)
                .hashTransacao(gerarHashSimples(idUsuarioOrigem, idUsuarioDestino, valor))
                .build();
        blockchainService.registrarTransacao(registro);

        if (valor > 10000.0) {
            String info = String.format(
                "Notificando BACEN: Transação de alto valor detectada! Origem: %d, Destino: %d, Valor: %.2f, Descrição: %s, Data: %s",
                idUsuarioOrigem, idUsuarioDestino, valor, descricao, OffsetDateTime.now()
            );
            logger.warn(info);

            TransacaoAltoValor registroAltoValor = TransacaoAltoValor.builder()
                .idTransacao(transacaoSalva.getId())
                .idUsuarioOrigem(idUsuarioOrigem)
                .idUsuarioDestino(idUsuarioDestino)
                .valor(valor)
                .dataCriacao(OffsetDateTime.now())
                .descricao(descricao)
                .build();
            transacaoAltoValorRepository.save(registroAltoValor);
        }

        logger.info("[TRANSACAO] Transação concluída com sucesso: transacaoId={}", transacaoSalva.getId());
        return transacaoSalva;
    }

    private void validarParametrosTransacao(Long idUsuarioOrigem, Long idUsuarioDestino, Double valor) {
        if (idUsuarioOrigem == null || idUsuarioDestino == null) {
            logger.error("[VALIDACAO] IDs de usuário obrigatórios não informados: origem={} destino={}", idUsuarioOrigem, idUsuarioDestino);
            throw new IllegalArgumentException("Os IDs dos usuários de origem e destino são obrigatórios.");
        }
        if (valor == null || valor <= 0) {
            logger.error("[VALIDACAO] Valor inválido informado: valor={}", valor);
            throw new IllegalArgumentException("O valor da transação deve ser maior que zero.");
        }
    }

    private void validarLimiteDiario(Long idUsuarioOrigem, Double valor) {
        OffsetDateTime startDate = OffsetDateTime.now().minusDays(1);
        OffsetDateTime endDate = OffsetDateTime.now();
        List<Transacao> transacoes = transacaoRepository.findByIdUsuarioOrigemAndDataCriacaoBetween(idUsuarioOrigem, startDate, endDate);
        double totalDiario = transacoes.stream().mapToDouble(Transacao::getValor).sum();

        if (totalDiario + valor > 1000.0) {
            logger.warn("[VALIDACAO] Limite diário excedido para usuário {}: total={} valorTentativa={}", idUsuarioOrigem, totalDiario, valor);
            throw new IllegalStateException("Limite diário excedido");
        }
    }

    public List<Transacao> listarTodasTransacoes() {
        return transacaoRepository.findAll();
    }

    public Optional<Transacao> buscarTransacaoPorId(Long id) {
        return transacaoRepository.findById(id);
    }

    public boolean existeTransacao(Long id) {
        return transacaoRepository.existsById(id);
    }

    public void deletarTransacao(Long id) {
        transacaoRepository.deleteById(id);
    }

    public List<Transacao> listarTransacoesPorStatus(StatusTransacao status) {
        return transacaoRepository.findByStatus(status);
    }

    public TransactionResponse mapToTransactionResponse(Transacao transacao) {
        TransactionResponse response = new TransactionResponse();
        response.setId(transacao.getId());
        response.setValor(transacao.getValor());
        response.setTipoOperacao(transacao.getTipoOperacao() != null ? transacao.getTipoOperacao().name() : null);
        response.setMetodoConexao(transacao.getMetodoConexao() != null ? transacao.getMetodoConexao().name() : null);
        response.setGatewayPagamento(transacao.getGatewayPagamento() != null ? transacao.getGatewayPagamento().name() : null);
        response.setDescricao(transacao.getDescricao());
        response.setDataCriacao(transacao.getDataCriacao());
        response.setDataAtualizacao(transacao.getDataAtualizacao());
        response.setSincronizada(transacao.isSincronizada());
        response.setStatus(transacao.getStatus() != null ? transacao.getStatus().name() : null);
        response.setNomeUsuarioOrigem(transacao.getNomeUsuarioOrigem());
        response.setEmailUsuarioOrigem(transacao.getEmailUsuarioOrigem());
        response.setCpfUsuarioOrigem(transacao.getCpfUsuarioOrigem());
        response.setNomeUsuarioDestino(transacao.getNomeUsuarioDestino());
        response.setEmailUsuarioDestino(transacao.getEmailUsuarioDestino());
        response.setCpfUsuarioDestino(transacao.getCpfUsuarioDestino());
        response.setDataSincronizacaoOrigem(transacao.getDataSincronizacaoOrigem());
        response.setDataSincronizacaoDestino(transacao.getDataSincronizacaoDestino());
        return response;
    }

    @Transactional
    public Transacao criarTransacao(Long idUsuarioOrigem, Long idUsuarioDestino, Double valor, TipoOperacao tipoOperacao,
                               MetodoConexao metodoConexao, GatewayPagamento gatewayPagamento, String descricao) {
        validarParametrosTransacao(idUsuarioOrigem, idUsuarioDestino, valor);

        // Se for sincronização, processa corretamente!
        if (tipoOperacao == TipoOperacao.SINCRONIZACAO) {
            Transacao transacao = new Transacao();
            transacao.setIdUsuarioOrigem(idUsuarioOrigem);
            transacao.setIdUsuarioDestino(idUsuarioDestino);
            transacao.setValor(valor);
            transacao.setTipoOperacao(tipoOperacao);
            transacao.setMetodoConexao(metodoConexao);
            transacao.setGatewayPagamento(gatewayPagamento);
            transacao.setDescricao(descricao);
            transacao.setDataCriacao(OffsetDateTime.now(ZoneOffset.UTC));

            // Salva como pendente e processa sincronização
            transacao.setStatus(StatusTransacao.PENDENTE);
            transacaoRepository.save(transacao);

            return processarSincronizacao(transacao, OffsetDateTime.now(ZoneOffset.UTC));
        }

        if (tipoOperacao == TipoOperacao.INTERNA) {
            
            ContaSincrona contaSincrona = contaSincronaRepository.findByUserId(idUsuarioOrigem);
            ContaAssincrona contaAssincrona = contaAssincronaRepository.findByUserId(idUsuarioOrigem);
            if (contaSincrona == null || contaAssincrona == null) throw new IllegalArgumentException("Conta não encontrada.");
            if (contaSincrona.getSaldo() < valor) throw new IllegalArgumentException("Saldo insuficiente.");
            contaSincrona.setSaldo(contaSincrona.getSaldo() - valor);
            contaAssincrona.setSaldo(contaAssincrona.getSaldo() + valor);
            contaSincronaRepository.save(contaSincrona);
            contaAssincronaRepository.save(contaAssincrona);
        } else if (metodoConexao == MetodoConexao.INTERNET) {
            
            ContaSincrona contaOrigem = contaSincronaRepository.findByUserId(idUsuarioOrigem);
            ContaSincrona contaDestino = contaSincronaRepository.findByUserId(idUsuarioDestino);
            if (contaOrigem == null || contaDestino == null) throw new IllegalArgumentException("Conta não encontrada.");
            if (contaOrigem.getSaldo() < valor) throw new IllegalArgumentException("Saldo insuficiente.");
            contaOrigem.setSaldo(contaOrigem.getSaldo() - valor);
            contaDestino.setSaldo(contaDestino.getSaldo() + valor);
            contaSincronaRepository.save(contaOrigem);
            contaSincronaRepository.save(contaDestino);
        } else if (metodoConexao == MetodoConexao.SMS || metodoConexao == MetodoConexao.NFC || metodoConexao == MetodoConexao.BLUETOOTH) {
            
            ContaAssincrona contaOrigem = contaAssincronaRepository.findByUserId(idUsuarioOrigem);
            ContaAssincrona contaDestino = contaAssincronaRepository.findByUserId(idUsuarioDestino);
            if (contaOrigem == null || contaDestino == null) throw new IllegalArgumentException("Conta não encontrada.");
            if (contaOrigem.getSaldo() < valor) throw new IllegalArgumentException("Saldo insuficiente.");
            contaOrigem.setSaldo(contaOrigem.getSaldo() - valor);
            contaDestino.setSaldo(contaDestino.getSaldo() + valor);
            contaAssincronaRepository.save(contaOrigem);
            contaAssincronaRepository.save(contaDestino);
        } else if (metodoConexao == MetodoConexao.ASYNC && gatewayPagamento == GatewayPagamento.INTERNO) {
            
            ContaSincrona contaSincrona = contaSincronaRepository.findByUserId(idUsuarioOrigem);
            ContaAssincrona contaAssincrona = contaAssincronaRepository.findByUserId(idUsuarioOrigem);
            if (contaSincrona == null || contaAssincrona == null) throw new IllegalArgumentException("Conta não encontrada.");
            if (contaSincrona.getSaldo() < valor) throw new IllegalArgumentException("Saldo insuficiente.");
            contaSincrona.setSaldo(contaSincrona.getSaldo() - valor);
            contaAssincrona.setSaldo(contaAssincrona.getSaldo() + valor);
            contaSincronaRepository.save(contaSincrona);
            contaAssincronaRepository.save(contaAssincrona);
        } else {
            throw new IllegalArgumentException("Fluxo de transação não suportado.");
        }

        
        User origem = userRepository.findById(idUsuarioOrigem)
            .orElseThrow(() -> new IllegalArgumentException("Usuário de origem não encontrado."));
        User destino = userRepository.findById(idUsuarioDestino)
            .orElseThrow(() -> new IllegalArgumentException("Usuário de destino não encontrado."));

        Transacao transacao = new Transacao();
        transacao.setIdUsuarioOrigem(idUsuarioOrigem);
        transacao.setIdUsuarioDestino(idUsuarioDestino);
        transacao.setValor(valor);
        transacao.setTipoOperacao(tipoOperacao);
        transacao.setMetodoConexao(metodoConexao);
        transacao.setGatewayPagamento(gatewayPagamento);
        transacao.setDescricao(descricao);
        transacao.setDataCriacao(OffsetDateTime.now(ZoneOffset.UTC));
        
        transacao.setNomeUsuarioOrigem(origem.getNome());
        transacao.setEmailUsuarioOrigem(origem.getEmail());
        transacao.setCpfUsuarioOrigem(origem.getCpf());
        
        transacao.setNomeUsuarioDestino(destino.getNome());
        transacao.setEmailUsuarioDestino(destino.getEmail());
        transacao.setCpfUsuarioDestino(destino.getCpf());

        return transacaoRepository.save(transacao);
    }

    @Transactional
    public Transacao processarSincronizacao(Transacao transacao, OffsetDateTime dataRecebida) {
        ContaAssincrona contaAssincrona = contaAssincronaRepository.findByUserId(transacao.getIdUsuarioOrigem());
        ContaSincrona contaSincrona = contaSincronaRepository.findByUserId(transacao.getIdUsuarioOrigem());

        if (contaAssincrona == null || contaSincrona == null) {
            logger.error("[SINCRONIZACAO] Conta não encontrada para usuário {}", transacao.getIdUsuarioOrigem());
            throw new IllegalArgumentException("Conta não encontrada.");
        }

        if (transacao.getNomeUsuarioOrigem() == null || transacao.getEmailUsuarioOrigem() == null) {
            User origem = userRepository.findById(transacao.getIdUsuarioOrigem())
                .orElse(null);
            if (origem != null) {
                transacao.setNomeUsuarioOrigem(origem.getNome());
                transacao.setEmailUsuarioOrigem(origem.getEmail());
                transacao.setCpfUsuarioOrigem(origem.getCpf());
            }
        }
        if (transacao.getNomeUsuarioDestino() == null || transacao.getEmailUsuarioDestino() == null) {
            User destino = userRepository.findById(transacao.getIdUsuarioDestino())
                .orElse(null);
            if (destino != null) {
                transacao.setNomeUsuarioDestino(destino.getNome());
                transacao.setEmailUsuarioDestino(destino.getEmail());
                transacao.setCpfUsuarioDestino(destino.getCpf());
            }
        }

        // Validação de prazo
        OffsetDateTime dataCriacao = transacao.getDataCriacao();
        if (dataCriacao != null && java.time.Duration.between(dataCriacao, dataRecebida).toHours() > 72) {
            logger.warn("[SINCRONIZACAO] Sincronização fora do prazo de 72h: usuario={} dataCriacao={} dataRecebida={}", transacao.getIdUsuarioOrigem(), dataCriacao, dataRecebida);
            transacao.setStatus(StatusTransacao.ROLLBACK);
            transacao.setDescricao("Sincronização não pode ser processada após 72h.");
            transacaoRepository.save(transacao);
            return transacao;
        }

        // Validação de valor
        double saldoAssincrona = contaAssincrona.getSaldo();
        if (saldoAssincrona <= 0) {
            logger.warn("[SINCRONIZACAO] Conta assíncrona já está zerada para usuário {}", transacao.getIdUsuarioOrigem());
            transacao.setStatus(StatusTransacao.ROLLBACK);
            transacao.setDescricao("Conta assíncrona já está zerada.");
            transacaoRepository.save(transacao);
            return transacao;
        }
        if (!saldoAssincronaEquals(transacao.getValor(), saldoAssincrona)) {
            logger.warn("[SINCRONIZACAO] Valor de sincronização não corresponde ao saldo da conta assíncrona: usuario={} valorTransacao={} saldoAssincrona={}", transacao.getIdUsuarioOrigem(), transacao.getValor(), saldoAssincrona);
            transacao.setStatus(StatusTransacao.ROLLBACK);
            transacao.setDescricao("Valor de sincronização não corresponde ao saldo da conta assíncrona.");
            transacaoRepository.save(transacao);
            return transacao;
        }

        // Realiza a sincronização
        contaSincrona.setSaldo(contaSincrona.getSaldo() + saldoAssincrona);
        contaAssincrona.setSaldo(0.0);
        contaSincronaRepository.save(contaSincrona);
        contaAssincronaRepository.save(contaAssincrona);

        transacao.setStatus(StatusTransacao.SINCRONIZADA);
        transacao.setDataAtualizacao(dataRecebida);
        transacaoRepository.save(transacao);

        logger.info("[SINCRONIZACAO] Sincronização concluída: usuario={} valor={}", transacao.getIdUsuarioOrigem(), saldoAssincrona);
        return transacao;
    }

    // Função auxiliar para comparar double com tolerância
    private boolean saldoAssincronaEquals(double valorTransacao, double saldoAssincrona) {
        return Math.abs(valorTransacao - saldoAssincrona) < 0.01;
    }

    private String gerarHashSimples(Long origem, Long destino, Double valor) {
        String raw = origem + "-" + destino + "-" + valor + "-" + System.currentTimeMillis();
        return Integer.toHexString(raw.hashCode());
    }

    public void sincronizarTransacoesOffline() {
        List<Transacao> pendentes = transacaoRepository.findByStatus(StatusTransacao.PENDENTE);
        for (Transacao transacao : pendentes) {
            if (transacao.getTipoOperacao() == TipoOperacao.ASSINCRONA) {
                transacao.setStatus(StatusTransacao.SINCRONIZADA);
                transacao.setDataAtualizacao(OffsetDateTime.now(ZoneOffset.UTC));
                transacaoRepository.save(transacao);
            }
        }
    }

    public List<Transacao> listarTransacoesEnviadas(String email) {
        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return List.of();
        }
        Long userId = userOpt.get().getId();
        return transacaoRepository.findAll().stream()
                .filter(t -> t.getIdUsuarioOrigem().equals(userId))
                .toList();
    }

    /**
     * Processa uma transação offline (assincrona), validando prazo e registrando no blockchain se necessário.
     */
    public void processarTransacaoOffline(long transacaoId, OffsetDateTime dataProcessamento) {
        Optional<Transacao> transacaoOpt = transacaoRepository.findById(transacaoId);
        if (transacaoOpt.isEmpty()) {
            logger.error("[VALIDACAO] Transação não encontrada: transacaoId={}", transacaoId);
            throw new IllegalArgumentException("Transação não encontrada.");
        }
        Transacao transacao = transacaoOpt.get();

        OffsetDateTime dataCriacao = transacao.getDataCriacao();
        if (dataCriacao != null && java.time.Duration.between(dataCriacao, dataProcessamento).toHours() > 72) {
            logger.warn("[VALIDACAO] Transação fora do prazo de 72h: transacaoId={} dataCriacao={} dataProcessamento={}", transacao.getId(), dataCriacao, dataProcessamento);
            transacao.setStatus(StatusTransacao.ROLLBACK);
            transacao.setDescricao("Transação não pode ser processada após 72h.");
            transacaoRepository.save(transacao);

            ContaSincrona contaSincrona = contaSincronaRepository.findByUserId(transacao.getIdUsuarioOrigem());
            if (contaSincrona != null) {
                contaSincrona.setSaldo(contaSincrona.getSaldo() + transacao.getValor());
                contaSincronaRepository.save(contaSincrona);
            }

            throw new IllegalStateException("Transação não pode ser processada após 72h.");
        }

        ContaAssincrona contaOrigem = contaAssincronaRepository.findByUserId(transacao.getIdUsuarioOrigem());
        ContaAssincrona contaDestino = contaAssincronaRepository.findByUserId(transacao.getIdUsuarioDestino());
        if (contaOrigem == null || contaDestino == null) {
            logger.error("[VALIDACAO] Conta assíncrona de origem ou destino não encontrada: origem={} destino={}", transacao.getIdUsuarioOrigem(), transacao.getIdUsuarioDestino());
            throw new IllegalArgumentException("Conta assíncrona de origem ou destino não encontrada.");
        }
        if (contaOrigem.getSaldo() < transacao.getValor()) {
            logger.warn("[VALIDACAO] Saldo insuficiente na conta assíncrona: usuario={} saldo={} valorTentativa={}", transacao.getIdUsuarioOrigem(), contaOrigem.getSaldo(), transacao.getValor());
            throw new IllegalStateException("Saldo insuficiente na conta assíncrona de origem.");
        }
        contaOrigem.setSaldo(contaOrigem.getSaldo() - transacao.getValor());
        contaDestino.setSaldo(contaDestino.getSaldo() + transacao.getValor());
        contaAssincronaRepository.save(contaOrigem);
        contaAssincronaRepository.save(contaDestino);

        transacao.setStatus(StatusTransacao.SINCRONIZADA);
        transacao.setDataAtualizacao(dataProcessamento);
        transacaoRepository.save(transacao);

        logger.info("[BLOCKCHAIN] Registrando transação offline no blockchain: transacaoId={}", transacao.getId());
        BlockchainRegistro registro = BlockchainRegistro.builder()
                .idUsuarioOrigem(transacao.getIdUsuarioOrigem())
                .idUsuarioDestino(transacao.getIdUsuarioDestino())
                .valor(transacao.getValor())
                .hashTransacao(gerarHashSimples(transacao.getIdUsuarioOrigem(), transacao.getIdUsuarioDestino(), transacao.getValor()))
                .build();
        blockchainService.registrarTransacao(registro);
    }

    /**
     * Realiza uma transação assíncrona (offline), validando limites e regras.
     */
    public void realizarTransacaoAssincrona(long idUsuarioOrigem, long idUsuarioDestino, double valor) {
        if (valor > 500.0) {
            logger.warn("[VALIDACAO] Valor acima do permitido para transação offline: usuario={} valor={}", idUsuarioOrigem, valor);
            throw new IllegalArgumentException("Limite de R$500 por transação offline");
        }
        if (valor <= 0) {
            logger.error("[VALIDACAO] Valor inválido informado para transação offline: valor={}", valor);
            throw new IllegalArgumentException("O valor da transação deve ser maior que zero.");
        }
        ContaAssincrona contaOrigem = contaAssincronaRepository.findByUserId(idUsuarioOrigem);
        ContaAssincrona contaDestino = contaAssincronaRepository.findByUserId(idUsuarioDestino);
        if (contaOrigem == null || contaDestino == null) {
            logger.error("[VALIDACAO] Conta assíncrona de origem ou destino não encontrada: origem={} destino={}", idUsuarioOrigem, idUsuarioDestino);
            throw new IllegalArgumentException("Conta assíncrona de origem ou destino não encontrada.");
        }
        if (contaOrigem.getSaldo() < valor) {
            logger.warn("[VALIDACAO] Saldo insuficiente na conta assíncrona: usuario={} saldo={} valorTentativa={}", idUsuarioOrigem, contaOrigem.getSaldo(), valor);
            throw new IllegalStateException("Saldo insuficiente na conta assíncrona de origem.");
        }

        contaOrigem.setSaldo(contaOrigem.getSaldo() - valor);
        contaDestino.setSaldo(contaDestino.getSaldo() + valor);
        contaAssincronaRepository.save(contaOrigem);
        contaAssincronaRepository.save(contaDestino);

        Transacao transacao = new Transacao();
        transacao.setIdUsuarioOrigem(idUsuarioOrigem);
        transacao.setIdUsuarioDestino(idUsuarioDestino);
        transacao.setValor(valor);
        transacao.setTipoOperacao(TipoOperacao.ASSINCRONA);
        transacao.setMetodoConexao(MetodoConexao.SMS);
        transacao.setGatewayPagamento(GatewayPagamento.DREX);
        transacao.setStatus(StatusTransacao.PENDENTE);
        transacao.setDataCriacao(OffsetDateTime.now(ZoneOffset.UTC));
        transacaoRepository.save(transacao);

        logger.info("[BLOCKCHAIN] Registrando transação assíncrona no blockchain: origem={} destino={} valor={}", idUsuarioOrigem, idUsuarioDestino, valor);
        BlockchainRegistro registro = BlockchainRegistro.builder()
                .idUsuarioOrigem(idUsuarioOrigem)
                .idUsuarioDestino(idUsuarioDestino)
                .valor(valor)
                .hashTransacao(gerarHashSimples(idUsuarioOrigem, idUsuarioDestino, valor))
                .build();
        blockchainService.registrarTransacao(registro);
    }

    public Transacao transferirSincronaParaAssincrona(Long userId, Double valor) {
        ContaSincrona contaSincrona = contaSincronaRepository.findByUserId(userId);
        ContaAssincrona contaAssincrona = contaAssincronaRepository.findByUserId(userId);

        if (contaSincrona == null || contaAssincrona == null) {
            throw new IllegalArgumentException("Conta não encontrada.");
        }
        if (valor == null || valor <= 0) {
            throw new IllegalArgumentException("Valor inválido.");
        }
        if (contaSincrona.getSaldo() < valor) {
            throw new IllegalStateException("Saldo insuficiente na conta síncrona.");
        }

        contaSincrona.setSaldo(contaSincrona.getSaldo() - valor);
        contaAssincrona.setSaldo(contaAssincrona.getSaldo() + valor);

        contaSincronaRepository.save(contaSincrona);
        contaAssincronaRepository.save(contaAssincrona);

        Transacao transacao = new Transacao();
        transacao.setIdUsuarioOrigem(userId);
        transacao.setIdUsuarioDestino(userId);
        transacao.setValor(valor);
        transacao.setTipoOperacao(TipoOperacao.ASSINCRONA);
        transacao.setStatus(StatusTransacao.SINCRONIZADA);
        transacao.setDataCriacao(java.time.OffsetDateTime.now());

        return transacaoRepository.save(transacao);
    }

    public List<Transacao> buscarTransacoesFiltradas(
            Long id,
            Long idUsuarioOrigem,
            Long idUsuarioDestino,
            Double valor,
            TipoOperacao tipoOperacao,
            MetodoConexao metodoConexao,
            GatewayPagamento gatewayPagamento,
            StatusTransacao status,
            String descricao,
            String nomeUsuarioOrigem,
            String emailUsuarioOrigem,
            String cpfUsuarioOrigem,
            String nomeUsuarioDestino,
            String emailUsuarioDestino,
            String cpfUsuarioDestino,
            String dataCriacaoInicio,
            String dataCriacaoFim,
            String dataAtualizacaoInicio,
            String dataAtualizacaoFim
    ) {
        List<Transacao> transacoes = listarTodasTransacoes();
        return transacoes.stream()
            .filter(t -> id == null || t.getId().equals(id))
            .filter(t -> idUsuarioOrigem == null || t.getIdUsuarioOrigem().equals(idUsuarioOrigem))
            .filter(t -> idUsuarioDestino == null || t.getIdUsuarioDestino().equals(idUsuarioDestino))
            .filter(t -> valor == null || t.getValor().equals(valor))
            .filter(t -> tipoOperacao == null || t.getTipoOperacao() == tipoOperacao)
            .filter(t -> metodoConexao == null || t.getMetodoConexao() == metodoConexao)
            .filter(t -> gatewayPagamento == null || t.getGatewayPagamento() == gatewayPagamento)
            .filter(t -> status == null || t.getStatus() == status)
            .filter(t -> descricao == null || (t.getDescricao() != null && t.getDescricao().toLowerCase().contains(descricao.toLowerCase())))
            .filter(t -> nomeUsuarioOrigem == null || (t.getNomeUsuarioOrigem() != null && t.getNomeUsuarioOrigem().toLowerCase().contains(nomeUsuarioOrigem.toLowerCase())))
            .filter(t -> emailUsuarioOrigem == null || (t.getEmailUsuarioOrigem() != null && t.getEmailUsuarioOrigem().toLowerCase().contains(emailUsuarioOrigem.toLowerCase())))
            .filter(t -> cpfUsuarioOrigem == null || (t.getCpfUsuarioOrigem() != null && t.getCpfUsuarioOrigem().contains(cpfUsuarioOrigem)))
            .filter(t -> nomeUsuarioDestino == null || (t.getNomeUsuarioDestino() != null && t.getNomeUsuarioDestino().toLowerCase().contains(nomeUsuarioDestino.toLowerCase())))
            .filter(t -> emailUsuarioDestino == null || (t.getEmailUsuarioDestino() != null && t.getEmailUsuarioDestino().toLowerCase().contains(emailUsuarioDestino.toLowerCase())))
            .filter(t -> cpfUsuarioDestino == null || (t.getCpfUsuarioDestino() != null && t.getCpfUsuarioDestino().contains(cpfUsuarioDestino)))
            .filter(t -> {
                if (dataCriacaoInicio == null && dataCriacaoFim == null) return true;
                OffsetDateTime inicio = null, fim = null;
                try {
                    if (dataCriacaoInicio != null) inicio = OffsetDateTime.parse(dataCriacaoInicio);
                    if (dataCriacaoFim != null) fim = OffsetDateTime.parse(dataCriacaoFim);
                } catch (DateTimeParseException e) { return false; }
                if (inicio != null && t.getDataCriacao() != null && t.getDataCriacao().isBefore(inicio)) return false;
                if (fim != null && t.getDataCriacao() != null && t.getDataCriacao().isAfter(fim)) return false;
                return true;
            })
            .filter(t -> {
                if (dataAtualizacaoInicio == null && dataAtualizacaoFim == null) return true;
                OffsetDateTime inicio = null, fim = null;
                try {
                    if (dataAtualizacaoInicio != null) inicio = OffsetDateTime.parse(dataAtualizacaoInicio);
                    if (dataAtualizacaoFim != null) fim = OffsetDateTime.parse(dataAtualizacaoFim);
                } catch (DateTimeParseException e) { return false; }
                if (inicio != null && t.getDataAtualizacao() != null && t.getDataAtualizacao().isBefore(inicio)) return false;
                if (fim != null && t.getDataAtualizacao() != null && t.getDataAtualizacao().isAfter(fim)) return false;
                return true;
            })
            .toList();
    }

    public void atualizarStatusTransacao(Long id, StatusTransacao novoStatus) {
        Optional<Transacao> transacaoOpt = transacaoRepository.findById(id);
        if (transacaoOpt.isPresent()) {
            Transacao transacao = transacaoOpt.get();
            transacao.setStatus(novoStatus);
            transacao.setDataAtualizacao(OffsetDateTime.now(ZoneOffset.UTC));
            transacaoRepository.save(transacao);
        } else {
            throw new IllegalArgumentException("Transação não encontrada para atualização de status.");
        }
    }
}