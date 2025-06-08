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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
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
                carregarContas(user); // Carrega contas se necessário
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

        validarEmailECpf(userDTO, id); // Validação centralizada
        atualizarDadosUsuario(user, userDTO);
        userRepository.save(user);
        return ResponseEntity.ok(new ApiResponse<>(toDTO(user), null));
    }

    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ApiResponse<UserDTO>> atualizarMe(Authentication authentication, @RequestBody UserDTO userDTO) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        validarEmailECpf(userDTO, user.getId()); // Validação centralizada
        atualizarDadosUsuario(user, userDTO);
        userRepository.save(user);
        return ResponseEntity.ok(new ApiResponse<>(toDTO(user), null));
    }

@DeleteMapping("/me")
@PreAuthorize("hasAnyRole('USER','ADMIN')")
public ResponseEntity<ApiResponse<Void>> excluirMe(Authentication authentication) {
    String email = authentication.getName();
    return userRepository.findByEmail(email).map(user -> {
        userRepository.delete(user);
        return ResponseEntity.<ApiResponse<Void>>ok(new ApiResponse<>(null, "Usuário excluído com sucesso."));
    }).orElse(ResponseEntity.<ApiResponse<Void>>status(HttpStatus.NOT_FOUND)
            .body(new ApiResponse<>(null, "Usuário não encontrado.")));
}


    private void validarEmailECpf(UserDTO userDTO, Long userId) {
        if (userRepository.findByEmail(userDTO.getEmail()).filter(u -> !u.getId().equals(userId)).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado.");
        }
        if (userRepository.findByCpf(userDTO.getCpf()).filter(u -> !u.getId().equals(userId)).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF já cadastrado.");
        }
    }

    private void atualizarDadosUsuario(User user, UserDTO userDTO) {
        user.setEmail(userDTO.getEmail());
        user.setCpf(userDTO.getCpf());
        user.setNome(userDTO.getNome());
        user.setSobrenome(userDTO.getSobrenome());
        user.setCelular(userDTO.getCelular());
    }

    private void carregarContas(User user) {
        if (user.getContaSincrona() != null) user.getContaSincrona().getSaldo();
        if (user.getContaAssincrona() != null) user.getContaAssincrona().getSaldo();
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