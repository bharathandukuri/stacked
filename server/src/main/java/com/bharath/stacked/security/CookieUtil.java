package com.bharath.stacked.security;

import com.bharath.stacked.config.CookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Component
public class CookieUtil {

    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    private final CookieProperties cookieProperties;

    public CookieUtil(@NonNull CookieProperties cookieProperties) {
        this.cookieProperties = cookieProperties;
    }

    /**
     * Create secure HTTP-only cookie containing the refresh token.
     */
    @NonNull
    public ResponseCookie createRefreshTokenCookie(@NonNull String refreshToken, @NonNull Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(cookieProperties.httpOnly())
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path(cookieProperties.path())
                .maxAge(maxAge);

        if (cookieProperties.domain() != null && !cookieProperties.domain().isBlank()) {
            builder.domain(cookieProperties.domain().trim());
        }

        return builder.build();
    }

    /**
     * Create an expiration cookie to clear the refresh token on logout.
     */
    @NonNull
    public ResponseCookie deleteRefreshTokenCookie() {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(cookieProperties.httpOnly())
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path(cookieProperties.path())
                .maxAge(0);

        if (cookieProperties.domain() != null && !cookieProperties.domain().isBlank()) {
            builder.domain(cookieProperties.domain().trim());
        }

        return builder.build();
    }

    /**
     * Extract the refresh token value from HttpServletRequest cookies.
     */
    @NonNull
    public Optional<String> extractRefreshToken(@Nullable HttpServletRequest request) {
        if (request == null) {
            return Optional.empty();
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(c -> c != null && REFRESH_TOKEN_COOKIE_NAME.equals(c.getName()))
                .map(c -> c.getValue())
                .findFirst();
    }
}
