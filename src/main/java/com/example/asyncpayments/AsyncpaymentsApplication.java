package com.example.asyncpayments;

import com.example.asyncpayments.entity.Conta;
import com.example.asyncpayments.entity.TipoConta;
import com.example.asyncpayments.repository.ContaRepository;
import com.example.asyncpayments.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@RequiredArgsConstructor
public class AsyncpaymentsApplication implements CommandLineRunner {

    private final ContaRepository contaRepository;
    private final UserService userService;

    public static void main(String[] args) {
        SpringApplication.run(AsyncpaymentsApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // Inicializar contas síncronas para teste
        if (contaRepository.count() == 0) {
            contaRepository.save(new Conta(null, 1L, 100.0, TipoConta.SINCRONA));
            contaRepository.save(new Conta(null, 2L, 100.0, TipoConta.SINCRONA));
            System.out.println("Contas síncronas inicializadas com saldo de 100.0");
        }
    }
}
