package com.example.asyncpayments.dto;

import com.example.asyncpayments.entity.ContaSincrona;
import com.example.asyncpayments.entity.ContaAssincrona;
import com.example.asyncpayments.entity.UserRole;
import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String email;
    private UserRole role;
    private ContaSincrona contaSincrona;
    private ContaAssincrona contaAssincrona;
}