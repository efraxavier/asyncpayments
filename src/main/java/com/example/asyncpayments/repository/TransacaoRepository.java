package com.example.asyncpayments.repository;

import com.example.asyncpayments.entity.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface TransacaoRepository extends JpaRepository<Transacao, Long> {
    List<Transacao> findByIdUsuarioOrigem(Long idUsuarioOrigem);

    List<Transacao> findBySincronizadaFalse();   
    
    List<Transacao> findByIdUsuarioOrigemAndDataCriacaoBetween(Long idUsuarioOrigem, OffsetDateTime startDate, OffsetDateTime endDate);
}