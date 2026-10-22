package com.bharath.stacked.modules.security.service;

import com.bharath.stacked.modules.security.dto.AdminLoginRequest;
import com.bharath.stacked.modules.security.dto.AdminResponse;
import com.bharath.stacked.modules.security.dto.AuthResponse;
import com.bharath.stacked.modules.security.dto.LoginRequest;
import com.bharath.stacked.modules.security.dto.RefreshResponse;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface AuthService {

    record AuthResult(@NonNull AuthResponse authResponse, @NonNull String refreshToken) {
    }

    record RtrRefreshResult(@NonNull RefreshResponse refreshResponse, @NonNull String newRefreshToken) {
    }

    @NonNull
    AuthResult login(@NonNull LoginRequest request);

    @NonNull
    AuthResult loginAdmin(@NonNull AdminLoginRequest request);

    @NonNull
    RtrRefreshResult refreshAccessToken(@Nullable String refreshToken);

    void logout(@Nullable String refreshToken, @Nullable String currentUserId);

    void logout(@Nullable String refreshToken, @Nullable String currentUserId, @Nullable String accessToken);

    @NonNull
    AdminResponse getAdminProfile(@Nullable String userId);

    @NonNull
    AdminResponse getUserProfile(@Nullable String userId);
}
