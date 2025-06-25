package com.example.asyncpayments.service;

import com.example.asyncpayments.dto.AuthRequest;
import com.example.asyncpayments.dto.AuthResponse;
import com.example.asyncpayments.dto.RegisterRequest;
import com.example.asyncpayments.entity.User;
import com.example.asyncpayments.entity.UserRole;
import com.example.asyncpayments.repository.UserRepository;
import com.example.asyncpayments.util.UserFakerUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;

    public AuthResponse register(RegisterRequest request) {
        logger.info("[AUTH] Tentativa de registro: email={} cpf={}", request.email(), request.cpf());
        if (userRepository.findByEmail(request.email()).isPresent()) {
            logger.warn("[AUTH] Registro negado: e-mail já cadastrado ({})", request.email());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado.");
        }
        if (userRepository.findByCpf(request.cpf()).isPresent()) {
            logger.warn("[AUTH] Registro negado: CPF já cadastrado ({})", request.cpf());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF já cadastrado.");
        }
        System.out.println("RegisterRequest recebido: " + request);

        User user = userService.criarUsuario(
            request.email(),
            passwordEncoder.encode(request.password()),
            request.cpf() != null ? request.cpf() : UserFakerUtil.fakeCpf(),
            request.nome() != null ? request.nome() : UserFakerUtil.fakeNome(),
            request.sobrenome() != null ? request.sobrenome() : UserFakerUtil.fakeSobrenome(),
            request.celular() != null ? request.celular() : UserFakerUtil.fakeCelular(),
            UserRole.valueOf(request.role()),
            request.consentimentoDados()
        );

        logger.info("[AUTH] Registro realizado com sucesso: userId={}", user.getId());

        var jwtToken = jwtService.generateToken(user, user.getId());
        return new AuthResponse(jwtToken);
    }


    public AuthResponse login(AuthRequest request) {
        logger.info("[AUTH] Tentativa de login: email={}", request.email());
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.email(),
                    request.password()
                )
            );
        } catch (Exception e) {
            logger.warn("[AUTH] Falha de autenticação para email={}: {}", request.email(), e.getMessage());
            throw e;
        }


        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));


        logger.info("[AUTH] Login realizado com sucesso: userId={}", user.getId());

        var jwtToken = jwtService.generateToken(user, user.getId());
        return new AuthResponse(jwtToken);
    }
}