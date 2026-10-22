package com.bharath.stacked.modules.security.util;

import com.bharath.stacked.modules.security.config.JwtProperties;
import com.bharath.stacked.modules.security.model.Role;
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

    @Test
    @DisplayName("Constructor throws IllegalArgumentException when secret is null or blank")
    void constructor_BlankSecret_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                new JwtProperties("", 900000L, 604800000L));

        assertThrows(IllegalArgumentException.class, () ->
                new JwtProperties("   ", 900000L, 604800000L));
    }

    @Test
    @DisplayName("Constructor throws IllegalArgumentException when secret is shorter than 32 bytes (256 bits)")
    void constructor_ShortSecret_ThrowsIllegalArgumentException() {
        // 16 bytes secret - unsafe, must NOT be zero-padded
        String shortSecret = "short-secret-16b";
        assertEquals(16, shortSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new JwtProperties(shortSecret, 900000L, 604800000L));

        assertTrue(ex.getMessage().contains("at least 256 bits (32 bytes)"));
    }

    @Test
    @DisplayName("Constructor succeeds when secret is exactly 32 bytes (256 bits)")
    void constructor_Exact32ByteSecret_Succeeds() {
        String exact32ByteSecret = "12345678901234567890123456789012";
        assertEquals(32, exact32ByteSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);

        JwtProperties props = new JwtProperties(exact32ByteSecret, 900000L, 604800000L);
        JwtTokenProvider provider = new JwtTokenProvider(props);

        String token = provider.generateAccessToken("user-1", "user@stacked.com", Role.ADMIN);
        assertTrue(provider.validateToken(token));
    }
}
