package com.bharath.stacked.modules.security.ratelimit;

import org.jspecify.annotations.NonNull;

public interface RateLimiterService {

    /**
     * Check and consume rate limit quota for the given key and policy parameters.
     *
     * @param key unique identity key (e.g. "auth:ip:127.0.0.1")
     * @param limit maximum allowed requests in the sliding window
     * @param windowSeconds sliding window duration in seconds
     * @param failClosed true to reject when Redis is unavailable, false to allow
     * @return rate limit evaluation result
     */
    @NonNull
    RateLimitResult tryAcquire(@NonNull String key, int limit, int windowSeconds, boolean failClosed);
}

