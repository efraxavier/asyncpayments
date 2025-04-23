package com.example.asyncpayments.entity;

import lombok.Data;

import jakarta.persistence.*;

@Entity
@Data
public class Conta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long idUsuario;

    private Double saldo;

    @Enumerated(EnumType.STRING)
    private TipoConta tipoConta; // SINCRONA ou ASSINCRONA

    // Construtor padrão para contas síncronas
    public Conta() {
        this.saldo = 100.0; // Saldo inicial padrão para contas síncronas
        this.tipoConta = TipoConta.SINCRONA;
    }

    // Construtor completo
    public Conta(Long id, Long idUsuario, Double saldo, TipoConta tipoConta) {
        this.id = id;
        this.idUsuario = idUsuario;
        this.saldo = saldo;
        this.tipoConta = tipoConta;
    }

    public Conta(String someParam) {
        // Initialize fields or perform actions with someParam
    }

}