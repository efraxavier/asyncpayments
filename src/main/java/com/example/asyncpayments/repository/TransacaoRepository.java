package com.example.asyncpayments.repository;

import com.example.asyncpayments.entity.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {
    List<Transacao> findByIdUsuarioOrigem(Long idUsuarioOrigem);   
}