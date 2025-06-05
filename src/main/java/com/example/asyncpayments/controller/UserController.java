package com.example.asyncpayments.controller;

import com.example.asyncpayments.dto.ApiResponse;
import com.example.asyncpayments.dto.UserDTO;
import com.example.asyncpayments.entity.User;
import com.example.asyncpayments.exception.NotFoundException;
import com.example.asyncpayments.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;


    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> listarUsuarios() {
        List<UserDTO> usuariosDTO = userRepository.findAll().stream().map(this::toDTO).toList();
        return ResponseEntity.ok(usuariosDTO);
    }


    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<UserDTO> getMe(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
            .map(user -> {
                // Força o carregamento das contas (caso seja LAZY)
                if (user.getContaSincrona() != null) user.getContaSincrona().getSaldo();
                if (user.getContaAssincrona() != null) user.getContaAssincrona().getSaldo();
                return toDTO(user);
            })
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }


    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> buscarUsuarioPorId(@PathVariable Long id) {
        return userRepository.findById(id)
            .map(this::toDTO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }


    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDTO>> atualizarUsuario(@PathVariable Long id, @RequestBody UserDTO userDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));


        if (userRepository.findByEmail(userDTO.getEmail()).filter(u -> !u.getId().equals(id)).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiResponse<>(null, "E-mail já cadastrado."));
        }
        if (userRepository.findByCpf(userDTO.getCpf()).filter(u -> !u.getId().equals(id)).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiResponse<>(null, "CPF já cadastrado."));
        }
        user.setEmail(userDTO.getEmail());
        user.setCpf(userDTO.getCpf());
        user.setNome(userDTO.getNome());
        user.setSobrenome(userDTO.getSobrenome());
        user.setCelular(userDTO.getCelular());
        userRepository.save(user);
        return ResponseEntity.ok(new ApiResponse<>(toDTO(user), null));
    }


    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ApiResponse<UserDTO>> atualizarMe(Authentication authentication, @RequestBody UserDTO userDTO) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));


        if (userRepository.findByEmail(userDTO.getEmail()).filter(u -> !u.getId().equals(user.getId())).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiResponse<>(null, "E-mail já cadastrado."));
        }
        if (userRepository.findByCpf(userDTO.getCpf()).filter(u -> !u.getId().equals(user.getId())).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiResponse<>(null, "CPF já cadastrado."));
        }
        user.setEmail(userDTO.getEmail());
        user.setCpf(userDTO.getCpf());
        user.setNome(userDTO.getNome());
        user.setSobrenome(userDTO.getSobrenome());
        user.setCelular(userDTO.getCelular());
        userRepository.save(user);
        return ResponseEntity.ok(new ApiResponse<>(toDTO(user), null));
    }

@DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ApiResponse<Void>> excluirUsuario(@PathVariable Long id) {
    if (userRepository.existsById(id)) {
        userRepository.deleteById(id);
        return ResponseEntity.ok(new ApiResponse<Void>(null, "Usuário excluído com sucesso."));
    }
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ApiResponse<Void>(null, "Usuário não encontrado."));
}

@DeleteMapping("/me")
@PreAuthorize("hasAnyRole('USER','ADMIN')")
public ResponseEntity<ApiResponse<Void>> excluirMe(Authentication authentication) {
    String email = authentication.getName();
    return userRepository.findByEmail(email).map(user -> {
        userRepository.delete(user);
        return ResponseEntity.ok(new ApiResponse<Void>(null, "Usuário excluído com sucesso."));
    }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ApiResponse<Void>(null, "Usuário não encontrado.")));
}


    @PostMapping("/me/aceitar-kyc")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<?> aceitarKyc(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email).map(user -> {
            user.setKycValidado(true);
            userRepository.save(user);
            return ResponseEntity.ok("KYC aceito.");
        }).orElse(ResponseEntity.notFound().build());
    }


    @PostMapping("/me/anonimizar")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<?> anonimizar(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email).map(user -> {
            user.setNome(null);
            user.setSobrenome(null);
            user.setCelular(null);
            user.setCpf(null);
            user.setEmail(null);
            user.setConsentimentoDados(false);

            userRepository.save(user);
            return ResponseEntity.ok("Dados anonimizados.");
        }).orElse(ResponseEntity.notFound().build());
    }


    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setCpf(user.getCpf());
        dto.setNome(user.getNome());
        dto.setSobrenome(user.getSobrenome());
        dto.setCelular(user.getCelular());
        dto.setContaSincrona(user.getContaSincrona());
        dto.setContaAssincrona(user.getContaAssincrona());
        dto.setRole(user.getRole() != null ? user.getRole().name() : null);
        dto.setConsentimentoDados(user.getConsentimentoDados());
        return dto;
    }
}