package com.example.asyncpayments.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnonimizationUtilTest {

    @Test
    void anonimizarCpfParcial_deveAnonimizarCorretamente() {
        String cpf = "12345678901";
        String cpfAnonimizado = AnonimizationUtil.anonimizarCpfParcial(cpf);
        assertEquals("123.***.***-01", cpfAnonimizado);
    }

    @Test
    void anonimizarCpfParcial_cpfInvalido_deveLancarExcecao() {
        String cpf = "12345";
        assertThrows(IllegalArgumentException.class, () -> AnonimizationUtil.anonimizarCpfParcial(cpf));
    }
}
