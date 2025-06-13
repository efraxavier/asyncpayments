package com.example.asyncpayments.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransacaoAltoValor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long idTransacao;
    private Long idUsuarioOrigem;
    private Long idUsuarioDestino;
    private Double valor;
    private OffsetDateTime dataCriacao;
    private String descricao;
}