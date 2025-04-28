package com.example.asyncpayments.controller;

import com.example.asyncpayments.dto.UserDTO;
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
public ResponseEntity<List<UserDTO>> listarUsuariosComContas() {
    List<User> usuarios = userRepository.findAll();
    List<UserDTO> usuariosDTO = usuarios.stream().map(user -> {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setContaSincrona(user.getContaSincrona());
        dto.setContaAssincrona(user.getContaAssincrona());
        return dto;
    }).toList();
    return ResponseEntity.ok(usuariosDTO);
}
}