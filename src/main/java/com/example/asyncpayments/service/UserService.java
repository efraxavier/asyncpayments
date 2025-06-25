package com.example.asyncpayments.service;

import com.example.asyncpayments.entity.ContaAssincrona;
import com.example.asyncpayments.entity.ContaSincrona;
import com.example.asyncpayments.entity.User;
import com.example.asyncpayments.entity.UserRole;
import com.example.asyncpayments.repository.ContaAssincronaRepository;
import com.example.asyncpayments.repository.ContaSincronaRepository;
import com.example.asyncpayments.repository.UserRepository;
import com.example.asyncpayments.util.AnonimizationUtil;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final ContaSincronaRepository contaSincronaRepository;
    private final ContaAssincronaRepository contaAssincronaRepository;

    public User criarUsuario(
            String email,
            String senha,
            String cpf,
            String nome,
            String sobrenome,
            String celular,
            UserRole role,
            boolean consentimentoDados
    ) {
        logger.info("[USER] Criando usuário: email={} cpf={}", email, cpf);

        boolean kycValidado = email != null && !email.isBlank()
                && senha != null && !senha.isBlank()
                && cpf != null && !cpf.isBlank()
                && nome != null && !nome.isBlank()
                && sobrenome != null && !sobrenome.isBlank()
                && celular != null && !celular.isBlank();

        
        User usuario = User.builder()
                .email(email)
                .password(senha)
                .cpf(cpf)
                .nome(nome)
                .sobrenome(sobrenome)
                .celular(celular)
                .role(role)
                .kycValidado(kycValidado)
                .consentimentoDados(consentimentoDados)
                .build();

        
        usuario = userRepository.save(usuario);

        
        ContaSincrona contaSincrona = new ContaSincrona(100.0, usuario);
        contaSincrona = contaSincronaRepository.save(contaSincrona);
        usuario.setContaSincrona(contaSincrona);

        
        ContaAssincrona contaAssincrona = new ContaAssincrona(0.0, usuario);
        contaAssincrona = contaAssincronaRepository.save(contaAssincrona);
        usuario.setContaAssincrona(contaAssincrona);

        
        logger.info("[USER] Usuário criado com sucesso: userId={}", usuario.getId());
        return usuario;
    }

    public long countUsers() {
        return userRepository.count();
    }
}