package com.bharath.stacked.controller;

import com.bharath.stacked.common.api.ApiResponse;
import com.bharath.stacked.dto.auth.AdminLoginRequest;
import com.bharath.stacked.dto.auth.AdminResponse;
import com.bharath.stacked.dto.auth.AuthResponse;
import com.bharath.stacked.dto.auth.RefreshResponse;
import com.bharath.stacked.exception.UnauthorizedException;
import com.bharath.stacked.security.CookieUtil;
import com.bharath.stacked.security.JwtTokenProvider;
import com.bharath.stacked.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Admin login endpoint.
     * Validates credentials, verifies ADMIN role, sets HTTP-only refresh cookie,
     * and returns access token.
     */
    @PostMapping("/admin/login")
    @NonNull
    public ResponseEntity<ApiResponse<AuthResponse>> loginAdmin(
            @Valid @RequestBody @NonNull AdminLoginRequest request) {
        AuthService.AuthResult result = authService.loginAdmin(request);

        Duration refreshDuration = Duration.ofSeconds(jwtTokenProvider.getRefreshTokenExpirationSeconds());
        ResponseCookie refreshCookie = cookieUtil.createRefreshTokenCookie(result.refreshToken(), refreshDuration);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.success(result.authResponse(), "Login successful").getBody());
    }

    /**
     * Refresh access token endpoint with Refresh Token Rotation (RTR).
     * Extracts refresh token from HTTP-only cookie, rotates the token in Redis,
     * issues a new refresh token cookie, and returns a new access token.
     */
    @PostMapping("/refresh")
    @NonNull
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(@NonNull HttpServletRequest request) {
        String refreshToken = cookieUtil.extractRefreshToken(request)
                .orElseThrow(() -> new UnauthorizedException("Refresh token cookie is missing or empty."));

        AuthService.RtrRefreshResult rtrResult = authService.refreshAccessToken(refreshToken);

        // Set the rotated refresh token in the HTTP-only cookie
        Duration refreshDuration = Duration.ofSeconds(jwtTokenProvider.getRefreshTokenExpirationSeconds());
        ResponseCookie rotatedCookie = cookieUtil.createRefreshTokenCookie(rtrResult.newRefreshToken(),
                refreshDuration);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, rotatedCookie.toString())
                .body(ApiResponse.success(rtrResult.refreshResponse(), "Access token refreshed successfully")
                        .getBody());
    }

    /**
     * Logout endpoint.
     * Validates session ownership, invalidates session in Redis, and clears
     * HTTP-only refresh token cookie.
     */
    @PostMapping("/logout")
    @NonNull
    public ResponseEntity<ApiResponse<Void>> logout(
            @NonNull HttpServletRequest request,
            @Nullable Authentication authentication) {
        String refreshToken = cookieUtil.extractRefreshToken(request).orElse(null);
        String currentUserId = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName()
                : null;

        authService.logout(refreshToken, currentUserId);
        SecurityContextHolder.clearContext();

        ResponseCookie deleteCookie = cookieUtil.deleteRefreshTokenCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(ApiResponse.<Void>success(null, "Logged out successfully").getBody());
    }

    /**
     * Protected endpoint to get current authenticated admin profile.
     */
    @GetMapping("/me")
    @NonNull
    public ResponseEntity<ApiResponse<AdminResponse>> getMe(@Nullable Authentication authentication) {
        if (authentication == null || authentication.getName() == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Authentication is required to access admin profile.");
        }

        AdminResponse adminResponse = authService.getAdminProfile(authentication.getName());
        return ApiResponse.success(adminResponse, "Admin profile retrieved successfully");
    }
}
