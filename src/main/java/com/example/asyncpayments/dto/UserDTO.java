package com.example.asyncpayments.dto;

import com.example.asyncpayments.entity.ContaSincrona;
import com.example.asyncpayments.entity.ContaAssincrona;
import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String email;
    private String cpf;
    private String nome;
    private String sobrenome;
    private String celular;
    private String role;
    private ContaSincrona contaSincrona;
    private ContaAssincrona contaAssincrona;
    private Boolean consentimentoDados;
}