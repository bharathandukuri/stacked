package com.bharath.stacked.modules.security.service;

import com.bharath.stacked.modules.security.dto.AdminLoginRequest;
import com.bharath.stacked.modules.security.dto.AdminResponse;
import com.bharath.stacked.modules.security.dto.AuthResponse;
import com.bharath.stacked.modules.security.dto.RefreshResponse;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface AuthService {

    record AuthResult(@NonNull AuthResponse authResponse, @NonNull String refreshToken) {
    }

    record RtrRefreshResult(@NonNull RefreshResponse refreshResponse, @NonNull String newRefreshToken) {
    }

    @NonNull
    AuthResult loginAdmin(@NonNull AdminLoginRequest request);

    @NonNull
    RtrRefreshResult refreshAccessToken(@Nullable String refreshToken);

    void logout(@Nullable String refreshToken, @Nullable String currentUserId);

    @NonNull
    AdminResponse getAdminProfile(@Nullable String userId);
}
