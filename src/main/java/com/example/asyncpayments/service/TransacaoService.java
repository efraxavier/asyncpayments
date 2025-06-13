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
        validarParametrosTransacao(idUsuarioOrigem, idUsuarioDestino, valor);

        if (valor <= 10000.0) {
            validarLimiteDiario(idUsuarioOrigem, valor);
        }

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

        
        Transacao transacao = new Transacao();
        transacao.setIdUsuarioOrigem(idUsuarioOrigem);
        transacao.setIdUsuarioDestino(idUsuarioDestino);
        transacao.setValor(valor);
        transacao.setGatewayPagamento(gatewayPagamento);
        transacao.setMetodoConexao(metodoConexao);
        transacao.setDescricao(descricao);
        transacao.setDataCriacao(OffsetDateTime.now(ZoneOffset.UTC));

        Transacao transacaoSalva = transacaoRepository.save(transacao);

        
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

        return transacaoSalva;
    }

    private void validarParametrosTransacao(Long idUsuarioOrigem, Long idUsuarioDestino, Double valor) {
        if (idUsuarioOrigem == null || idUsuarioDestino == null) {
            throw new IllegalArgumentException("Os IDs dos usuários de origem e destino são obrigatórios.");
        }
        if (valor == null || valor <= 0) {
            throw new IllegalArgumentException("O valor da transação deve ser maior que zero.");
        }
    }

    private void validarLimiteDiario(Long idUsuarioOrigem, Double valor) {
        OffsetDateTime startDate = OffsetDateTime.now().minusDays(1);
        OffsetDateTime endDate = OffsetDateTime.now();

        List<Transacao> transacoes = transacaoRepository.findByIdUsuarioOrigemAndDataCriacaoBetween(idUsuarioOrigem, startDate, endDate);
        double totalDiario = transacoes.stream().mapToDouble(Transacao::getValor).sum();

        if (totalDiario + valor > 1000.0) {
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
    public void atualizarStatusTransacao(Long idTransacao, StatusTransacao novoStatus) {
        Transacao transacao = transacaoRepository.findById(idTransacao)
                .orElseThrow(() -> new IllegalArgumentException("Transação não encontrada."));
        transacao.setStatus(novoStatus);
        transacaoRepository.save(transacao);
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
            throw new IllegalArgumentException("Transação não encontrada.");
        }
        Transacao transacao = transacaoOpt.get();

        
        OffsetDateTime dataCriacao = transacao.getDataCriacao();
        if (dataCriacao != null && java.time.Duration.between(dataCriacao, dataProcessamento).toHours() > 72) {
            transacao.setStatus(StatusTransacao.ROLLBACK);
            transacao.setDescricao("Transação não pode ser processada após 72h.");
            transacaoRepository.save(transacao);
            throw new IllegalStateException("Transação não pode ser processada após 72h.");
        }

        
        ContaAssincrona contaOrigem = contaAssincronaRepository.findByUserId(transacao.getIdUsuarioOrigem());
        ContaAssincrona contaDestino = contaAssincronaRepository.findByUserId(transacao.getIdUsuarioDestino());
        if (contaOrigem == null || contaDestino == null) {
            throw new IllegalArgumentException("Conta assíncrona de origem ou destino não encontrada.");
        }
        if (contaOrigem.getSaldo() < transacao.getValor()) {
            throw new IllegalStateException("Saldo insuficiente na conta assíncrona de origem.");
        }
        contaOrigem.setSaldo(contaOrigem.getSaldo() - transacao.getValor());
        contaDestino.setSaldo(contaDestino.getSaldo() + transacao.getValor());
        contaAssincronaRepository.save(contaOrigem);
        contaAssincronaRepository.save(contaDestino);

        
        transacao.setStatus(StatusTransacao.SINCRONIZADA);
        transacao.setDataAtualizacao(dataProcessamento);
        transacaoRepository.save(transacao);

        
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
            throw new IllegalArgumentException("Limite de R$500 por transação offline");
        }
        if (valor <= 0) {
            throw new IllegalArgumentException("O valor da transação deve ser maior que zero.");
        }
        ContaAssincrona contaOrigem = contaAssincronaRepository.findByUserId(idUsuarioOrigem);
        ContaAssincrona contaDestino = contaAssincronaRepository.findByUserId(idUsuarioDestino);
        if (contaOrigem == null || contaDestino == null) {
            throw new IllegalArgumentException("Conta assíncrona de origem ou destino não encontrada.");
        }
        if (contaOrigem.getSaldo() < valor) {
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
}