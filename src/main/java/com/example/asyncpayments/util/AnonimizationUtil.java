package com.example.asyncpayments.util;

public class AnonimizationUtil {

    public static String anonimizarCpfParcial(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            throw new IllegalArgumentException("CPF inválido para anonimização parcial.");
        }
        return cpf.substring(0, 3) + ".***.***-" + cpf.substring(9);
    }
}
