package com.example.asyncpayments.service;

import com.example.asyncpayments.entity.User;
import com.example.asyncpayments.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "testsecret1234567890");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 100000L);
    }

    @Test
    void testGenerateAndValidateToken() {
        UserDetails user = User.builder().email("a@b.com").password("pw").role(UserRole.USER).build();
        String token = jwtService.generateToken(user, 1L);

        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token, user));
        assertEquals("a@b.com", jwtService.extractUsername(token));
        assertEquals(1L, jwtService.extractUserId(token));
    }

    @Test
    void testInvalidToken() {
        UserDetails user = User.builder().email("a@b.com").password("pw").role(UserRole.USER).build();
        assertFalse(jwtService.isTokenValid("invalid.token", user));
    }
}