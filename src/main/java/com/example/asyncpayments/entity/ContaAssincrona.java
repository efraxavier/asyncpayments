package com.example.asyncpayments.entity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@EqualsAndHashCode(callSuper = false)
public class ContaAssincrona extends Conta {

    private boolean bloqueada;
    private OffsetDateTime ultimaSincronizacao;

    public ContaAssincrona(Double saldo, User user) {
        super(saldo, user);
    }

    @Override
    public TipoOperacao getTipoOperacao() {
        return TipoOperacao.ASSINCRONA;
    }

    @PrePersist
    protected void onCreate() {
        this.ultimaSincronizacao = OffsetDateTime.now(ZoneOffset.UTC);
        this.bloqueada = false;
    }

    @PreUpdate
    protected void onUpdate() {
        
        if (ultimaSincronizacao != null && ultimaSincronizacao.isBefore(OffsetDateTime.now(ZoneOffset.UTC).minusHours(72))) {
            this.bloqueada = true;
        }
    }



    public void bloquear() {
        this.bloqueada = true;
    }
}