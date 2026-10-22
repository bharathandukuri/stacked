package com.bharath.stacked.modules.security.service.impl;

import com.bharath.stacked.modules.security.config.JwtProperties;
import com.bharath.stacked.service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenRevocationServiceImpl Unit Tests")
class TokenRevocationServiceImplTest {

    @Mock
    private RedisService redisService;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private TokenRevocationServiceImpl tokenRevocationService;

    @BeforeEach
    void setUp() {
        lenient().when(jwtProperties.accessTokenExpirationMs()).thenReturn(900000L);
    }

    @Test
    @DisplayName("revokeToken stores blacklist entry in Redis with remaining TTL")
    void revokeToken_StoresInRedis() {
        tokenRevocationService.revokeToken("token-id-123", Duration.ofMinutes(10));

        verify(redisService).set("auth:blacklist:token:token-id-123", "revoked", Duration.ofMinutes(10));
    }

    @Test
    @DisplayName("revokeToken skips blank or invalid tokens")
    void revokeToken_BlankToken_NoOp() {
        tokenRevocationService.revokeToken("", Duration.ofMinutes(10));
        tokenRevocationService.revokeToken(null, Duration.ofMinutes(10));
        tokenRevocationService.revokeToken("token-1", Duration.ZERO);
        tokenRevocationService.revokeToken("token-1", Duration.ofSeconds(-5));

        verifyNoInteractions(redisService);
    }

    @Test
    @DisplayName("revokeAllUserTokens stores invalidation marker in Redis")
    void revokeAllUserTokens_StoresMarker() {
        tokenRevocationService.revokeAllUserTokens("user-1");

        verify(redisService).set(eq("auth:user:invalidated_at:user-1"), any(String.class), eq(Duration.ofMillis(900000L)));
    }

    @Test
    @DisplayName("isRevoked returns true when token ID is present in blacklist")
    void isRevoked_BlacklistedToken_ReturnsTrue() {
        when(redisService.hasKey("auth:blacklist:token:token-abc")).thenReturn(true);

        boolean revoked = tokenRevocationService.isRevoked("token-abc", "user-1", new Date());

        assertThat(revoked).isTrue();
    }

    @Test
    @DisplayName("isRevoked returns true when token was issued before user invalidation marker")
    void isRevoked_TokenIssuedBeforeMarker_ReturnsTrue() {
        long invalidatedAt = 100000L;
        when(redisService.hasKey("auth:blacklist:token:token-abc")).thenReturn(false);
        when(redisService.get("auth:user:invalidated_at:user-1")).thenReturn(String.valueOf(invalidatedAt));

        // Issued at 90000, before 100000
        Date issuedAt = new Date(90000L);
        boolean revoked = tokenRevocationService.isRevoked("token-abc", "user-1", issuedAt);

        assertThat(revoked).isTrue();
    }

    @Test
    @DisplayName("isRevoked returns false when token was issued after user invalidation marker")
    void isRevoked_TokenIssuedAfterMarker_ReturnsFalse() {
        long invalidatedAt = 100000L;
        when(redisService.hasKey("auth:blacklist:token:token-abc")).thenReturn(false);
        when(redisService.get("auth:user:invalidated_at:user-1")).thenReturn(String.valueOf(invalidatedAt));

        // Issued at 110000, after 100000
        Date issuedAt = new Date(110000L);
        boolean revoked = tokenRevocationService.isRevoked("token-abc", "user-1", issuedAt);

        assertThat(revoked).isFalse();
    }

    @Test
    @DisplayName("isRevoked returns false when neither token nor user is revoked")
    void isRevoked_NotRevoked_ReturnsFalse() {
        when(redisService.hasKey("auth:blacklist:token:token-abc")).thenReturn(false);
        when(redisService.get("auth:user:invalidated_at:user-1")).thenReturn(null);

        boolean revoked = tokenRevocationService.isRevoked("token-abc", "user-1", new Date());

        assertThat(revoked).isFalse();
    }
}

