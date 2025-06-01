package com.example.asyncpayments.repository;

import com.example.asyncpayments.entity.ContaAssincrona;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContaAssincronaRepository extends JpaRepository<ContaAssincrona, Long> {
    ContaAssincrona findByUserId(Long userId);
}