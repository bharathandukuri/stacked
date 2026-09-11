package com.bharath.stacked.security;

import com.bharath.stacked.config.JwtProperties;
import com.bharath.stacked.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey key;

    public JwtTokenProvider(@NonNull JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        byte[] keyBytes = jwtProperties.secret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            this.key = Keys.hmacShaKeyFor(padded);
        } else {
            this.key = Keys.hmacShaKeyFor(keyBytes);
        }
    }

    /**
     * Generate short-lived Access Token containing subject (userId), email, and
     * role.
     */
    @NonNull
    public String generateAccessToken(@NonNull String userId, @NonNull String email, @NonNull Role role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.accessTokenExpirationMs());

        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .claim("role", role.name())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Generate long-lived Refresh Token containing subject (userId) and unique
     * tokenId (jti).
     */
    @NonNull
    public String generateRefreshToken(@NonNull String userId, @Nullable String tokenId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.refreshTokenExpirationMs());

        return Jwts.builder()
                .subject(userId)
                .id(tokenId != null ? tokenId : UUID.randomUUID().toString())
                .claim("token_type", "refresh")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Parse and validate JWT token.
     */
    public boolean validateToken(@Nullable String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract Claims from token.
     */
    @NonNull
    public Claims getClaims(@NonNull String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Nullable
    public String getUserIdFromToken(@NonNull String token) {
        return getClaims(token).getSubject();
    }

    @Nullable
    public String getEmailFromToken(@NonNull String token) {
        return getClaims(token).get("email", String.class);
    }

    @Nullable
    public String getRoleFromToken(@NonNull String token) {
        return getClaims(token).get("role", String.class);
    }

    @Nullable
    public String getTokenIdFromRefreshToken(@NonNull String token) {
        return getClaims(token).getId();
    }

    public long getAccessTokenExpirationSeconds() {
        return jwtProperties.accessTokenExpirationMs() / 1000;
    }

    public long getRefreshTokenExpirationSeconds() {
        return jwtProperties.refreshTokenExpirationMs() / 1000;
    }
}
