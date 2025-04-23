package com.example.asyncpayments.repository;

import com.example.asyncpayments.entity.Conta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContaRepository extends JpaRepository<Conta, Long> {
    Conta findByIdUsuario(Long idUsuario);
}
