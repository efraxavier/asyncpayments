package com.example.asyncpayments.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ContaSincronaTest {

    @Test
    void testBuilderAndGetters() {
        User user = User.builder().email("a@b.com").build();
        ContaSincrona conta = new ContaSincrona(100.0, user);

        assertEquals(100.0, conta.getSaldo());
        assertEquals(user, conta.getUser());
        assertEquals(TipoConta.SINCRONA, conta.getTipoConta());
    }

    @Test
    void testSetters() {
        ContaSincrona conta = new ContaSincrona();
        conta.setSaldo(200.0);
        User user = new User();
        conta.setUser(user);

        assertEquals(200.0, conta.getSaldo());
        assertEquals(user, conta.getUser());
    }

    @Test
    void testTransferenciaEntreContas() {
        ContaSincrona contaOrigem = new ContaSincrona();
        contaOrigem.setSaldo(1000.0);

        ContaSincrona contaDestino = new ContaSincrona();
        contaDestino.setSaldo(500.0);


        contaOrigem.setSaldo(contaOrigem.getSaldo() - 200.0);
        contaDestino.setSaldo(contaDestino.getSaldo() + 200.0);

        assertEquals(800.0, contaOrigem.getSaldo());
        assertEquals(700.0, contaDestino.getSaldo());
    }
}