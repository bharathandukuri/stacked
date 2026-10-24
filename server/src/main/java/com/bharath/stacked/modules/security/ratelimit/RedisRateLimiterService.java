package com.bharath.stacked.modules.security.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisRateLimiterService implements RateLimiterService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String LUA_SCRIPT = """
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local limit = tonumber(ARGV[3])
            local ttl = tonumber(ARGV[4])
            local memberId = ARGV[5]

            local clearBefore = now - window
            redis.call('ZREMRANGEBYSCORE', key, '-inf', clearBefore)

            local currentCount = redis.call('ZCARD', key)

            if currentCount < limit then
                redis.call('ZADD', key, now, memberId)
                redis.call('EXPIRE', key, ttl)
                return {1, limit - currentCount - 1, 0, ttl}
            else
                local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
                local retryAfter = 1
                if #oldest >= 2 then
                    local oldestScore = tonumber(oldest[2])
                    local waitMs = (oldestScore + window) - now
                    if waitMs > 0 then
                        retryAfter = math.ceil(waitMs / 1000)
                    end
                end
                return {0, 0, retryAfter, ttl}
            end
            """;

    @SuppressWarnings("rawtypes")
    private static final DefaultRedisScript<List> SCRIPT;

    static {
        SCRIPT = new DefaultRedisScript<>();
        SCRIPT.setScriptText(LUA_SCRIPT);
        SCRIPT.setResultType(List.class);
    }

    @Override
    @NonNull
    public RateLimitResult tryAcquire(
            @NonNull String key,
            int limit,
            int windowSeconds,
            boolean failClosed) {
        String redisKey = "ratelimit:" + key;
        long now = System.currentTimeMillis();
        long windowMs = windowSeconds * 1000L;
        long ttlSeconds = windowSeconds + 2;
        String memberId = now + "-" + UUID.randomUUID().toString();

        try {
            @SuppressWarnings("unchecked")
            List<Long> result = stringRedisTemplate.execute(
                    SCRIPT,
                    List.of(redisKey),
                    String.valueOf(now),
                    String.valueOf(windowMs),
                    String.valueOf(limit),
                    String.valueOf(ttlSeconds),
                    memberId
            );

            if (result != null && !result.isEmpty()) {
                boolean allowed = (result.get(0) != null && result.get(0) == 1L);
                long remaining = (result.size() > 1 && result.get(1) != null) ? result.get(1) : 0L;
                long retryAfter = (result.size() > 2 && result.get(2) != null) ? result.get(2) : windowSeconds;
                long resetSeconds = (result.size() > 3 && result.get(3) != null) ? result.get(3) : ttlSeconds;

                if (allowed) {
                    return RateLimitResult.allowed(limit, remaining, resetSeconds);
                } else {
                    return RateLimitResult.denied(limit, retryAfter, resetSeconds);
                }
            }
        } catch (Exception ex) {
            log.error("Redis rate limiter failed for key [{}]: {}. Fail-closed={}", redisKey, ex.getMessage(), failClosed);
            if (failClosed) {
                return RateLimitResult.denied(limit, windowSeconds, windowSeconds);
            } else {
                return RateLimitResult.allowed(limit, limit - 1, windowSeconds);
            }
        }

        return failClosed
                ? RateLimitResult.denied(limit, windowSeconds, windowSeconds)
                : RateLimitResult.allowed(limit, limit - 1, windowSeconds);
    }
}

