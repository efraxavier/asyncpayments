package com.example.asyncpayments.service;

import com.example.asyncpayments.dto.AuthRequest;
import com.example.asyncpayments.dto.AuthResponse;
import com.example.asyncpayments.dto.RegisterRequest;
import com.example.asyncpayments.entity.User;
import com.example.asyncpayments.entity.UserRole;
import com.example.asyncpayments.repository.UserRepository;
import com.example.asyncpayments.util.UserFakerUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
        throw new IllegalArgumentException("E-mail já cadastrado.");
    }
    if (userRepository.findByCpf(request.cpf()).isPresent()) {
        throw new IllegalArgumentException("CPF já cadastrado.");
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

        var jwtToken = jwtService.generateToken(user, user.getId());
        return new AuthResponse(jwtToken);
    }


    public AuthResponse login(AuthRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );


        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));


        var jwtToken = jwtService.generateToken(user, user.getId());
        return new AuthResponse(jwtToken);
    }
}