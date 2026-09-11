package com.bharath.stacked.security;

import com.bharath.stacked.config.JwtProperties;
import com.bharath.stacked.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        // 256-bit test secret
        jwtProperties = new JwtProperties(
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
                900000L, // 15 mins
                604800000L // 7 days
        );
        tokenProvider = new JwtTokenProvider(jwtProperties);
    }

    @Test
    @DisplayName("generateAccessToken creates valid token with expected claims")
    void generateAccessToken_ValidClaims() {
        String token = tokenProvider.generateAccessToken("user-123", "admin@stacked.com", Role.ADMIN);

        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));
        assertEquals("user-123", tokenProvider.getUserIdFromToken(token));
        assertEquals("admin@stacked.com", tokenProvider.getEmailFromToken(token));
        assertEquals("ADMIN", tokenProvider.getRoleFromToken(token));
    }

    @Test
    @DisplayName("generateRefreshToken creates valid token with expected tokenId")
    void generateRefreshToken_ValidClaims() {
        String token = tokenProvider.generateRefreshToken("user-123", "token-uuid-456");

        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));
        assertEquals("user-123", tokenProvider.getUserIdFromToken(token));
        assertEquals("token-uuid-456", tokenProvider.getTokenIdFromRefreshToken(token));
    }

    @Test
    @DisplayName("validateToken returns false for corrupted or invalid token")
    void validateToken_CorruptedToken_ReturnsFalse() {
        String token = tokenProvider.generateAccessToken("user-123", "admin@stacked.com", Role.ADMIN);
        String corruptedToken = token + "xyz";

        assertFalse(tokenProvider.validateToken(corruptedToken));
    }

    @Test
    @DisplayName("validateToken returns false for expired token")
    void validateToken_ExpiredToken_ReturnsFalse() {
        JwtProperties expiredProperties = new JwtProperties(
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
                -1000L, // Already expired
                -1000L);
        JwtTokenProvider expiredProvider = new JwtTokenProvider(expiredProperties);
        String token = expiredProvider.generateAccessToken("user-123", "admin@stacked.com", Role.ADMIN);

        assertFalse(tokenProvider.validateToken(token));
    }
}
