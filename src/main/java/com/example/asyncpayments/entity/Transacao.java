package com.example.asyncpayments.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

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
    @NotNull(message = "O tipo de transação é obrigatório.")
    private TipoTransacao tipoTransacao;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "O método de conexão é obrigatório.")
    private MetodoConexao metodoConexao;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "O gateway de pagamento é obrigatório.")
    private GatewayPagamento gatewayPagamento;

    @Column(length = 140)
    private String descricao;

    private OffsetDateTime dataCriacao;
    private OffsetDateTime dataAtualizacao;

    private boolean sincronizada;

    @PrePersist
    protected void onCreate() {
        this.dataCriacao = OffsetDateTime.now(ZoneOffset.UTC);
        this.dataAtualizacao = OffsetDateTime.now(ZoneOffset.UTC);
    }

    @PreUpdate
    protected void onUpdate() {
        this.dataAtualizacao = OffsetDateTime.now(ZoneOffset.UTC);
    }
}