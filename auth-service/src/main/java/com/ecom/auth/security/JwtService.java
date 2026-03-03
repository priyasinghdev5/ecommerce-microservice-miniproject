package com.ecom.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Handles JWT creation and parsing.
 * Used by AuthService to sign tokens and by JwtAuthFilter to validate them.
 */
@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiry}")
    private long accessTokenExpiry;  // seconds

    @Value("${jwt.issuer}")
    private String issuer;

    /**
     * Creates a signed JWT access token.
     * Claims include: sub (userId), roles, jti (unique ID for blacklisting).
     */
    public String generateAccessToken(String userId, String roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(userId)
                .claim("roles", roles)
                .id(UUID.randomUUID().toString())   // jti — used for blacklisting on logout
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenExpiry)))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Generates a random opaque refresh token.
     * Stored as a hash in PostgreSQL, not as a JWT.
     */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString().replace("-", "") +
               UUID.randomUUID().toString().replace("-", "");
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUserId(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractJti(String token) {
        return extractAllClaims(token).getId();
    }

    public long getAccessTokenExpiry() {
        return accessTokenExpiry;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
