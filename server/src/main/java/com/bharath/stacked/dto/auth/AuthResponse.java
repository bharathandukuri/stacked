package com.bharath.stacked.dto.auth;

import org.jspecify.annotations.NonNull;

public record AuthResponse(
        @NonNull String accessToken,
        @NonNull String tokenType,
        long expiresIn,
        @NonNull AdminResponse admin) {
    @NonNull
    public static AuthResponse of(@NonNull String accessToken, long expiresInSeconds, @NonNull AdminResponse admin) {
        return new AuthResponse(accessToken, "Bearer", expiresInSeconds, admin);
    }
}
