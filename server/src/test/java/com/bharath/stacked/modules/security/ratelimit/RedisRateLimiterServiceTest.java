package com.bharath.stacked.modules.security.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisRateLimiterServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    private RedisRateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RedisRateLimiterService(stringRedisTemplate);
    }

    @Test
    @DisplayName("tryAcquire returns allowed when count is within limit")
    void tryAcquire_WithinLimit_Allowed() {
        when(stringRedisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                anyString(), anyString(), anyString(), anyString(), anyString()
        )).thenReturn(List.of(1L, 4L, 0L, 62L));

        RateLimitResult result = rateLimiterService.tryAcquire("test-key", 5, 60, true);

        assertThat(result.allowed()).isTrue();
        assertThat(result.limit()).isEqualTo(5);
        assertThat(result.remaining()).isEqualTo(4);
        assertThat(result.retryAfterSeconds()).isEqualTo(0);
        assertThat(result.resetSeconds()).isEqualTo(62);
    }

    @Test
    @DisplayName("tryAcquire returns denied when limit is reached")
    void tryAcquire_LimitExceeded_Denied() {
        when(stringRedisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                anyString(), anyString(), anyString(), anyString(), anyString()
        )).thenReturn(List.of(0L, 0L, 25L, 62L));

        RateLimitResult result = rateLimiterService.tryAcquire("test-key", 5, 60, true);

        assertThat(result.allowed()).isFalse();
        assertThat(result.limit()).isEqualTo(5);
        assertThat(result.remaining()).isEqualTo(0);
        assertThat(result.retryAfterSeconds()).isEqualTo(25);
        assertThat(result.resetSeconds()).isEqualTo(62);
    }

    @Test
    @DisplayName("tryAcquire fails closed when Redis throws and failClosed is true")
    void tryAcquire_RedisError_FailClosed() {
        when(stringRedisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                anyString(), anyString(), anyString(), anyString(), anyString()
        )).thenThrow(new RuntimeException("Redis connection refused"));

        RateLimitResult result = rateLimiterService.tryAcquire("test-key", 10, 60, true);

        assertThat(result.allowed()).isFalse();
        assertThat(result.limit()).isEqualTo(10);
        assertThat(result.retryAfterSeconds()).isEqualTo(60);
    }

    @Test
    @DisplayName("tryAcquire fails open when Redis throws and failClosed is false")
    void tryAcquire_RedisError_FailOpen() {
        when(stringRedisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                anyString(), anyString(), anyString(), anyString(), anyString()
        )).thenThrow(new RuntimeException("Redis connection refused"));

        RateLimitResult result = rateLimiterService.tryAcquire("test-key", 10, 60, false);

        assertThat(result.allowed()).isTrue();
        assertThat(result.limit()).isEqualTo(10);
        assertThat(result.remaining()).isEqualTo(9);
    }

    @Test
    @DisplayName("tryAcquire formats redis key with ratelimit: prefix")
    void tryAcquire_FormatsRedisKey() {
        when(stringRedisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                anyString(), anyString(), anyString(), anyString(), anyString()
        )).thenReturn(List.of(1L, 9L, 0L, 62L));

        rateLimiterService.tryAcquire("auth:ip:192.168.1.1", 10, 60, true);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        verify(stringRedisTemplate).execute(
                any(DefaultRedisScript.class),
                keysCaptor.capture(),
                anyString(), anyString(), anyString(), anyString(), anyString()
        );

        assertThat(keysCaptor.getValue()).containsExactly("ratelimit:auth:ip:192.168.1.1");
    }
}

