package com.example.asyncpayments.api;

import com.example.asyncpayments.dto.RegisterRequest;
import com.example.asyncpayments.dto.UserDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fluxoUsuarioMe() throws Exception {
        String random = String.valueOf(System.nanoTime());
        String email = "me" + random + "@mail.com";
        String cpf = random.substring(0, 11);

        RegisterRequest req = new RegisterRequest(email, "123456", cpf, "Nome", "Sobrenome", "11999999999", "USER", true);
        String registerResponse = mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(registerResponse).get("token").asText();

        mockMvc.perform(get("/usuarios/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        String novoNome = "NovoNome";
        UserDTO update = new UserDTO();
        update.setEmail(email);
        update.setCpf(cpf);
        update.setNome(novoNome);
        update.setSobrenome("Sobrenome");
        update.setCelular("11999999999");
        mockMvc.perform(put("/usuarios/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nome").value(novoNome));

        mockMvc.perform(delete("/usuarios/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void cadastroEConsultaUsuarioBasico() throws Exception {
        String random = String.valueOf(System.nanoTime());
        String email = "user" + random + "@mail.com";
        String cpf = random.substring(0, 11);

        RegisterRequest req = new RegisterRequest(email, "123456", cpf, "Nome", "Sobrenome", "11999999999", "USER", true);
        String registerResponse = mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(registerResponse).get("token").asText();

        mockMvc.perform(get("/usuarios/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        mockMvc.perform(delete("/usuarios/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void consultarUsuarioNaoAutenticado_deveRetornar401() throws Exception {
        mockMvc.perform(get("/usuarios/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void atualizarUsuarioComCpfRepetido_deveRetornarErro() throws Exception {
        String random = String.valueOf(System.nanoTime());
        String email1 = "altuser1" + random + "@mail.com";
        String cpf = random.substring(0, 11);
        String email2 = "altuser2" + random + "@mail.com";

        RegisterRequest req1 = new RegisterRequest(email1, "123456", cpf, "Nome", "Sobrenome", "11999999999", "USER", true);
        String token1 = objectMapper.readTree(
                mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                        .andReturn().getResponse().getContentAsString()
        ).get("token").asText();

        RegisterRequest req2 = new RegisterRequest(email2, "123456", cpf, "Nome", "Sobrenome", "11999999998", "USER", true);
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/usuarios/me").header("Authorization", "Bearer " + token1)).andExpect(status().isOk());
    }
}