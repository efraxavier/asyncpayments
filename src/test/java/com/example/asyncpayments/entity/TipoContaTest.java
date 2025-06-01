package com.example.asyncpayments.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TipoContaTest {
    @Test
    void testValues() {
        assertEquals(TipoConta.SINCRONA, TipoConta.valueOf("SINCRONA"));
        assertEquals(TipoConta.ASSINCRONA, TipoConta.valueOf("ASSINCRONA"));
    }
}