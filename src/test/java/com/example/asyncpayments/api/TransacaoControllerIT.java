package com.example.asyncpayments.api;

import com.example.asyncpayments.dto.RegisterRequest;
import com.example.asyncpayments.dto.TransacaoRequest;
import com.example.asyncpayments.entity.GatewayPagamento;
import com.example.asyncpayments.entity.MetodoConexao;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TransacaoControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;


@Test
void fluxoTransacaoEntreUsuarios() throws Exception {
    String random1 = String.valueOf(System.nanoTime());
    String emailOrigem = "origem" + random1 + "@mail.com";
    String cpfOrigem = random1.substring(0, 11);
    RegisterRequest reqOrigem = new RegisterRequest(emailOrigem, "123456", cpfOrigem, "Origem", "User", "11999990001", "USER", true);
    String tokenOrigem = objectMapper.readTree(
            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(reqOrigem)))
                    .andReturn().getResponse().getContentAsString()
    ).get("token").asText();

    String random2 = String.valueOf(System.nanoTime());
    String emailDestino = "destino" + random2 + "@mail.com";
    String cpfDestino = random2.substring(0, 11);
    RegisterRequest reqDestino = new RegisterRequest(emailDestino, "123456", cpfDestino, "Destino", "User", "11999990002", "USER", true);
    String tokenDestino = objectMapper.readTree(
            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(reqDestino)))
                    .andReturn().getResponse().getContentAsString()
    ).get("token").asText();

    Long idOrigem = objectMapper.readTree(
            mockMvc.perform(get("/usuarios/me").header("Authorization", "Bearer " + tokenOrigem))
                    .andReturn().getResponse().getContentAsString()
    ).get("id").asLong();
    Long idDestino = objectMapper.readTree(
            mockMvc.perform(get("/usuarios/me").header("Authorization", "Bearer " + tokenDestino))
                    .andReturn().getResponse().getContentAsString()
    ).get("id").asLong();

    
    String payload = """
        {
          "idUsuarioOrigem": %d,
          "idUsuarioDestino": %d,
          "valor": 1.0,
          "gatewayPagamento": "PAGARME",
          "metodoConexao": "INTERNET",
          "tipoOperacao": "SINCRONA"
        }
        """.formatted(idOrigem, idDestino);

    mockMvc.perform(post("/transacoes")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload)
            .header("Authorization", "Bearer " + tokenOrigem))
            .andExpect(status().isOk());

    mockMvc.perform(delete("/usuarios/me").header("Authorization", "Bearer " + tokenOrigem)).andExpect(status().isOk());
    mockMvc.perform(delete("/usuarios/me").header("Authorization", "Bearer " + tokenDestino)).andExpect(status().isOk());
}

@Test
void realizarTransacao_limiteDiarioExcedido_deveRetornarErro() throws Exception {
    
    String random = String.valueOf(System.nanoTime());
    String email = "user" + random + "@mail.com";
    String cpf = random.substring(0, 11);
    RegisterRequest req = new RegisterRequest(email, "123456", cpf, "Nome", "Sobrenome", "11999999999", "USER", true);
    String token = objectMapper.readTree(
        mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andReturn().getResponse().getContentAsString()
    ).get("token").asText();

    String payload = """
        {
          "idUsuarioOrigem": 1,
          "idUsuarioDestino": 2,
          "valor": 1500.0,
          "gatewayPagamento": "PAGARME",
          "metodoConexao": "INTERNET",
          "tipoOperacao": "SINCRONA"
        }
        """;

    mockMvc.perform(post("/transacoes")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload)
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Limite diário excedido"));
}

@Test
void realizarTransacaoOffline_valorAcimaDe500_deveRetornarErro() throws Exception {
    
    String random = String.valueOf(System.nanoTime());
    String email = "user" + random + "@mail.com";
    String cpf = random.substring(0, 11);
    RegisterRequest req = new RegisterRequest(email, "123456", cpf, "Nome", "Sobrenome", "11999999999", "USER", true);
    String token = objectMapper.readTree(
        mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andReturn().getResponse().getContentAsString()
    ).get("token").asText();

    String payload = """
        {
          "idUsuarioOrigem": 1,
          "idUsuarioDestino": 2,
          "valor": 600.0,
          "gatewayPagamento": "PAGARME",
          "metodoConexao": "SMS",
          "tipoOperacao": "ASSINCRONA"
        }
        """;

    mockMvc.perform(post("/transacoes")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload)
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Limite de R$500 por transação offline"));
}

@Test
void realizarTransacao_valorAcimaDe10000_deveRetornarOk() throws Exception {
    
    String random = String.valueOf(System.nanoTime());
    String email = "user" + random + "@mail.com";
    String cpf = random.substring(0, 11);
    RegisterRequest req = new RegisterRequest(email, "123456", cpf, "Nome", "Sobrenome", "11999999999", "USER", true);
    String token = objectMapper.readTree(
        mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andReturn().getResponse().getContentAsString()
    ).get("token").asText();

    String payload = """
        {
          "idUsuarioOrigem": 1,
          "idUsuarioDestino": 2,
          "valor": 15000.0,
          "gatewayPagamento": "PAGARME",
          "metodoConexao": "INTERNET",
          "tipoOperacao": "SINCRONA"
        }
        """;

    mockMvc.perform(post("/transacoes")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload)
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
}

@Test
void fluxoBasicoTransacaoEntreUsuarios() throws Exception {
    String random1 = String.valueOf(System.nanoTime());
    String emailOrigem = "origem" + random1 + "@mail.com";
    String cpfOrigem = random1.substring(0, 11);
    RegisterRequest reqOrigem = new RegisterRequest(emailOrigem, "123456", cpfOrigem, "Origem", "User", "11999990001", "USER", true);
    String tokenOrigem = objectMapper.readTree(
            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(reqOrigem)))
                    .andReturn().getResponse().getContentAsString()
    ).get("token").asText();

    String random2 = String.valueOf(System.nanoTime());
    String emailDestino = "destino" + random2 + "@mail.com";
    String cpfDestino = random2.substring(0, 11);
    RegisterRequest reqDestino = new RegisterRequest(emailDestino, "123456", cpfDestino, "Destino", "User", "11999990002", "USER", true);
    String tokenDestino = objectMapper.readTree(
            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(reqDestino)))
                    .andReturn().getResponse().getContentAsString()
    ).get("token").asText();

    Long idOrigem = objectMapper.readTree(
            mockMvc.perform(get("/usuarios/me").header("Authorization", "Bearer " + tokenOrigem))
                    .andReturn().getResponse().getContentAsString()
    ).get("id").asLong();
    Long idDestino = objectMapper.readTree(
            mockMvc.perform(get("/usuarios/me").header("Authorization", "Bearer " + tokenDestino))
                    .andReturn().getResponse().getContentAsString()
    ).get("id").asLong();

    String payload = """
        {
          "idUsuarioOrigem": %d,
          "idUsuarioDestino": %d,
          "valor": 10.0,
          "gatewayPagamento": "PAGARME",
          "metodoConexao": "INTERNET",
          "tipoOperacao": "SINCRONA"
        }
        """.formatted(idOrigem, idDestino);

    mockMvc.perform(post("/transacoes")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload)
            .header("Authorization", "Bearer " + tokenOrigem))
            .andExpect(status().isOk());

    mockMvc.perform(delete("/usuarios/me").header("Authorization", "Bearer " + tokenOrigem)).andExpect(status().isOk());
    mockMvc.perform(delete("/usuarios/me").header("Authorization", "Bearer " + tokenDestino)).andExpect(status().isOk());
}

@Test
void criarTransacaoComTokenInvalido_deveRetornar401() throws Exception {
    String payload = """
        {
          "idUsuarioOrigem": 1,
          "idUsuarioDestino": 2,
          "valor": 10.0,
          "gatewayPagamento": "PAGARME",
          "metodoConexao": "INTERNET",
          "tipoOperacao": "SINCRONA"
        }
        """;
    mockMvc.perform(post("/transacoes")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload)
            .header("Authorization", "Bearer tokenInvalido"))
            .andExpect(status().isUnauthorized());
}

@Test
void criarTransacaoComDadosInvalidos_deveRetornar400() throws Exception {
    String random = String.valueOf(System.nanoTime());
    String email = "alttrans" + random + "@mail.com";
    String cpf = random.substring(0, 11);
    RegisterRequest req = new RegisterRequest(email, "123456", cpf, "Nome", "Sobrenome", "11999999999", "USER", true);
    String token = objectMapper.readTree(
        mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andReturn().getResponse().getContentAsString()
    ).get("token").asText();

    
    String payload = """
        {
          "idUsuarioOrigem": 1,
          "idUsuarioDestino": 2,
          "valor": -10.0,
          "gatewayPagamento": "PAGARME",
          "metodoConexao": "INTERNET",
          "tipoOperacao": "SINCRONA"
        }
        """;
    mockMvc.perform(post("/transacoes")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload)
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isBadRequest());
}
}