package com.bharath.stacked.modules.security.service;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Date;

public interface TokenRevocationService {

    void revokeToken(@Nullable String tokenId, @NonNull Duration remainingTtl);

    void revokeAllUserTokens(@NonNull String userId);

    boolean isRevoked(@Nullable String tokenId, @Nullable String userId, @Nullable Date issuedAt);
}

