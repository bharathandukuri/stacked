package com.bharath.stacked.common.cache;

import com.bharath.stacked.config.cache.CacheProperties;
import com.bharath.stacked.service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StackedCacheServiceTest {

    @Mock
    private RedisService redisService;

    private CacheProperties enabledProperties;
    private CacheProperties disabledProperties;
    private StackedCacheService cacheService;

    @BeforeEach
    void setUp() {
        enabledProperties = new CacheProperties(
                true,
                Duration.ofMinutes(5),
                Duration.ofMinutes(2),
                Duration.ofMinutes(10),
                Duration.ofMinutes(30),
                Duration.ofHours(1),
                Duration.ofMinutes(5)
        );
        disabledProperties = new CacheProperties(
                false,
                Duration.ofMinutes(5),
                Duration.ofMinutes(2),
                Duration.ofMinutes(10),
                Duration.ofMinutes(30),
                Duration.ofHours(1),
                Duration.ofMinutes(5)
        );
        cacheService = new StackedCacheServiceImpl(redisService, enabledProperties);
    }

    @Test
    @DisplayName("Cache hit returns cached object and avoids computer invocation")
    void cacheHitReturnsCachedValue() {
        when(redisService.get("cache:test:1", String.class)).thenReturn("cached-data");

        AtomicInteger computerInvocations = new AtomicInteger(0);
        String result = cacheService.getOrCompute(
                "cache:test:1",
                String.class,
                Duration.ofMinutes(5),
                () -> {
                    computerInvocations.incrementAndGet();
                    return "computed-data";
                }
        );

        assertThat(result).isEqualTo("cached-data");
        assertThat(computerInvocations.get()).isZero();
        verify(redisService, never()).set(any(), any(), any());
    }

    @Test
    @DisplayName("Cache miss invokes computer and caches the result with requested TTL")
    void cacheMissComputesAndStores() {
        when(redisService.get("cache:test:2", String.class)).thenReturn(null);

        AtomicInteger computerInvocations = new AtomicInteger(0);
        String result = cacheService.getOrCompute(
                "cache:test:2",
                String.class,
                Duration.ofMinutes(15),
                () -> {
                    computerInvocations.incrementAndGet();
                    return "freshly-computed";
                }
        );

        assertThat(result).isEqualTo("freshly-computed");
        assertThat(computerInvocations.get()).isEqualTo(1);
        verify(redisService).set("cache:test:2", "freshly-computed", Duration.ofMinutes(15));
    }

    @Test
    @DisplayName("Disabled cache bypasses Redis and always calls computer")
    void disabledCacheBypassesRedis() {
        StackedCacheService disabledService = new StackedCacheServiceImpl(redisService, disabledProperties);

        AtomicInteger computerInvocations = new AtomicInteger(0);
        String result = disabledService.getOrCompute(
                "cache:test:3",
                String.class,
                Duration.ofMinutes(5),
                () -> {
                    computerInvocations.incrementAndGet();
                    return "direct-computation";
                }
        );

        assertThat(result).isEqualTo("direct-computation");
        assertThat(computerInvocations.get()).isEqualTo(1);
        verifyNoInteractions(redisService);
    }

    @Test
    @DisplayName("Fail-open: Redis connection failure does not throw and safely falls back to computer")
    void failOpenOnRedisError() {
        when(redisService.get("cache:test:fail", String.class))
                .thenThrow(new RuntimeException("Redis connection refused"));

        AtomicInteger computerInvocations = new AtomicInteger(0);
        String result = cacheService.getOrCompute(
                "cache:test:fail",
                String.class,
                Duration.ofMinutes(5),
                () -> {
                    computerInvocations.incrementAndGet();
                    return "fallback-result";
                }
        );

        assertThat(result).isEqualTo("fallback-result");
        assertThat(computerInvocations.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("TypeReference generic lookups cache and deserialize correctly")
    void typeReferenceCaching() {
        List<String> cachedList = List.of("alpha", "beta", "gamma");
        TypeReference<List<String>> typeRef = new TypeReference<>() {};
        when(redisService.getByType("cache:list", typeRef)).thenReturn(cachedList);

        List<String> result = cacheService.getOrCompute(
                "cache:list",
                typeRef,
                Duration.ofHours(1),
                () -> List.of("fresh")
        );

        assertThat(result).containsExactly("alpha", "beta", "gamma");
    }

    @Test
    @DisplayName("Eviction removes single and multiple keys safely")
    void evictionRemovesKeys() {
        cacheService.evict("cache:test:single");
        verify(redisService).delete("cache:test:single");

        List<String> multiple = List.of("cache:1", "cache:2", "cache:3");
        cacheService.evict(multiple);
        verify(redisService).delete(multiple);
    }
}

