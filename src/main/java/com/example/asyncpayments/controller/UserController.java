package com.example.asyncpayments.controller;

import com.example.asyncpayments.entity.User;
import com.example.asyncpayments.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/listar")
    public ResponseEntity<List<User>> listarUsuariosComContas() {
        List<User> usuarios = userRepository.findAll();
        usuarios.forEach(user -> {
            user.getContaSincrona(); // Garante que a conta síncrona seja carregada
            user.getContaAssincrona(); // Garante que a conta assíncrona seja carregada
        });
        return ResponseEntity.ok(usuarios);
    }
}