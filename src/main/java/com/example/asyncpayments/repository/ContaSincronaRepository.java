package com.example.asyncpayments.repository;

import com.example.asyncpayments.entity.ContaSincrona;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContaSincronaRepository extends JpaRepository<ContaSincrona, Long> {
    ContaSincrona findByUserId(Long userId);
}