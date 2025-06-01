package com.example.asyncpayments.entity;

import java.time.OffsetDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
public class ContaAssincrona extends Conta {

    private boolean bloqueada;

    private OffsetDateTime ultimaSincronizacao;

    public ContaAssincrona(Double saldo, User user) {
        super(saldo, user);
    }

    @Override
    public TipoConta getTipoConta() {
        return TipoConta.ASSINCRONA;
    }

    @PrePersist
    protected void onCreate() {
        this.ultimaSincronizacao = OffsetDateTime.now(ZoneOffset.UTC);
        this.bloqueada = false;
    }

    @PreUpdate
    protected void onUpdate() {

        if (ultimaSincronizacao.isBefore(OffsetDateTime.now(ZoneOffset.UTC).minusHours(72))) {
            this.bloqueada = true;
        }
    }


    public void sincronizar() {
        this.ultimaSincronizacao = OffsetDateTime.now(ZoneOffset.UTC);
        this.bloqueada = false;
    }


    public void bloquear() {
        this.bloqueada = true;
    }
}