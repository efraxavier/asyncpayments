package com.example.asyncpayments.entity;

import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import static org.junit.jupiter.api.Assertions.*;

class TransacaoTest {

    @Test
    void testBuilderAndGetters() {
        Transacao t = new Transacao();
        t.setId(1L);
        t.setIdUsuarioOrigem(10L);
        t.setIdUsuarioDestino(20L);
        t.setValor(100.0);
        t.setTipoTransacao(TipoTransacao.SINCRONA);
        t.setMetodoConexao(MetodoConexao.INTERNET);
        t.setGatewayPagamento(GatewayPagamento.PAGARME);
        t.setSincronizada(true);

        OffsetDateTime now = OffsetDateTime.now();
        t.setDataCriacao(now);
        t.setDataAtualizacao(now);

        assertEquals(1L, t.getId());
        assertEquals(10L, t.getIdUsuarioOrigem());
        assertEquals(20L, t.getIdUsuarioDestino());
        assertEquals(100.0, t.getValor());
        assertEquals(TipoTransacao.SINCRONA, t.getTipoTransacao());
        assertEquals(MetodoConexao.INTERNET, t.getMetodoConexao());
        assertEquals(GatewayPagamento.PAGARME, t.getGatewayPagamento());
        assertTrue(t.isSincronizada());
        assertEquals(now, t.getDataCriacao());
        assertEquals(now, t.getDataAtualizacao());
    }

    @Test
    void testAllArgsConstructor() {
        OffsetDateTime now = OffsetDateTime.now();
        Transacao t = new Transacao(
                2L, 11L, 21L, 200.0,
                TipoTransacao.ASSINCRONA, MetodoConexao.SMS, GatewayPagamento.STRIPE,
                now, now, false
        );
        assertEquals(2L, t.getId());
        assertEquals(11L, t.getIdUsuarioOrigem());
        assertEquals(21L, t.getIdUsuarioDestino());
        assertEquals(200.0, t.getValor());
        assertEquals(TipoTransacao.ASSINCRONA, t.getTipoTransacao());
        assertEquals(MetodoConexao.SMS, t.getMetodoConexao());
        assertEquals(GatewayPagamento.STRIPE, t.getGatewayPagamento());
        assertEquals(now, t.getDataCriacao());
        assertEquals(now, t.getDataAtualizacao());
        assertFalse(t.isSincronizada());
    }

    @Test
    void testOnCreateAndOnUpdate() {
        Transacao t = new Transacao();
        t.onCreate();
        assertNotNull(t.getDataCriacao());
        assertNotNull(t.getDataAtualizacao());

        OffsetDateTime beforeUpdate = t.getDataAtualizacao();
        t.onUpdate();
        assertTrue(t.getDataAtualizacao().isAfter(beforeUpdate) || t.getDataAtualizacao().isEqual(beforeUpdate));
    }

    @Test
    void testEqualsAndHashCode() {
        Transacao t1 = new Transacao();
        t1.setId(1L);
        Transacao t2 = new Transacao();
        t2.setId(1L);
        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    void testToString() {
        Transacao t = new Transacao();
        t.setId(123L);
        String str = t.toString();
        assertTrue(str.contains("123"));
    }
}