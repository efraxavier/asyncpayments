package com.example.asyncpayments.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.time.OffsetDateTime;

class ContaAssincronaTest {

    @Test
    void testBuilderAndGetters() {
        User user = User.builder().email("a@b.com").build();
        ContaAssincrona conta = new ContaAssincrona(50.0, user);

        assertEquals(50.0, conta.getSaldo());
        assertEquals(user, conta.getUser());
        assertEquals(TipoConta.ASSINCRONA, conta.getTipoConta());
    }

    @Test
    void testSetters() {
        ContaAssincrona conta = new ContaAssincrona();
        conta.setSaldo(200.0);
        User user = new User();
        conta.setUser(user);
        conta.setBloqueada(true);
        OffsetDateTime now = OffsetDateTime.now();
        conta.setUltimaSincronizacao(now);

        assertEquals(200.0, conta.getSaldo());
        assertEquals(user, conta.getUser());
        assertTrue(conta.isBloqueada());
        assertEquals(now, conta.getUltimaSincronizacao());
    }

    @Test
    void testSincronizarEBloquear() {
        ContaAssincrona conta = new ContaAssincrona();
        conta.setBloqueada(true);
        conta.sincronizar();
        assertFalse(conta.isBloqueada());
        assertNotNull(conta.getUltimaSincronizacao());

        conta.bloquear();
        assertTrue(conta.isBloqueada());
    }

    @Test
    void testOnCreateAndOnUpdate() {
        ContaAssincrona conta = new ContaAssincrona();
        conta.onCreate();
        assertNotNull(conta.getUltimaSincronizacao());
        assertFalse(conta.isBloqueada());


        conta.setUltimaSincronizacao(OffsetDateTime.now().minusHours(73));
        conta.onUpdate();
        assertTrue(conta.isBloqueada());
    }

}