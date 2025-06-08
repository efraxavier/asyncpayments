package com.example.asyncpayments.service;

import com.example.asyncpayments.entity.BlockchainRegistro;

public interface BlockchainService {
    void registrarTransacao(BlockchainRegistro registro);
    boolean validarTransacao(String hash);
}
