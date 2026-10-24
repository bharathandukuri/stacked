package com.bharath.stacked.common.cache;

import com.bharath.stacked.config.cache.CacheProperties;
import com.bharath.stacked.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class StackedCacheServiceImpl implements StackedCacheService {

    private final RedisService redisService;
    private final CacheProperties cacheProperties;

    @Override
    public boolean isEnabled() {
        return cacheProperties.enabled();
    }

    @Override
    public <T> T getOrCompute(String key, Class<T> targetClass, Duration ttl, Supplier<T> computer) {
        if (!isEnabled()) {
            return computer.get();
        }

        try {
            T cached = redisService.get(key, targetClass);
            if (cached != null) {
                log.trace("Cache HIT for key [{}]", key);
                return cached;
            }
        } catch (Exception ex) {
            log.warn("Redis read failed for key [{}], failing open to primary source: {}", key, ex.getMessage());
        }

        log.trace("Cache MISS for key [{}]", key);
        T computed = computer.get();
        if (computed != null) {
            safePut(key, computed, ttl);
        }
        return computed;
    }

    @Override
    public <T> T getOrCompute(String key, TypeReference<T> typeReference, Duration ttl, Supplier<T> computer) {
        if (!isEnabled()) {
            return computer.get();
        }

        try {
            T cached = redisService.getByType(key, typeReference);
            if (cached != null) {
                log.trace("Cache HIT for key [{}] (TypeReference)", key);
                return cached;
            }
        } catch (Exception ex) {
            log.warn("Redis read failed for key [{}], failing open to primary source: {}", key, ex.getMessage());
        }

        log.trace("Cache MISS for key [{}] (TypeReference)", key);
        T computed = computer.get();
        if (computed != null) {
            safePut(key, computed, ttl);
        }
        return computed;
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> targetClass) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(redisService.get(key, targetClass));
        } catch (Exception ex) {
            log.warn("Redis read failed for key [{}]: {}", key, ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public <T> Optional<T> get(String key, TypeReference<T> typeReference) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(redisService.getByType(key, typeReference));
        } catch (Exception ex) {
            log.warn("Redis read failed for key [{}]: {}", key, ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        if (!isEnabled() || value == null) {
            return;
        }
        safePut(key, value, ttl);
    }

    @Override
    public void evict(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            redisService.delete(key);
            log.debug("Evicted cache key [{}]", key);
        } catch (Exception ex) {
            log.warn("Failed to evict Redis cache key [{}]: {}", key, ex.getMessage());
        }
    }

    @Override
    public void evict(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        try {
            redisService.delete(keys);
            log.debug("Evicted {} cache keys", keys.size());
        } catch (Exception ex) {
            log.warn("Failed to evict Redis cache keys [count={}]: {}", keys.size(), ex.getMessage());
        }
    }

    private void safePut(String key, Object value, Duration ttl) {
        try {
            Duration effectiveTtl = (ttl != null && !ttl.isNegative() && !ttl.isZero())
                    ? ttl
                    : cacheProperties.defaultTtl();
            redisService.set(key, value, effectiveTtl);
            log.trace("Cached key [{}] with TTL {}", key, effectiveTtl);
        } catch (Exception ex) {
            log.warn("Failed to write to Redis cache for key [{}]: {}", key, ex.getMessage());
        }
    }
}
