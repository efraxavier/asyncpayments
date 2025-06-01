package com.example.asyncpayments.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

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
