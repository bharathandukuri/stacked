package com.bharath.stacked.modules.security.dto;

import org.jspecify.annotations.NonNull;

public record RefreshResponse(
        @NonNull String accessToken,
        @NonNull String tokenType,
        long expiresIn) {
    @NonNull
    public static RefreshResponse of(@NonNull String accessToken, long expiresInSeconds) {
        return new RefreshResponse(accessToken, "Bearer", expiresInSeconds);
    }
}
