package com.example.asyncpayments.util;

import com.example.asyncpayments.entity.User;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserFakerUtilTest {

    @Test
    void testGenerateFakeUser() {
        User user = UserFakerUtil.generateFakeUser();
        assertNotNull(user);
        assertNotNull(user.getEmail());
        assertNotNull(user.getPassword());
        assertNotNull(user.getNome());
        assertNotNull(user.getSobrenome());
        assertNotNull(user.getCpf());
        assertNotNull(user.getCelular());
    }

    @Test
    void testGenerateFakeCpf() {
        String cpf = UserFakerUtil.generateFakeCpf();
        assertNotNull(cpf);
        assertEquals(11, cpf.length());
    }

    @Test
    void testGenerateFakeEmail() {
        String email = UserFakerUtil.generateFakeEmail();
        assertNotNull(email);
        assertTrue(email.contains("@"));
    }
}