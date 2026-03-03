package com.ecom.auth.service;

import com.ecom.auth.dto.*;
import com.ecom.auth.entity.AuthUser;
import com.ecom.auth.entity.RefreshToken;
import com.ecom.auth.entity.Role;
import com.ecom.auth.repository.AuthUserRepository;
import com.ecom.auth.repository.RefreshTokenRepository;
import com.ecom.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthUserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;

    private static final long REFRESH_TOKEN_EXPIRY_DAYS = 7;

    @Transactional
    public TokenResponse login(LoginRequest request) {
        AuthUser user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!user.isEnabled() || user.isLocked()) {
            throw new RuntimeException("Account is disabled or locked");
        }

        return issueTokens(user);
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email already registered");
        }

        AuthUser user = AuthUser.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .enabled(true)
                .locked(false)
                .build();

        userRepository.save(user);
        log.info("New user registered: {}", request.email());
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        String tokenHash = hashToken(request.refreshToken());

        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (stored.isRevoked()) {
            throw new RuntimeException("Refresh token has been revoked");
        }

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Refresh token has expired");
        }

        // Rotate: revoke old, issue new
        stored.setRevoked(true);
        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);

        return issueTokens(stored.getUser());
    }

    @Transactional
    public void logout(String accessToken) {
        try {
            String jti = jwtService.extractJti(accessToken);
            // Add jti to Redis blacklist — expires when token would have expired
            redisTemplate.opsForValue().set(
                "blacklist:" + jti,
                "1",
                Duration.ofSeconds(jwtService.getAccessTokenExpiry())
            );
            log.info("Token blacklisted: jti={}", jti);
        } catch (Exception e) {
            log.warn("Could not blacklist token: {}", e.getMessage());
        }
    }

    // ── Private helpers ────────────────────────────────────────────

    private TokenResponse issueTokens(AuthUser user) {
        String roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.joining(","));

        String accessToken = jwtService.generateAccessToken(user.getId().toString(), roles);
        String rawRefreshToken = jwtService.generateRefreshToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(hashToken(rawRefreshToken))
                .user(user)
                .expiresAt(Instant.now().plus(Duration.ofDays(REFRESH_TOKEN_EXPIRY_DAYS)))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        return TokenResponse.of(accessToken, rawRefreshToken, jwtService.getAccessTokenExpiry());
    }

    private String hashToken(String token) {
        // Simple SHA-256 hash of the raw token for storage
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }
}
