package com.extractor.unraveldocs.security;

import com.extractor.unraveldocs.user.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {
    @Value("${app.jwt-secret}")
    private String jwtSecret;

    @Value("${app.jwt-expiration-milliseconds}")
    private Long jwtAccessExpirationInMs;

    @Value("${app.jwt-refresh-token-expiration-milliseconds}")
    private Long jwtRefreshExpirationInMs;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiryDate = now.plusMillis(jwtAccessExpirationInMs);
        String jti = UUID.randomUUID().toString();

        Claims claims = Jwts.claims()
                .subject(user.getEmail())
                .add("roles", user.getRole().name())
                .add("isVerified", user.isVerified())
                .add("userId", user.getId())
                .id(jti)
                .build();

        return Jwts.builder()
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryDate))
                .signWith(key())
                .compact();
    }

    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expiryDate = now.plusMillis(jwtRefreshExpirationInMs);
        String jti = UUID.randomUUID().toString();

        Claims claims = Jwts.claims()
                .subject(user.getEmail())
                .add("userId", user.getId())
                .id(jti)
                .add("type", "REFRESH")
                .build();

        return Jwts.builder()
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryDate))
                .signWith(key())
                .compact();
    }

    public String getEmailFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public String getUserIdFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("userId", String.class);
    }

    public String getJtiFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getId();
        } catch (JwtException e) {
            log.error("Could not get JTI from token: {}", e.getMessage());
            return null;
        }
    }

    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            log.warn("JWT token validation failed: {} - Token: {}", e.getMessage(), "[PROTECTED]");
            return false;
        }
    }

    public Long getAccessExpirationInMs() {
        return jwtAccessExpirationInMs;
    }

    public Long getRefreshExpirationInMs() {
        return jwtRefreshExpirationInMs;
    }
}
