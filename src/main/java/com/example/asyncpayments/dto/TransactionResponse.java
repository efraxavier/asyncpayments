package com.example.asyncpayments.dto;

import java.time.OffsetDateTime;

import lombok.Data;

@Data
public class TransactionResponse {
    private Long id;
    private Double valor;
    private String tipoOperacao;
    private String metodoConexao;
    private String gatewayPagamento;
    private String descricao;
    private OffsetDateTime dataCriacao;
    private OffsetDateTime dataAtualizacao;
    private boolean sincronizada;
    private String status;
    private String nomeUsuarioOrigem;
    private String emailUsuarioOrigem;
    private String cpfUsuarioOrigem;
    private String nomeUsuarioDestino;
    private String emailUsuarioDestino;
    private String cpfUsuarioDestino;
    private OffsetDateTime dataSincronizacaoOrigem;
    private OffsetDateTime dataSincronizacaoDestino;
}
