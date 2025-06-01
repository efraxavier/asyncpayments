package com.example.asyncpayments.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserRoleTest {
    @Test
    void testValues() {
        assertEquals(UserRole.ADMIN, UserRole.valueOf("ADMIN"));
        assertEquals(UserRole.USER, UserRole.valueOf("USER"));
    }
}