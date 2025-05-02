package com.example.asyncpayments.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
public class ContaAssincrona extends Conta {

    private boolean bloqueada; // Indica se a conta está bloqueada

    private LocalDateTime ultimaSincronizacao; // Data/hora da última sincronização

    public ContaAssincrona(Double saldo, User user) {
        super(saldo, user);
    }

    @Override
    public TipoConta getTipoConta() {
        return TipoConta.ASSINCRONA;
    }

    @PrePersist
    protected void onCreate() {
        this.ultimaSincronizacao = LocalDateTime.now();
        this.bloqueada = false;
    }

    @PreUpdate
    protected void onUpdate() {
        // Verifica se a conta deve ser bloqueada antes de salvar
        if (ultimaSincronizacao.isBefore(LocalDateTime.now().minusMinutes(5))) {
            this.bloqueada = true;
        }
    }

    /**
     * Método para sincronizar a conta.
     * Atualiza a última sincronização e desbloqueia a conta.
     */
    public void sincronizar() {
        this.ultimaSincronizacao = LocalDateTime.now();
        this.bloqueada = false;
    }

    /**
     * Método para bloquear a conta.
     */
    public void bloquear() {
        this.bloqueada = true;
    }
}