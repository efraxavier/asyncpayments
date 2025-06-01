package com.example.asyncpayments.api;

import com.example.asyncpayments.dto.AuthRequest;
import com.example.asyncpayments.dto.RegisterRequest;
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
class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

   @Test
void fluxoCadastroELogin() throws Exception {
    String random = String.valueOf(System.nanoTime());
    String email = "user" + random + "@mail.com";
    String cpf = random.substring(0, 11);


    RegisterRequest req = new RegisterRequest(email, "123456", cpf, "Nome", "Sobrenome", "11999999999", "USER", true);
    String registerResponse = mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists())
            .andReturn().getResponse().getContentAsString();
    String token = objectMapper.readTree(registerResponse).get("token").asText();


    AuthRequest login = new AuthRequest(email, "123456");
    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists());


    mockMvc.perform(delete("/usuarios/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
}
}