package com.example.asyncpayments.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import com.example.asyncpayments.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.http.MediaType;

@SpringBootTest
@AutoConfigureMockMvc
class SincronizacaoControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fluxoSincronizacaoAdmin() throws Exception {
        String random = String.valueOf(System.nanoTime());
        String adminEmail = "admin" + random + "@mail.com";
        String adminCpf = random.substring(0, 11);
        RegisterRequest adminReq = new RegisterRequest(adminEmail, "123456", adminCpf, "Admin", "User", "11999999999", "ADMIN", true);
        String adminToken = objectMapper.readTree(
                mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminReq)))
                        .andReturn().getResponse().getContentAsString()
        ).get("token").asText();

        mockMvc.perform(post("/sincronizacao/manual")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/usuarios/me").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}