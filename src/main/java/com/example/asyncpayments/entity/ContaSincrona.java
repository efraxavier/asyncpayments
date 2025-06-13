package com.example.asyncpayments.entity;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@EqualsAndHashCode(callSuper = false)
public class ContaSincrona extends Conta {

    public ContaSincrona(Double saldo, User user) {
        super(saldo, user);
    }

    @Override
    public TipoOperacao getTipoOperacao() {
        return TipoOperacao.SINCRONA;
    }
}