package com.github.birulazena.PaymentService.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtServiceTest {

    @Value("${JWT_SECRET}")
    private String jwtSecret;

    @Value("${ACCESS_EXPIRATION}")
    private long jwtAccessExpiration;

    private SecretKey secretKey;

    @PostConstruct
    private void init() {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String username, Long userId, String role) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime() + jwtAccessExpiration))
                .claim("type", "access")
                .claim("userId", userId)
                .claim("role", role)
                .signWith(secretKey)
                .compact();
    }

}
