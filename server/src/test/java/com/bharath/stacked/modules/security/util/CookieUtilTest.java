package com.bharath.stacked.modules.security.util;

import com.bharath.stacked.modules.security.config.CookieProperties;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CookieUtil Unit Tests")
class CookieUtilTest {

    @Test
    @DisplayName("createRefreshTokenCookie creates httpOnly, secure, configured cookie")
    void createRefreshTokenCookie() {
        CookieProperties properties = new CookieProperties(true, true, "strict", "example.com", "/api");
        CookieUtil cookieUtil = new CookieUtil(properties);

        ResponseCookie cookie = cookieUtil.createRefreshTokenCookie("sample-refresh-token", Duration.ofDays(7));

        assertThat(cookie.getName()).isEqualTo(CookieUtil.REFRESH_TOKEN_COOKIE_NAME);
        assertThat(cookie.getValue()).isEqualTo("sample-refresh-token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("strict");
        assertThat(cookie.getPath()).isEqualTo("/api");
        assertThat(cookie.getDomain()).isEqualTo("example.com");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofDays(7));
    }

    @Test
    @DisplayName("createRefreshTokenCookie omits domain when null or blank")
    void createRefreshTokenCookieWithoutDomain() {
        CookieProperties properties = new CookieProperties(true, false, "lax", "  ", "/");
        CookieUtil cookieUtil = new CookieUtil(properties);

        ResponseCookie cookie = cookieUtil.createRefreshTokenCookie("token123", Duration.ofHours(1));

        assertThat(cookie.getDomain()).isNull();
        assertThat(cookie.isSecure()).isFalse();
    }

    @Test
    @DisplayName("deleteRefreshTokenCookie creates expired cookie with maxAge 0 and empty value")
    void deleteRefreshTokenCookie() {
        CookieProperties properties = new CookieProperties(true, true, "none", "stacked.com", "/");
        CookieUtil cookieUtil = new CookieUtil(properties);

        ResponseCookie cookie = cookieUtil.deleteRefreshTokenCookie();

        assertThat(cookie.getName()).isEqualTo(CookieUtil.REFRESH_TOKEN_COOKIE_NAME);
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
        assertThat(cookie.getDomain()).isEqualTo("stacked.com");
    }

    @Test
    @DisplayName("extractRefreshToken handles null request, null cookies, and missing refreshToken")
    void extractRefreshTokenEdgeCases() {
        CookieProperties properties = new CookieProperties(true, false, "lax", null, "/");
        CookieUtil cookieUtil = new CookieUtil(properties);

        assertThat(cookieUtil.extractRefreshToken(null)).isEmpty();

        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThat(cookieUtil.extractRefreshToken(request)).isEmpty();

        request.setCookies(new Cookie("otherCookie", "value123"));
        assertThat(cookieUtil.extractRefreshToken(request)).isEmpty();
    }

    @Test
    @DisplayName("extractRefreshToken extracts refreshToken cookie value when present")
    void extractRefreshTokenSuccess() {
        CookieProperties properties = new CookieProperties(true, false, "lax", null, "/");
        CookieUtil cookieUtil = new CookieUtil(properties);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(
                new Cookie("session", "xyz"),
                new Cookie(CookieUtil.REFRESH_TOKEN_COOKIE_NAME, "valid-refresh-token")
        );

        Optional<String> token = cookieUtil.extractRefreshToken(request);

        assertThat(token).isPresent().contains("valid-refresh-token");
    }
}
