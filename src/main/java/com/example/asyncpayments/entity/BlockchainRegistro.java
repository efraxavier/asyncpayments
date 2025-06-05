package com.example.asyncpayments.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.*;
import lombok.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class BlockchainRegistro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long idUsuarioOrigem;
    private Long idUsuarioDestino;
    private Double valor;
    private OffsetDateTime dataRegistro;
    private String hashTransacao;
}
