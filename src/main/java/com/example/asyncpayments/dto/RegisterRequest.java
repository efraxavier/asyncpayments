package com.example.asyncpayments.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank String password,
    String cpf,
    @JsonProperty("nome") String nome,
    String sobrenome,
    String celular,
    String role,
    boolean consentimentoDados
) {}