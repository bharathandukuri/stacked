package com.bharath.stacked.modules.security.model;

import com.bharath.stacked.config.AppProperties;
import com.bharath.stacked.modules.security.config.CookieProperties;
import com.bharath.stacked.modules.security.config.JwtProperties;
import com.bharath.stacked.modules.security.config.SecurityConfig;
import com.bharath.stacked.modules.security.dto.AdminLoginRequest;
import com.bharath.stacked.modules.security.dto.AdminResponse;
import com.bharath.stacked.modules.security.dto.AuthResponse;
import com.bharath.stacked.modules.security.dto.RefreshResponse;
import com.bharath.stacked.modules.security.service.CustomUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("Security Models, DTOs, and Configuration Unit Tests")
class SecurityModelsAndDtoTest {

    @Test
    @DisplayName("Role enum values")
    void roleEnum() {
        assertThat(Role.valueOf("ADMIN")).isEqualTo(Role.ADMIN);
        assertThat(Role.values()).containsExactly(Role.ADMIN);
    }

    @Test
    @DisplayName("User builder, getters, setters, and defaults")
    void userModel() {
        Instant now = Instant.now();
        User user = User.builder()
                .id("u-1")
                .name("Alice")
                .email("alice@test.com")
                .password("secret")
                .role(Role.ADMIN)
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertThat(user.getId()).isEqualTo("u-1");
        assertThat(user.getName()).isEqualTo("Alice");
        assertThat(user.getEmail()).isEqualTo("alice@test.com");
        assertThat(user.getPassword()).isEqualTo("secret");
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        assertThat(user.getCreatedAt()).isEqualTo(now);
        assertThat(user.getUpdatedAt()).isEqualTo(now);

        User defaultUser = new User();
        defaultUser.setName("Bob");
        assertThat(defaultUser.getRole()).isEqualTo(Role.ADMIN);

        user.setName("Alice Updated");
        assertThat(user.getName()).isEqualTo("Alice Updated");
        assertThat(user.toString()).contains("Alice Updated");
        assertThat(user.canEqual(defaultUser)).isTrue();
    }

    @Test
    @DisplayName("AdminLoginRequest record")
    void adminLoginRequest() {
        AdminLoginRequest req = new AdminLoginRequest("admin@test.com", "Password123");
        assertThat(req.email()).isEqualTo("admin@test.com");
        assertThat(req.password()).isEqualTo("Password123");
        assertThat(req.toString()).contains("admin@test.com");
    }

    @Test
    @DisplayName("AdminResponse fromUser maps all fields and handles null")
    void adminResponseFromUser() {
        assertThat(AdminResponse.fromUser(null)).isNull();

        Instant now = Instant.now();
        User user = User.builder()
                .id("id-123")
                .name("John")
                .email("john@test.com")
                .role(Role.ADMIN)
                .createdAt(now)
                .updatedAt(now)
                .build();

        AdminResponse response = AdminResponse.fromUser(user);
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("id-123");
        assertThat(response.name()).isEqualTo("John");
        assertThat(response.email()).isEqualTo("john@test.com");
        assertThat(response.role()).isEqualTo(Role.ADMIN);
        assertThat(response.createdAt()).isEqualTo(now);
        assertThat(response.updatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("AuthResponse and RefreshResponse factory methods")
    void authAndRefreshResponse() {
        AdminResponse admin = new AdminResponse("1", "Admin", "admin@test.com", Role.ADMIN, null, null);
        AuthResponse auth = AuthResponse.of("access-token-123", 900L, admin);

        assertThat(auth.accessToken()).isEqualTo("access-token-123");
        assertThat(auth.tokenType()).isEqualTo("Bearer");
        assertThat(auth.expiresIn()).isEqualTo(900L);
        assertThat(auth.admin()).isEqualTo(admin);

        RefreshResponse refresh = RefreshResponse.of("new-token-456", 3600L);
        assertThat(refresh.accessToken()).isEqualTo("new-token-456");
        assertThat(refresh.tokenType()).isEqualTo("Bearer");
        assertThat(refresh.expiresIn()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("CookieProperties and JwtProperties configuration records")
    void propertiesRecords() {
        CookieProperties cookieProps = new CookieProperties(true, true, "strict", "stacked.com", "/");
        assertThat(cookieProps.httpOnly()).isTrue();
        assertThat(cookieProps.secure()).isTrue();
        assertThat(cookieProps.sameSite()).isEqualTo("strict");
        assertThat(cookieProps.domain()).isEqualTo("stacked.com");
        assertThat(cookieProps.path()).isEqualTo("/");

        JwtProperties jwtProps = new JwtProperties("super-secret-key-12345678901234567890", 900000L, 604800000L);
        assertThat(jwtProps.secret()).isEqualTo("super-secret-key-12345678901234567890");
        assertThat(jwtProps.accessTokenExpirationMs()).isEqualTo(900000L);
        assertThat(jwtProps.refreshTokenExpirationMs()).isEqualTo(604800000L);
    }

    @Test
    @DisplayName("SecurityConfig beans: passwordEncoder, corsConfigurationSource, daoAuthenticationProvider")
    void securityConfigBeans() {
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        AppProperties appProperties = new AppProperties(
                "http://localhost:3000",
                "http://localhost:8080",
                List.of("http://localhost:3000")
        );

        SecurityConfig config = new SecurityConfig(
                userDetailsService,
                null,
                null,
                null,
                appProperties
        );

        PasswordEncoder encoder = config.passwordEncoder();
        assertThat(encoder).isNotNull();
        String encoded = encoder.encode("password");
        assertThat(encoder.matches("password", encoded)).isTrue();
        assertThat(encoder.matches("wrong", encoded)).isFalse();

        DaoAuthenticationProvider provider = config.daoAuthenticationProvider();
        assertThat(provider).isNotNull();

        CorsConfigurationSource corsSource = config.corsConfigurationSource();
        assertThat(corsSource).isNotNull();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        CorsConfiguration corsConfig = corsSource.getCorsConfiguration(request);
        assertThat(corsConfig).isNotNull();
        assertThat(corsConfig.getAllowedOrigins()).contains("http://localhost:3000");
        assertThat(corsConfig.getAllowCredentials()).isTrue();
    }
}
