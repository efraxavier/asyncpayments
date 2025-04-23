package com.example.asyncpayments.repository;

import com.example.asyncpayments.entity.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {
}