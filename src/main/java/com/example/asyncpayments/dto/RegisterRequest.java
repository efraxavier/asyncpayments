package com.example.asyncpayments.dto;

import com.example.asyncpayments.entity.UserRole;

public record RegisterRequest(String email, String password, UserRole role) {}

