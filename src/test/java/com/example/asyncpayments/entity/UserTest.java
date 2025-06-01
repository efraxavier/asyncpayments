package com.example.asyncpayments.entity;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testUserBuilderAndGetters() {
        User user = User.builder()
                .email("test@email.com")
                .password("senha")
                .cpf("12345678900")
                .nome("Test")
                .sobrenome("User")
                .celular("11999999999")
                .role(UserRole.USER)
                .consentimentoDados(true)
                .kycValidado(true)
                .build();

        assertEquals("test@email.com", user.getEmail());
        assertEquals("senha", user.getPassword());
        assertEquals("12345678900", user.getCpf());
        assertEquals("Test", user.getNome());
        assertEquals("User", user.getSobrenome());
        assertEquals("11999999999", user.getCelular());
        assertEquals(UserRole.USER, user.getRole());
        assertTrue(user.getConsentimentoDados());
        assertTrue(user.isKycValidado());
    }

    @Test
    void testSetters() {
        User user = new User();
        user.setEmail("a@b.com");
        user.setPassword("123");
        user.setCpf("11122233344");
        user.setNome("A");
        user.setSobrenome("B");
        user.setCelular("999999999");
        user.setConsentimentoDados(false);
        user.setKycValidado(false);

        assertEquals("a@b.com", user.getEmail());
        assertEquals("123", user.getPassword());
        assertEquals("11122233344", user.getCpf());
        assertEquals("A", user.getNome());
        assertEquals("B", user.getSobrenome());
        assertEquals("999999999", user.getCelular());
        assertFalse(user.getConsentimentoDados());
        assertFalse(user.isKycValidado());
    }

    @Test
    void testCustomConstructors() {
        User user1 = new User("mail@mail.com", "pw", UserRole.ADMIN);
        assertEquals("mail@mail.com", user1.getEmail());
        assertEquals("pw", user1.getPassword());
        assertEquals(UserRole.ADMIN, user1.getRole());

        User user2 = new User("mail2@mail.com", "pw2", UserRole.USER, "123", "Nome", "Sobrenome", "999");
        assertEquals("mail2@mail.com", user2.getEmail());
        assertEquals("pw2", user2.getPassword());
        assertEquals(UserRole.USER, user2.getRole());
        assertEquals("123", user2.getCpf());
        assertEquals("Nome", user2.getNome());
        assertEquals("Sobrenome", user2.getSobrenome());
        assertEquals("999", user2.getCelular());
    }

    @Test
    void testUserDetailsMethods() {
        User user = User.builder()
                .email("user@x.com")
                .password("pw")
                .role(UserRole.ADMIN)
                .build();

        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertEquals("ROLE_ADMIN", authorities.iterator().next().getAuthority());

        assertEquals("user@x.com", user.getUsername());
        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isCredentialsNonExpired());
        assertTrue(user.isEnabled());
    }

    @Test
    void testEqualsAndHashCode() {
        User user1 = User.builder().email("a@b.com").build();
        User user2 = User.builder().email("a@b.com").build();
        assertEquals(user1, user2);
        assertEquals(user1.hashCode(), user2.hashCode());
    }

    @Test
    void testToString() {
        User user = User.builder().email("a@b.com").build();
        String str = user.toString();
        assertTrue(str.contains("a@b.com"));
    }
}