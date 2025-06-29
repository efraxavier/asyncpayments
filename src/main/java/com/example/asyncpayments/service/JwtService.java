package com.example.asyncpayments.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret.key}")
    private String secretKey;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    private Algorithm getAlgorithm() {
        return Algorithm.HMAC256(secretKey);
    }

    public String generateToken(UserDetails userDetails, Long userId) {
        logger.info("[JWT] Gerando token para userId={}", userId);
        return JWT.create()
                .withSubject(userDetails.getUsername())
                .withClaim("id", userId)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + jwtExpiration))
                .sign(getAlgorithm());
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        boolean valid = false;
        try {
            String username = extractUsername(token);
            valid = username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JWTVerificationException e) {
            logger.warn("[JWT] Token inválido: {}", e.getMessage());
            return false;
        }
        logger.info("[JWT] Validação de token para user={} resultado={}", userDetails.getUsername(), valid);
        return valid;
    }

    public String extractUsername(String token) {
        return decodeToken(token).getSubject();
    }

    public Long extractUserId(String token) {
        return decodeToken(token).getClaim("id").asLong();
    }

    private boolean isTokenExpired(String token) {
        return decodeToken(token).getExpiresAt().before(new Date());
    }


    private DecodedJWT decodeToken(String token) {
        JWTVerifier verifier = JWT.require(getAlgorithm()).build();
        return verifier.verify(token);
    }
}