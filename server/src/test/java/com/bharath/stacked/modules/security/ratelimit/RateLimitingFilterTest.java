package com.bharath.stacked.modules.security.ratelimit;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private FilterChain filterChain;

    private RateLimitProperties properties;
    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        properties = new RateLimitProperties(
                true,
                false,
                new RateLimitProperties.PolicyConfig(10, 60, true, List.of("POST:/api/auth/login", "POST:/api/auth/admin/login")),
                new RateLimitProperties.PolicyConfig(5, 60, true, List.of("POST:/api/admin/coding-problem/run-preview", "POST:/api/coding-problem/run-preview", "POST:/api/assessments/*/run-sample")),
                new RateLimitProperties.PolicyConfig(20, 60, false, List.of("POST:/api/assessments/join-by-code")),
                new RateLimitProperties.PolicyConfig(100, 60, false, List.of("/api/**"))
        );
        filter = new RateLimitingFilter(rateLimiterService, properties);
    }

    @Test
    @DisplayName("Disabled rate limiting passes through without checking quota")
    void disabledRateLimiting_PassesThrough() throws Exception {
        RateLimitProperties disabledProps = new RateLimitProperties(
                false, false, null, null, null, null
        );
        RateLimitingFilter disabledFilter = new RateLimitingFilter(rateLimiterService, disabledProps);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        disabledFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimiterService, never()).tryAcquire(anyString(), anyInt(), anyInt(), anyBoolean());
    }

    @Test
    @DisplayName("Excluded paths (e.g. /actuator/health) bypass rate limiting")
    void excludedPath_BypassesRateLimiting() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimiterService, never()).tryAcquire(anyString(), anyInt(), anyInt(), anyBoolean());
    }

    @Test
    @DisplayName("Auth endpoint matches POST /api/auth/login and limits by IP")
    void authEndpoint_LimitsByIp() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("203.0.113.195");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimiterService.tryAcquire(eq("auth:ip:203.0.113.195"), eq(10), eq(60), eq(true)))
                .thenReturn(RateLimitResult.allowed(10, 9, 60));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("10");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("9");
        assertThat(response.getHeader("X-RateLimit-Reset")).isEqualTo("60");
    }

    @Test
    @DisplayName("Code execution endpoint matches authenticated user")
    void executionEndpoint_AuthenticatedUser() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user-12345", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/coding-problem/run-preview");
        request.setRemoteAddr("203.0.113.50");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimiterService.tryAcquire(eq("exec:user:user-12345"), eq(5), eq(60), eq(true)))
                .thenReturn(RateLimitResult.allowed(5, 4, 60));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("5");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("4");
    }

    @Test
    @DisplayName("Code execution endpoint falls back to IP when unauthenticated")
    void executionEndpoint_UnauthenticatedFallsBackToIp() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/coding-problem/run-preview");
        request.setRemoteAddr("203.0.113.88");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimiterService.tryAcquire(eq("exec:ip:203.0.113.88"), eq(5), eq(60), eq(true)))
                .thenReturn(RateLimitResult.allowed(5, 4, 60));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Rate limit exceeded returns 429 with Retry-After and does not invoke filter chain")
    void rateLimitExceeded_Returns429() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("203.0.113.195");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimiterService.tryAcquire(anyString(), anyInt(), anyInt(), anyBoolean()))
                .thenReturn(RateLimitResult.denied(10, 42, 60));

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("42");
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("10");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(response.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
        assertThat(response.getContentAsString()).contains("42 seconds");
    }

    @Test
    @DisplayName("When trustForwardHeaders is false, X-Forwarded-For is ignored (spoofing prevented)")
    void spoofedIp_IgnoredWhenTrustForwardHeadersIsFalse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "8.8.8.8, 10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimiterService.tryAcquire(anyString(), anyInt(), anyInt(), anyBoolean()))
                .thenReturn(RateLimitResult.allowed(10, 9, 60));

        filter.doFilter(request, response, filterChain);

        ArgumentCaptor<String> identityCaptor = ArgumentCaptor.forClass(String.class);
        verify(rateLimiterService).tryAcquire(identityCaptor.capture(), anyInt(), anyInt(), anyBoolean());
        assertThat(identityCaptor.getValue()).isEqualTo("auth:ip:10.0.0.1");
    }

    @Test
    @DisplayName("When trustForwardHeaders is true, X-Forwarded-For leftmost IP is used")
    void trustForwardHeaders_UsesClientIp() throws Exception {
        RateLimitProperties trustedProps = new RateLimitProperties(
                true,
                true,
                properties.auth(),
                properties.codeExecution(),
                properties.assessmentIntegrity(),
                properties.defaultApi()
        );
        RateLimitingFilter trustedFilter = new RateLimitingFilter(rateLimiterService, trustedProps);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.77, 10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimiterService.tryAcquire(anyString(), anyInt(), anyInt(), anyBoolean()))
                .thenReturn(RateLimitResult.allowed(10, 9, 60));

        trustedFilter.doFilter(request, response, filterChain);

        ArgumentCaptor<String> identityCaptor = ArgumentCaptor.forClass(String.class);
        verify(rateLimiterService).tryAcquire(identityCaptor.capture(), anyInt(), anyInt(), anyBoolean());
        assertThat(identityCaptor.getValue()).isEqualTo("auth:ip:203.0.113.77");
    }
}

