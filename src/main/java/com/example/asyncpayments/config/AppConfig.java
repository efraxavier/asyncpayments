package com.example.asyncpayments.config;

import com.example.asyncpayments.dto.TransactionResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public TransactionResponse transactionResponse() {
        return new TransactionResponse();
    }
}