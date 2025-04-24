package com.example.asyncpayments.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "O ID do usuário de origem é obrigatório.")
    private Long idUsuarioOrigem;

    @NotNull(message = "O ID do usuário de destino é obrigatório.")
    private Long idUsuarioDestino;

    @NotNull(message = "O valor da transação é obrigatório.")
    @Positive(message = "O valor da transação deve ser positivo.")
    private Double valor;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "O tipo de transação é obrigatório.")
    private TipoTransacao tipoTransacao; // SINCRONA ou ASSINCRONA

    private String metodoConexao; // bluetooth, nfc, sms (apenas para assincronas)

    @NotNull(message = "O serviço de pagamento é obrigatório.")
    private String servicoPagamento; // PagarMe, Mercado Pago, Stripe

    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;

    private boolean sincronizada; // Indica se a transação foi sincronizada

    @PrePersist
    protected void onCreate() {
        this.dataCriacao = LocalDateTime.now();
        this.dataAtualizacao = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.dataAtualizacao = LocalDateTime.now();
    }
}