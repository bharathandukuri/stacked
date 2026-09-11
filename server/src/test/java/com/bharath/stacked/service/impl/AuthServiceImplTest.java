package com.bharath.stacked.service.impl;

import com.bharath.stacked.dto.auth.AdminLoginRequest;
import com.bharath.stacked.dto.auth.AdminResponse;
import com.bharath.stacked.exception.ForbiddenException;
import com.bharath.stacked.exception.ResourceNotFoundException;
import com.bharath.stacked.exception.UnauthorizedException;
import com.bharath.stacked.model.Role;
import com.bharath.stacked.model.User;
import com.bharath.stacked.repository.UserRepository;
import com.bharath.stacked.security.JwtTokenProvider;
import com.bharath.stacked.service.AuthService;
import com.bharath.stacked.service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RedisService redisService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private SetOperations<String, Object> setOperations;

    @Mock
    private Authentication authentication;

    private AuthServiceImpl authService;

    private User sampleAdmin;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);

        authService = new AuthServiceImpl(
                userRepository,
                passwordEncoder,
                authenticationManager,
                jwtTokenProvider,
                redisService,
                redisTemplate);

        sampleAdmin = User.builder()
                .id("admin-123")
                .name("Test Admin")
                .email("admin@stacked.com")
                .password("$2a$10$hashedPassword")
                .role(Role.ADMIN)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("loginAdmin successfully authenticates valid admin credentials and returns tokens")
    void loginAdmin_Success() {
        AdminLoginRequest request = new AdminLoginRequest("admin@stacked.com", "Admin@123456");

        when(userRepository.findByEmail("admin@stacked.com")).thenReturn(Optional.of(sampleAdmin));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);

        when(jwtTokenProvider.generateAccessToken(eq("admin-123"), eq("admin@stacked.com"), eq(Role.ADMIN)))
                .thenReturn("access-token-xyz");
        when(jwtTokenProvider.generateRefreshToken(eq("admin-123"), any()))
                .thenReturn("refresh-token-xyz");
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(jwtTokenProvider.getRefreshTokenExpirationSeconds()).thenReturn(604800L);

        AuthService.AuthResult result = authService.loginAdmin(request);

        assertNotNull(result);
        assertEquals("access-token-xyz", result.authResponse().accessToken());
        assertEquals("Bearer", result.authResponse().tokenType());
        assertEquals(900L, result.authResponse().expiresIn());
        assertEquals("admin@stacked.com", result.authResponse().admin().email());
        assertEquals(Role.ADMIN, result.authResponse().admin().role());
        assertEquals("refresh-token-xyz", result.refreshToken());

        verify(redisService).set(startsWith("auth:session:admin-123:"), eq("active"), any(Duration.class));
        verify(setOperations).add(eq("auth:user_sessions:admin-123"), any());
    }

    @Test
    @DisplayName("loginAdmin performs dummy hash check and throws UnauthorizedException when user does not exist (Timing Attack Defense)")
    void loginAdmin_UserNotFound_ExecutesDummyHash_ThrowsUnauthorized() {
        AdminLoginRequest request = new AdminLoginRequest("notfound@stacked.com", "Admin@123456");
        when(userRepository.findByEmail("notfound@stacked.com")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authService.loginAdmin(request));
        // Verify dummy hash execution took place
        verify(passwordEncoder).matches(eq("Admin@123456"), startsWith("$2a$10$"));
        verify(redisService, never()).set(anyString(), any(), any());
    }

    @Test
    @DisplayName("loginAdmin throws UnauthorizedException when authentication fails with BadCredentialsException")
    void loginAdmin_InvalidPassword_ThrowsUnauthorized() {
        AdminLoginRequest request = new AdminLoginRequest("admin@stacked.com", "WrongPassword");
        when(userRepository.findByEmail("admin@stacked.com")).thenReturn(Optional.of(sampleAdmin));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(UnauthorizedException.class, () -> authService.loginAdmin(request));
        verify(redisService, never()).set(anyString(), any(), any());
    }

    @Test
    @DisplayName("refreshAccessToken successfully performs Refresh Token Rotation (RTR)")
    void refreshAccessToken_Success_RotatesToken() {
        String oldRefreshToken = "old-refresh-token";
        when(jwtTokenProvider.validateToken(oldRefreshToken)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(oldRefreshToken)).thenReturn("admin-123");
        when(jwtTokenProvider.getTokenIdFromRefreshToken(oldRefreshToken)).thenReturn("old-token-id-456");
        when(redisService.hasKey("auth:session:admin-123:old-token-id-456")).thenReturn(true);
        when(userRepository.findById("admin-123")).thenReturn(Optional.of(sampleAdmin));

        when(jwtTokenProvider.generateAccessToken("admin-123", "admin@stacked.com", Role.ADMIN))
                .thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(eq("admin-123"), any()))
                .thenReturn("new-rotated-refresh-token");
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(jwtTokenProvider.getRefreshTokenExpirationSeconds()).thenReturn(604800L);

        AuthService.RtrRefreshResult result = authService.refreshAccessToken(oldRefreshToken);

        assertNotNull(result);
        assertEquals("new-access-token", result.refreshResponse().accessToken());
        assertEquals("new-rotated-refresh-token", result.newRefreshToken());

        // Invalidate old session in Redis and Set
        verify(redisService).delete("auth:session:admin-123:old-token-id-456");
        verify(setOperations).remove("auth:user_sessions:admin-123", "old-token-id-456");

        // Persist new session in Redis and Set
        verify(redisService).set(startsWith("auth:session:admin-123:"), eq("active"), any(Duration.class));
        verify(setOperations).add(eq("auth:user_sessions:admin-123"), any());
    }

    @Test
    @DisplayName("refreshAccessToken throws UnauthorizedException when session not found in Redis")
    void refreshAccessToken_SessionMissingInRedis_ThrowsUnauthorized() {
        String refreshToken = "valid-token-but-expired-session";
        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(refreshToken)).thenReturn("admin-123");
        when(jwtTokenProvider.getTokenIdFromRefreshToken(refreshToken)).thenReturn("token-id-456");
        when(redisService.hasKey("auth:session:admin-123:token-id-456")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.refreshAccessToken(refreshToken));
    }

    @Test
    @DisplayName("logout throws ForbiddenException when current user tries to invalidate another user's session")
    void logout_MismatchedUser_ThrowsForbidden() {
        String refreshToken = "user2-refresh-token";
        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(refreshToken)).thenReturn("user-2");
        when(jwtTokenProvider.getTokenIdFromRefreshToken(refreshToken)).thenReturn("token-456");

        assertThrows(ForbiddenException.class, () -> authService.logout(refreshToken, "user-1"));
        verify(redisService, never()).delete(anyString());
    }

    @Test
    @DisplayName("logout invalidates session and removes token from user sessions set when user IDs match")
    void logout_MatchingUser_DeletesRedisKey() {
        String refreshToken = "valid-refresh-token";
        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(refreshToken)).thenReturn("admin-123");
        when(jwtTokenProvider.getTokenIdFromRefreshToken(refreshToken)).thenReturn("token-id-456");

        authService.logout(refreshToken, "admin-123");

        verify(redisService).delete("auth:session:admin-123:token-id-456");
        verify(setOperations).remove("auth:user_sessions:admin-123", "token-id-456");
    }

    @Test
    @DisplayName("getAdminProfile returns admin details when admin exists")
    void getAdminProfile_Success() {
        when(userRepository.findById("admin-123")).thenReturn(Optional.of(sampleAdmin));

        AdminResponse profile = authService.getAdminProfile("admin-123");

        assertNotNull(profile);
        assertEquals("admin-123", profile.id());
        assertEquals("admin@stacked.com", profile.email());
        assertEquals(Role.ADMIN, profile.role());
    }

    @Test
    @DisplayName("getAdminProfile throws ResourceNotFoundException when user not found")
    void getAdminProfile_NotFound_ThrowsException() {
        when(userRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.getAdminProfile("unknown"));
    }
}
