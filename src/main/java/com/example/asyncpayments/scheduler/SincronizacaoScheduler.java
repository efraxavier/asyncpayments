package com.example.asyncpayments.scheduler;

import com.example.asyncpayments.service.SincronizacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SincronizacaoScheduler {

    private final SincronizacaoService sincronizacaoService;


    @Scheduled(fixedRate = 60000)
    public void verificarEBloquearContas() {
        sincronizacaoService.verificarEBloquearContas();
    }
}
