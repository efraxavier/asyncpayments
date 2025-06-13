package com.example.asyncpayments.repository;

import com.example.asyncpayments.entity.StatusTransacao;
import com.example.asyncpayments.entity.TipoOperacao;
import com.example.asyncpayments.entity.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface TransacaoRepository extends JpaRepository<Transacao, Long> {
    List<Transacao> findByIdUsuarioOrigem(Long idUsuarioOrigem);

    List<Transacao> findByIdUsuarioOrigemAndDataCriacaoBetween(Long idUsuarioOrigem, OffsetDateTime startDate, OffsetDateTime endDate);

    List<Transacao> findByTipoOperacao(TipoOperacao tipoOperacao);

    List<Transacao> findByStatus(StatusTransacao status);
}