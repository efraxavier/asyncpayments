package com.example.asyncpayments.entity;

import jakarta.persistence.Entity;
import lombok.Data;

@Data
@Entity
public class ContaAssincrona extends Conta {

    public ContaAssincrona() {
        super();
        this.setTipoConta(TipoConta.ASSINCRONA); // Define o tipo como ASSINCRONA
        this.setSaldo(0.0); // Saldo inicial para contas assíncronas
    }

    public ContaAssincrona(Long id, Long idUsuario, Double saldo) {
        super(id, idUsuario, saldo, TipoConta.ASSINCRONA);
    }
}