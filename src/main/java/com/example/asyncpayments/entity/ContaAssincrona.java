package com.example.asyncpayments.entity;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
public class ContaAssincrona extends Conta {

    public ContaAssincrona(Double saldo, User user) {
        super(saldo, user);
    }

    @Override
    public TipoConta getTipoConta() {
        return TipoConta.ASSINCRONA;
    }
}