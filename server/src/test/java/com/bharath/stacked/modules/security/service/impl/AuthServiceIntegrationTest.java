package com.bharath.stacked.modules.security.service.impl;

import com.bharath.stacked.AbstractIntegrationTest;
import com.bharath.stacked.exception.ForbiddenException;
import com.bharath.stacked.exception.ResourceNotFoundException;
import com.bharath.stacked.exception.UnauthorizedException;
import com.bharath.stacked.modules.security.dto.AdminLoginRequest;
import com.bharath.stacked.modules.security.dto.AdminResponse;
import com.bharath.stacked.modules.security.model.Role;
import com.bharath.stacked.modules.security.model.User;
import com.bharath.stacked.modules.security.repository.UserRepository;
import com.bharath.stacked.modules.security.service.AuthService;
import com.bharath.stacked.modules.security.util.JwtTokenProvider;
import com.bharath.stacked.service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AuthService Live Integration Tests")
class AuthServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisService redisService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        var factory = stringRedisTemplate.getConnectionFactory();
        if (factory != null) {
            try (var conn = factory.getConnection()) {
                conn.serverCommands().flushDb();
            }
        }
    }

    private User createAdmin(String email, String rawPassword) {
        User admin = User.builder()
                .name("Test Admin")
                .email(email.toLowerCase().trim())
                .password(passwordEncoder.encode(rawPassword))
                .role(Role.ADMIN)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return userRepository.save(admin);
    }

    @Test
    @DisplayName("loginAdmin authenticates against MongoDB and stores active session in Redis")
    void loginAdminSuccessLive() {
        User admin = createAdmin("admin@stacked.com", "Admin@123456");

        var result = authService.loginAdmin(new AdminLoginRequest("admin@stacked.com", "Admin@123456"));

        assertThat(result).isNotNull();
        assertThat(result.authResponse().accessToken()).isNotBlank();
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.authResponse().admin().email()).isEqualTo("admin@stacked.com");
        assertThat(result.authResponse().admin().role()).isEqualTo(Role.ADMIN);

        // Verify JWT validity
        assertThat(jwtTokenProvider.validateToken(result.authResponse().accessToken())).isTrue();
        assertThat(jwtTokenProvider.validateToken(result.refreshToken())).isTrue();

        // Verify active session stored in Redis
        String tokenId = jwtTokenProvider.getTokenIdFromRefreshToken(result.refreshToken());
        String sessionKey = "auth:session:" + admin.getId() + ":" + tokenId;
        assertThat(redisService.hasKey(sessionKey)).isTrue();
        assertThat(redisService.get(sessionKey)).isEqualTo("active");
    }

    @Test
    @DisplayName("loginAdmin throws UnauthorizedException when password does not match")
    void loginAdminBadPasswordLive() {
        createAdmin("admin@stacked.com", "Admin@123456");

        assertThatThrownBy(() -> authService.loginAdmin(new AdminLoginRequest("admin@stacked.com", "WrongPassword!")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid email or password.");
    }

    @Test
    @DisplayName("loginAdmin throws UnauthorizedException when email not found in MongoDB")
    void loginAdminUnknownEmailLive() {
        assertThatThrownBy(() -> authService.loginAdmin(new AdminLoginRequest("nobody@stacked.com", "Admin@123456")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid email or password.");
    }

    @Test
    @DisplayName("refreshAccessToken rotates tokens (RTR) and invalidates old Redis session")
    void refreshAccessTokenRotationLive() {
        User admin = createAdmin("admin-rtr@stacked.com", "Admin@123456");

        var loginResult = authService.loginAdmin(new AdminLoginRequest("admin-rtr@stacked.com", "Admin@123456"));
        String oldRefreshToken = loginResult.refreshToken();
        String oldTokenId = jwtTokenProvider.getTokenIdFromRefreshToken(oldRefreshToken);
        String oldSessionKey = "auth:session:" + admin.getId() + ":" + oldTokenId;

        assertThat(redisService.hasKey(oldSessionKey)).isTrue();

        // Perform Refresh Token Rotation
        var refreshResult = authService.refreshAccessToken(oldRefreshToken);

        assertThat(refreshResult).isNotNull();
        assertThat(refreshResult.refreshResponse().accessToken()).isNotBlank();
        assertThat(refreshResult.newRefreshToken()).isNotBlank();

        // Old token session must be revoked in Redis
        assertThat(redisService.hasKey(oldSessionKey)).isFalse();

        // New token session must be active in Redis
        String newTokenId = jwtTokenProvider.getTokenIdFromRefreshToken(refreshResult.newRefreshToken());
        String newSessionKey = "auth:session:" + admin.getId() + ":" + newTokenId;
        assertThat(redisService.hasKey(newSessionKey)).isTrue();

        // Attempting to reuse old refresh token must be rejected
        assertThatThrownBy(() -> authService.refreshAccessToken(oldRefreshToken))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Session has expired or was revoked");
    }

    @Test
    @DisplayName("logout revokes session from Redis preventing subsequent token refresh")
    void logoutRevokesSessionLive() {
        User admin = createAdmin("admin-logout@stacked.com", "Admin@123456");

        var loginResult = authService.loginAdmin(new AdminLoginRequest("admin-logout@stacked.com", "Admin@123456"));
        String refreshToken = loginResult.refreshToken();
        String tokenId = jwtTokenProvider.getTokenIdFromRefreshToken(refreshToken);
        String sessionKey = "auth:session:" + admin.getId() + ":" + tokenId;

        assertThat(redisService.hasKey(sessionKey)).isTrue();

        authService.logout(refreshToken, admin.getId());

        assertThat(redisService.hasKey(sessionKey)).isFalse();

        // Subsequent refresh must fail
        assertThatThrownBy(() -> authService.refreshAccessToken(refreshToken))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("getAdminProfile fetches profile and validates user ID requirement")
    void getAdminProfileLive() {
        User admin = createAdmin("admin-profile@stacked.com", "Admin@123456");

        AdminResponse profile = authService.getAdminProfile(admin.getId());
        assertThat(profile).isNotNull();
        assertThat(profile.email()).isEqualTo("admin-profile@stacked.com");
        assertThat(profile.role()).isEqualTo(Role.ADMIN);

        // Null user ID is rejected
        assertThatThrownBy(() -> authService.getAdminProfile(null))
                .isInstanceOf(UnauthorizedException.class);

        // Unknown ID is not found
        assertThatThrownBy(() -> authService.getAdminProfile("unknown-id-xyz"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
