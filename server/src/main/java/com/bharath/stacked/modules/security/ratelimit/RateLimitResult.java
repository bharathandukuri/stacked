package com.bharath.stacked.modules.security.ratelimit;

import org.jspecify.annotations.NonNull;

public record RateLimitResult(
        boolean allowed,
        long limit,
        long remaining,
        long retryAfterSeconds,
        long resetSeconds) {

    @NonNull
    public static RateLimitResult allowed(long limit, long remaining, long resetSeconds) {
        return new RateLimitResult(true, limit, Math.max(0, remaining), 0, resetSeconds);
    }

    @NonNull
    public static RateLimitResult denied(long limit, long retryAfterSeconds, long resetSeconds) {
        return new RateLimitResult(false, limit, 0, Math.max(1, retryAfterSeconds), resetSeconds);
    }
}

