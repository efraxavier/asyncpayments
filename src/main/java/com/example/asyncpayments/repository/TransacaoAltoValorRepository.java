package com.example.asyncpayments.repository;

import com.example.asyncpayments.entity.TransacaoAltoValor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransacaoAltoValorRepository extends JpaRepository<TransacaoAltoValor, Long> {
}