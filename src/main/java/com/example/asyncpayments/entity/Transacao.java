package com.example.asyncpayments.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "O ID do usuário de origem é obrigatório.")
    private Long idUsuarioOrigem;

    @NotNull(message = "O ID do usuário de destino é obrigatório.")
    private Long idUsuarioDestino;

    @NotNull(message = "O valor da transação é obrigatório.")
    @Positive(message = "O valor da transação deve ser positivo.")
    private Double valor;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "O tipo de operação é obrigatório.")
    private TipoOperacao tipoOperacao;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "O método de conexão é obrigatório.")
    private MetodoConexao metodoConexao;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "O gateway de pagamento é obrigatório.")
    private GatewayPagamento gatewayPagamento;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "O status da transação é obrigatório.")
    private StatusTransacao status;

    @Column(length = 140)
    private String descricao;

    private OffsetDateTime dataCriacao;
    private OffsetDateTime dataAtualizacao;

    private String nomeUsuarioOrigem;
    private String emailUsuarioOrigem;
    private String cpfUsuarioOrigem;

    private String nomeUsuarioDestino;
    private String emailUsuarioDestino;
    private String cpfUsuarioDestino;

    @PrePersist
    protected void onCreate() {
        this.dataCriacao = OffsetDateTime.now();
        this.dataAtualizacao = OffsetDateTime.now();
        this.status = StatusTransacao.PENDENTE; 
    }

    @PreUpdate
    protected void onUpdate() {
        this.dataAtualizacao = OffsetDateTime.now();
    }

    public boolean isSincronizada() {
        return this.status == StatusTransacao.SINCRONIZADA;
    }

    public OffsetDateTime getDataSincronizacaoOrigem() {
        return this.dataCriacao;
    }

    public OffsetDateTime getDataSincronizacaoDestino() {
        return this.dataAtualizacao;
    }

    public Transacao(Long id, Long idUsuarioOrigem, Long idUsuarioDestino, Double valor, TipoOperacao tipoOperacao,
                     MetodoConexao metodoConexao, GatewayPagamento gatewayPagamento, StatusTransacao status,
                     OffsetDateTime dataCriacao, OffsetDateTime dataAtualizacao, String descricao) {
        this.id = id;
        this.idUsuarioOrigem = idUsuarioOrigem;
        this.idUsuarioDestino = idUsuarioDestino;
        this.valor = valor;
        this.tipoOperacao = tipoOperacao;
        this.metodoConexao = metodoConexao;
        this.gatewayPagamento = gatewayPagamento;
        this.status = status;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.descricao = descricao;
    }
}