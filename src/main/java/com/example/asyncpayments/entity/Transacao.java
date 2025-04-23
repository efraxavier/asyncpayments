package com.example.asyncpayments.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transacoes")
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double valor;

    @Enumerated(EnumType.STRING)
    private StatusTransacao status;

    @ManyToOne
    @JoinColumn(name = "conta_id", nullable = false)
    private Conta conta; // Agora `Conta` é uma entidade válida
}