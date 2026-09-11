package com.bharath.stacked.controller;

import com.bharath.stacked.dto.auth.AdminLoginRequest;
import com.bharath.stacked.dto.auth.AdminResponse;
import com.bharath.stacked.dto.auth.AuthResponse;
import com.bharath.stacked.dto.auth.RefreshResponse;
import com.bharath.stacked.exception.GlobalExceptionHandler;
import com.bharath.stacked.exception.UnauthorizedException;
import com.bharath.stacked.model.Role;
import com.bharath.stacked.security.CookieUtil;
import com.bharath.stacked.security.JwtTokenProvider;
import com.bharath.stacked.service.AuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private CookieUtil cookieUtil;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/auth/admin/login returns 200, JWT in body, and Set-Cookie header")
    void loginAdmin_Success() throws Exception {
        AdminResponse adminResponse = new AdminResponse(
                "admin-123", "Admin User", "admin@stacked.com", Role.ADMIN, Instant.now(), Instant.now());
        AuthResponse authResponse = AuthResponse.of("access-token-123", 900L, adminResponse);
        AuthService.AuthResult authResult = new AuthService.AuthResult(authResponse, "refresh-token-xyz");

        ResponseCookie cookie = ResponseCookie.from("refreshToken", "refresh-token-xyz")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ofDays(7))
                .build();

        when(authService.loginAdmin(any(AdminLoginRequest.class))).thenReturn(authResult);
        when(jwtTokenProvider.getRefreshTokenExpirationSeconds()).thenReturn(604800L);
        when(cookieUtil.createRefreshTokenCookie(eq("refresh-token-xyz"), any(Duration.class))).thenReturn(cookie);

        mockMvc.perform(post("/api/auth/admin/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "email": "admin@stacked.com",
                            "password": "Admin@123456"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.containsString("refreshToken=refresh-token-xyz")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("HttpOnly")))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.data.admin.email").value("admin@stacked.com"))
                .andExpect(jsonPath("$.data.admin.role").value("ADMIN"));
    }

    @Test
    @DisplayName("POST /api/auth/admin/login returns 401 when invalid credentials")
    void loginAdmin_InvalidCredentials_Returns401() throws Exception {
        when(authService.loginAdmin(any(AdminLoginRequest.class)))
                .thenThrow(new UnauthorizedException("Invalid email or password."));

        mockMvc.perform(post("/api/auth/admin/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "email": "admin@stacked.com",
                            "password": "WrongPassword"
                        }
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("POST /api/auth/refresh returns 200 with new access token and rotated refresh cookie")
    void refresh_Success() throws Exception {
        Cookie refreshCookie = new Cookie("refreshToken", "valid-refresh-token");
        when(cookieUtil.extractRefreshToken(any())).thenReturn(Optional.of("valid-refresh-token"));

        AuthService.RtrRefreshResult rtrResult = new AuthService.RtrRefreshResult(
                RefreshResponse.of("new-access-token", 900L),
                "rotated-refresh-token");
        when(authService.refreshAccessToken("valid-refresh-token")).thenReturn(rtrResult);
        when(jwtTokenProvider.getRefreshTokenExpirationSeconds()).thenReturn(604800L);

        ResponseCookie rotatedCookie = ResponseCookie.from("refreshToken", "rotated-refresh-token")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ofDays(7))
                .build();
        when(cookieUtil.createRefreshTokenCookie(eq("rotated-refresh-token"), any(Duration.class)))
                .thenReturn(rotatedCookie);

        mockMvc.perform(post("/api/auth/refresh")
                .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.containsString("refreshToken=rotated-refresh-token")))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.data.expiresIn").value(900));
    }

    @Test
    @DisplayName("POST /api/auth/refresh returns 401 when refresh cookie is missing")
    void refresh_MissingCookie_Returns401() throws Exception {
        when(cookieUtil.extractRefreshToken(any())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/auth/logout clears refresh cookie and returns 200")
    void logout_Success() throws Exception {
        Cookie refreshCookie = new Cookie("refreshToken", "token-to-logout");
        when(cookieUtil.extractRefreshToken(any())).thenReturn(Optional.of("token-to-logout"));

        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .build();
        when(cookieUtil.deleteRefreshTokenCookie()).thenReturn(deleteCookie);

        mockMvc.perform(post("/api/auth/logout")
                .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        verify(authService).logout(eq("token-to-logout"), any());
    }

    @Test
    @DisplayName("GET /api/auth/me returns 200 and admin details when authenticated")
    void getMe_Authenticated_ReturnsAdmin() throws Exception {
        AdminResponse adminResponse = new AdminResponse(
                "admin-123", "Admin User", "admin@stacked.com", Role.ADMIN, Instant.now(), Instant.now());
        when(authService.getAdminProfile("admin-123")).thenReturn(adminResponse);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "admin-123", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        mockMvc.perform(get("/api/auth/me")
                .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("admin@stacked.com"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }
}
