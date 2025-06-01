package com.example.asyncpayments.dto;

import com.example.asyncpayments.entity.GatewayPagamento;
import com.example.asyncpayments.entity.MetodoConexao;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransacaoRequest {
    @NotNull(message = "O ID do usuário de origem é obrigatório.")
    private Long idUsuarioOrigem;

    @NotNull(message = "O ID do usuário de destino é obrigatório.")
    private Long idUsuarioDestino;

    @NotNull(message = "O valor da transação é obrigatório.")
    @Positive(message = "O valor da transação deve ser positivo.")
    private Double valor;

    @NotNull(message = "O método de conexão é obrigatório.")
    private MetodoConexao metodoConexao;

    @NotNull(message = "O gateway de pagamento é obrigatório.")
    private GatewayPagamento gatewayPagamento;

    private String descricao; // <-- Novo campo
}