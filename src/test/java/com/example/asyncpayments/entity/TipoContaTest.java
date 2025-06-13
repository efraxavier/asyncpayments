package com.example.asyncpayments.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TipoOperacaoTest {
    @Test
    void testValues() {
        assertEquals(TipoOperacao.SINCRONA, TipoOperacao.valueOf("SINCRONA"));
        assertEquals(TipoOperacao.ASSINCRONA, TipoOperacao.valueOf("ASSINCRONA"));
    }
}