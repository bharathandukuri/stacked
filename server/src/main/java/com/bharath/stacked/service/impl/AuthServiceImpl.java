package com.bharath.stacked.service.impl;

import com.bharath.stacked.dto.auth.AdminLoginRequest;
import com.bharath.stacked.dto.auth.AdminResponse;
import com.bharath.stacked.dto.auth.AuthResponse;
import com.bharath.stacked.dto.auth.RefreshResponse;
import com.bharath.stacked.exception.ForbiddenException;
import com.bharath.stacked.exception.ResourceNotFoundException;
import com.bharath.stacked.exception.UnauthorizedException;
import com.bharath.stacked.model.Role;
import com.bharath.stacked.model.User;
import com.bharath.stacked.repository.UserRepository;
import com.bharath.stacked.security.JwtTokenProvider;
import com.bharath.stacked.service.AuthService;
import com.bharath.stacked.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String SESSION_KEY_PREFIX = "auth:session:";
    private static final String USER_SESSIONS_PREFIX = "auth:user_sessions:";

    // Constant dummy hash used for constant-time comparison when user does not
    // exist
    private static final String DUMMY_BCRYPT_HASH = "$2a$10$7EqJtq98hPqEX7fNZaFWoO96F77B0wZt3DqB95m3Y6uM18K0uXQae";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @NonNull
    public AuthResult loginAdmin(@NonNull AdminLoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        log.info("Attempting admin authentication for email: {}", normalizedEmail);

        // Pre-check for timing attack defense
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null) {
            // Execute dummy password verification so timing cannot be used to enumerate
            // registered emails
            passwordEncoder.matches(request.password(), DUMMY_BCRYPT_HASH);
            log.warn("Login failed: email [{}] not found", normalizedEmail);
            throw new UnauthorizedException("Invalid email or password.");
        }

        try {
            // Delegate authentication through Spring Security AuthenticationManager &
            // DaoAuthenticationProvider
            Authentication authResult = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.password()));

            if (!authResult.isAuthenticated()) {
                throw new UnauthorizedException("Invalid email or password.");
            }
        } catch (BadCredentialsException e) {
            log.warn("Login failed: password mismatch for email [{}]", normalizedEmail);
            throw new UnauthorizedException("Invalid email or password.");
        }

        if (user.getRole() != Role.ADMIN) {
            log.warn("Login rejected: user [{}] lacks ADMIN role", normalizedEmail);
            throw new ForbiddenException("Access denied: ADMIN role required.");
        }

        String userId = Objects.requireNonNull(user.getId(), "User ID must not be null");

        // Generate tokens
        String tokenId = UUID.randomUUID().toString();
        String accessToken = jwtTokenProvider.generateAccessToken(userId, user.getEmail(), user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId, tokenId);

        // Persist session and track in user sessions set
        persistSession(userId, tokenId);

        AdminResponse adminResponse = AdminResponse.fromUser(user);
        AuthResponse authResponse = AuthResponse.of(
                accessToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds(),
                adminResponse);

        return new AuthResult(authResponse, refreshToken);
    }

    @Override
    @NonNull
    public RtrRefreshResult refreshAccessToken(@Nullable String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException("Refresh token is required.");
        }

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token.");
        }

        String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        String oldTokenId = jwtTokenProvider.getTokenIdFromRefreshToken(refreshToken);

        if (userId == null || oldTokenId == null) {
            throw new UnauthorizedException("Malformed refresh token payload.");
        }

        String oldSessionKey = buildSessionKey(userId, oldTokenId);
        if (!redisService.hasKey(oldSessionKey)) {
            log.warn("Refresh failed: Session key [{}] not found in Redis (expired, revoked, or already rotated)",
                    oldSessionKey);
            throw new UnauthorizedException("Session has expired or was revoked. Please log in again.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User associated with session was not found."));

        if (user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Access denied: ADMIN role required.");
        }

        // --- Refresh Token Rotation (RTR) ---
        // 1. Invalidate old session
        redisService.delete(oldSessionKey);
        try {
            redisTemplate.opsForSet().remove(buildUserSessionsKey(userId), oldTokenId);
        } catch (Exception e) {
            log.warn("Failed to remove old token from user sessions set: {}", e.getMessage());
        }

        // 2. Issue new access token and new rotated refresh token
        String newTokenId = UUID.randomUUID().toString();
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), newTokenId);

        // 3. Persist new session
        persistSession(userId, newTokenId);
        log.info("RTR complete: rotated refresh token and issued new access token for user [{}]", userId);

        RefreshResponse refreshResponse = RefreshResponse.of(newAccessToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds());
        return new RtrRefreshResult(refreshResponse, newRefreshToken);
    }

    @Override
    public void logout(@Nullable String refreshToken, @Nullable String currentUserId) {
        if (refreshToken == null || refreshToken.isBlank()) {
            log.debug("Logout called with null or empty refresh token; skipping session invalidation.");
            return;
        }

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            log.warn("Logout received invalid or expired refresh token.");
            return;
        }

        String tokenUserId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        String tokenId = jwtTokenProvider.getTokenIdFromRefreshToken(refreshToken);

        // Fix: Prevent cross-user session invalidation
        if (currentUserId != null && !currentUserId.isBlank() && tokenUserId != null
                && !tokenUserId.equals(currentUserId)) {
            log.warn("Security violation: user [{}] attempted to invalidate session belonging to user [{}]",
                    currentUserId, tokenUserId);
            throw new ForbiddenException("Cannot invalidate another user's session.");
        }

        if (tokenUserId != null && tokenId != null) {
            String sessionKey = buildSessionKey(tokenUserId, tokenId);
            boolean deleted = redisService.delete(sessionKey);
            try {
                redisTemplate.opsForSet().remove(buildUserSessionsKey(tokenUserId), tokenId);
            } catch (Exception e) {
                log.warn("Failed to remove token [{}] from user sessions set: {}", tokenId, e.getMessage());
            }
            log.info("Logout session invalidated for user [{}], key [{}] (wasActive: {})", tokenUserId, sessionKey,
                    deleted);
        }
    }

    @Override
    @NonNull
    public AdminResponse getAdminProfile(@Nullable String userId) {
        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("User ID is required to fetch admin profile.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with ID: " + userId));

        if (user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Access denied: ADMIN role required.");
        }

        return AdminResponse.fromUser(user);
    }

    private void persistSession(String userId, String tokenId) {
        String sessionKey = buildSessionKey(userId, tokenId);
        Duration sessionTtl = Duration.ofSeconds(jwtTokenProvider.getRefreshTokenExpirationSeconds());

        // Store active session flag
        redisService.set(sessionKey, "active", sessionTtl);

        // Track token ID in user sessions set
        try {
            String userSessionsKey = buildUserSessionsKey(userId);
            redisTemplate.opsForSet().add(userSessionsKey, tokenId);
            redisTemplate.expire(userSessionsKey, sessionTtl);
        } catch (Exception e) {
            log.warn("Unable to add token [{}] to user sessions set: {}", tokenId, e.getMessage());
        }
    }

    private String buildSessionKey(String userId, String tokenId) {
        return SESSION_KEY_PREFIX + userId + ":" + tokenId;
    }

    private String buildUserSessionsKey(String userId) {
        return USER_SESSIONS_PREFIX + userId;
    }
}
