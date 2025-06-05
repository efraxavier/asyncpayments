package com.example.asyncpayments.util;

import com.example.asyncpayments.entity.User;
import com.example.asyncpayments.entity.UserRole;
import com.github.javafaker.Faker;


public class UserFakerUtil {
    private static final Faker faker = new Faker();

    public static String fakeCpf() {
        return faker.number().digits(11);
    }

    public static String fakeNome() {
        return faker.name().firstName();
    }

    public static String fakeSobrenome() {
        return faker.name().lastName();
    }

    public static String fakeCelular() {
        return faker.phoneNumber().cellPhone();
    }

    public static String generateFakeCpf() {
        return faker.number().digits(11);
    }

    public static String generateFakeEmail() {
        return faker.internet().emailAddress();
    }

    public static User generateFakeUser() {
        return User.builder()
                .email(generateFakeEmail())
                .password(faker.internet().password())
                .cpf(generateFakeCpf())
                .nome(fakeNome())
                .sobrenome(fakeSobrenome())
                .celular(fakeCelular())
                .role(UserRole.USER)
                .consentimentoDados(true)
                .kycValidado(false)
                .build();
    }
}