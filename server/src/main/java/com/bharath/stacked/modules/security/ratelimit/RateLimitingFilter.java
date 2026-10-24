package com.bharath.stacked.modules.security.ratelimit;

import com.bharath.stacked.common.api.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final RateLimitProperties rateLimitProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final List<String> EXCLUDED_PATHS = List.of(
            "/actuator/**",
            "/ws/**",
            "/api/files/*/content",
            "/api/coding-problem/supported-languages",
            "/api/coding-problem/languages",
            "/api/admin/coding-problem/supported-languages",
            "/api/admin/coding-problem/languages"
    );

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (!rateLimitProperties.enabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String uri = request.getRequestURI();
        String method = request.getMethod();

        // Skip excluded endpoints
        for (String excluded : EXCLUDED_PATHS) {
            if (pathMatcher.match(excluded, uri)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        // Match against policies in priority order
        MatchedPolicy matched = matchPolicy(method, uri, request);
        if (matched == null) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitProperties.PolicyConfig config = matched.config();
        RateLimitResult result = rateLimiterService.tryAcquire(
                matched.identityKey(),
                config.limit(),
                config.windowSeconds(),
                config.failClosed()
        );

        // Always attach rate limit headers
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(result.resetSeconds()));

        if (!result.allowed()) {
            log.warn("Rate limit exceeded for [{}] on [{} {}]: retry after {}s",
                    matched.identityKey(), method, uri, result.retryAfterSeconds());

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(result.retryAfterSeconds()));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            String errorMessage = String.format("Rate limit exceeded. Please try again in %d seconds.", result.retryAfterSeconds());
            String json = String.format(
                    "{\"success\":false,\"message\":\"%s\",\"error\":{\"errorCode\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"},\"timestamp\":\"%s\"}",
                    escapeJson(errorMessage),
                    escapeJson(ErrorCode.RATE_LIMIT_EXCEEDED.name()),
                    escapeJson(errorMessage),
                    escapeJson(uri),
                    Instant.now().toString());

            response.getWriter().write(json);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private MatchedPolicy matchPolicy(String method, String uri, HttpServletRequest request) {
        // 1. Authentication endpoints (IP-based abuse prevention)
        RateLimitProperties.PolicyConfig authConfig = rateLimitProperties.auth();
        if (authConfig != null && matchesAny(authConfig.paths(), method, uri)) {
            String clientIp = getClientIp(request);
            return new MatchedPolicy("auth:ip:" + clientIp, authConfig);
        }

        // 2. Code execution endpoints (User-based or IP-based)
        RateLimitProperties.PolicyConfig execConfig = rateLimitProperties.codeExecution();
        if (execConfig != null && matchesAny(execConfig.paths(), method, uri)) {
            String identity = resolveUserOrIp("exec", request);
            return new MatchedPolicy(identity, execConfig);
        }

        // 3. Assessment integrity endpoints
        RateLimitProperties.PolicyConfig assessmentConfig = rateLimitProperties.assessmentIntegrity();
        if (assessmentConfig != null && matchesAny(assessmentConfig.paths(), method, uri)) {
            String identity = resolveUserOrIp("assessment", request);
            return new MatchedPolicy(identity, assessmentConfig);
        }

        // 4. Default API endpoints
        RateLimitProperties.PolicyConfig defaultApiConfig = rateLimitProperties.defaultApi();
        if (defaultApiConfig != null && matchesAny(defaultApiConfig.paths(), method, uri)) {
            String identity = resolveUserOrIp("api", request);
            return new MatchedPolicy(identity, defaultApiConfig);
        }

        return null;
    }

    private boolean matchesAny(List<String> rules, String method, String uri) {
        if (rules == null || rules.isEmpty()) {
            return false;
        }
        for (String rule : rules) {
            if (rule.contains(":")) {
                String[] parts = rule.split(":", 2);
                String requiredMethod = parts[0];
                String pattern = parts[1];
                if (requiredMethod.equalsIgnoreCase(method) && pathMatcher.match(pattern, uri)) {
                    return true;
                }
            } else if (pathMatcher.match(rule, uri)) {
                return true;
            }
        }
        return false;
    }

    private String resolveUserOrIp(String prefix, HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
            return prefix + ":user:" + auth.getName();
        }
        return prefix + ":ip:" + getClientIp(request);
    }

    public String getClientIp(HttpServletRequest request) {
        if (rateLimitProperties.trustForwardHeaders()) {
            String xff = request.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(xff)) {
                String clientIp = xff.split(",")[0].trim();
                if (StringUtils.hasText(clientIp)) {
                    return clientIp;
                }
            }
        }
        String remoteAddr = request.getRemoteAddr();
        return StringUtils.hasText(remoteAddr) ? remoteAddr : "unknown";
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private record MatchedPolicy(String identityKey, RateLimitProperties.PolicyConfig config) {
    }
}

