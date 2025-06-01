package com.example.asyncpayments;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@AutoConfigureMockMvc
class AsyncpaymentsApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void main_deveExecutarSemErros() {
        AsyncpaymentsApplication.main(new String[]{});
    }
}