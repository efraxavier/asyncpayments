package com.example.asyncpayments;

import com.example.asyncpayments.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@RequiredArgsConstructor
public class AsyncpaymentsApplication implements CommandLineRunner {

    private final UserService userService;

    public static void main(String[] args) {
        SpringApplication.run(AsyncpaymentsApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // Inicializar usuários e suas contas para teste
        if (userService.countUsers() == 0) {
            userService.criarUsuario("user1@example.com", "password1");
            userService.criarUsuario("user2@example.com", "password2");
            System.out.println("Usuários e contas criados com sucesso!");
        }
    }
}
