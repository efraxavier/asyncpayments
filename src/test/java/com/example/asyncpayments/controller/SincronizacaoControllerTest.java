package com.example.asyncpayments.controller;

import com.example.asyncpayments.service.SincronizacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SincronizacaoControllerTest {

    @Mock
    private SincronizacaoService sincronizacaoService;

    @InjectMocks
    private SincronizacaoController sincronizacaoController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void sincronizarContasManual_deveRetornarOk() {
        ResponseEntity<?> response = sincronizacaoController.sincronizarContasManual();
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void sincronizarContaPorId_deveRetornarOk() {
        ResponseEntity<?> response = sincronizacaoController.sincronizarContaPorId(1L);
        assertEquals(200, response.getStatusCode().value());
    }
}