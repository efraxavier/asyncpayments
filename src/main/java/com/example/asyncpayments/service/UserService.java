package com.example.asyncpayments.service;

import com.example.asyncpayments.entity.Conta;
import com.example.asyncpayments.entity.TipoConta;
import com.example.asyncpayments.entity.User;
import com.example.asyncpayments.entity.UserRole;
import com.example.asyncpayments.repository.ContaRepository;
import com.example.asyncpayments.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ContaRepository contaRepository;

    public void criarUsuario(String email, String password, UserRole role) {
        // Lógica para criar o usuário
        var usuario = userRepository.save(new User(email, password, role));

        // Criar conta síncrona automaticamente com saldo inicial de 100.0
        Conta contaSincrona = new Conta();
        contaSincrona.setIdUsuario(usuario.getId());
        contaSincrona.setSaldo(100.0);
        contaSincrona.setTipoConta(TipoConta.SINCRONA);
        contaRepository.save(contaSincrona);
    }
}