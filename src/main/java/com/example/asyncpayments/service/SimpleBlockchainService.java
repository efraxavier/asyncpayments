package com.example.asyncpayments.service;

import com.example.asyncpayments.entity.BlockchainRegistro;
import com.example.asyncpayments.repository.BlockchainRegistroRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SimpleBlockchainService implements BlockchainService {

    private static final Logger logger = LoggerFactory.getLogger(SimpleBlockchainService.class);
    private final BlockchainRegistroRepository blockchainRegistroRepository;

    @Override
    public void registrarTransacao(BlockchainRegistro registro) {
        blockchainRegistroRepository.save(registro);
        logger.info("Transação registrada no blockchain simulado: {}", registro.getHashTransacao());
    }

    @Override
    public boolean validarTransacao(String hash) {
        boolean exists = blockchainRegistroRepository.findAll().stream()
                .anyMatch(registro -> registro.getHashTransacao().equals(hash));
        logger.info("Validação de transação no blockchain simulado: hash={}, válido={}", hash, exists);
        return exists;
    }
}
