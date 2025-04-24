package com.example.asyncpayments.service;

import com.example.asyncpayments.entity.Conta;
import com.example.asyncpayments.entity.ContaAssincrona;
import com.example.asyncpayments.entity.ContaSincrona;
import com.example.asyncpayments.entity.TipoConta;
import com.example.asyncpayments.entity.User;
import com.example.asyncpayments.entity.UserRole;
import com.example.asyncpayments.repository.ContaAssincronaRepository;
import com.example.asyncpayments.repository.ContaSincronaRepository;
import com.example.asyncpayments.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ContaSincronaRepository contaSincronaRepository;
    private final ContaAssincronaRepository contaAssincronaRepository;

    public User criarUsuario(String email, String senha) {
        User usuario = new User(email, senha, UserRole.USER);
        usuario = userRepository.save(usuario);
    
        ContaSincrona contaSincrona = new ContaSincrona(100.0, usuario);
        contaSincronaRepository.save(contaSincrona);
    
        ContaAssincrona contaAssincrona = new ContaAssincrona(0.0, usuario);
        contaAssincronaRepository.save(contaAssincrona);
    
        return usuario;
    }

public long countUsers() {
    return userRepository.count();
}
}