package com.bharath.stacked.modules.security.service.impl;

import com.bharath.stacked.modules.security.config.JwtProperties;
import com.bharath.stacked.modules.security.service.TokenRevocationService;
import com.bharath.stacked.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRevocationServiceImpl implements TokenRevocationService {

    public static final String TOKEN_BLACKLIST_PREFIX = "auth:blacklist:token:";
    public static final String USER_INVALIDATED_PREFIX = "auth:user:invalidated_at:";

    private final RedisService redisService;
    private final JwtProperties jwtProperties;

    @Override
    public void revokeToken(@Nullable String tokenId, @NonNull Duration remainingTtl) {
        if (tokenId == null || tokenId.isBlank() || remainingTtl.isNegative() || remainingTtl.isZero()) {
            return;
        }
        try {
            redisService.set(TOKEN_BLACKLIST_PREFIX + tokenId, "revoked", remainingTtl);
            log.debug("Revoked access token [id={}] with TTL {}s", tokenId, remainingTtl.toSeconds());
        } catch (Exception e) {
            log.warn("Failed to blacklist token in Redis: {}", e.getMessage());
        }
    }

    @Override
    public void revokeAllUserTokens(@NonNull String userId) {
        if (userId.isBlank()) {
            return;
        }
        try {
            Duration ttl = Duration.ofMillis(jwtProperties.accessTokenExpirationMs());
            long now = System.currentTimeMillis();
            redisService.set(USER_INVALIDATED_PREFIX + userId, String.valueOf(now), ttl);
            log.info("Revoked all active tokens for user [{}] issued before {}", userId, now);
        } catch (Exception e) {
            log.warn("Failed to set user revocation marker in Redis for user [{}]: {}", userId, e.getMessage());
        }
    }

    @Override
    public boolean isRevoked(@Nullable String tokenId, @Nullable String userId, @Nullable Date issuedAt) {
        try {
            if (tokenId != null && !tokenId.isBlank()) {
                if (redisService.hasKey(TOKEN_BLACKLIST_PREFIX + tokenId)) {
                    return true;
                }
            }

            if (userId != null && !userId.isBlank() && issuedAt != null) {
                Object val = redisService.get(USER_INVALIDATED_PREFIX + userId);
                if (val != null) {
                    long invalidatedAt = Long.parseLong(val.toString());
                    if (issuedAt.getTime() <= invalidatedAt) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error checking token revocation in Redis: {}", e.getMessage());
        }
        return false;
    }
}

