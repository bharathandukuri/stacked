package com.bharath.stacked.common.cache;

import tools.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * High-level, resilient caching abstraction for the Stacked platform.
 *
 * <p>Provides fail-open read semantics so that Redis transient errors or network partitions
 * never make healthy business endpoints unavailable, while supporting typed generics and
 * structured key invalidation.</p>
 */
public interface StackedCacheService {

    /**
     * Check if caching is globally enabled.
     */
    boolean isEnabled();

    /**
     * Fetch a cached value, or compute and cache it if absent or expired.
     *
     * @param key cache key
     * @param targetClass expected return type
     * @param ttl time to live
     * @param computer computation fallback function
     * @return cached or newly computed value
     */
    <T> T getOrCompute(String key, Class<T> targetClass, Duration ttl, Supplier<T> computer);

    /**
     * Fetch a cached generic value (e.g. PageResponse, List), or compute and cache it if absent.
     *
     * @param key cache key
     * @param typeReference Jackson TypeReference for generic type resolution
     * @param ttl time to live
     * @param computer computation fallback function
     * @return cached or newly computed value
     */
    <T> T getOrCompute(String key, TypeReference<T> typeReference, Duration ttl, Supplier<T> computer);

    /**
     * Retrieve an optional value from cache.
     */
    <T> Optional<T> get(String key, Class<T> targetClass);

    /**
     * Retrieve an optional generic value from cache.
     */
    <T> Optional<T> get(String key, TypeReference<T> typeReference);

    /**
     * Store an entry in cache with TTL.
     */
    void put(String key, Object value, Duration ttl);

    /**
     * Evict a single cache key.
     */
    void evict(String key);

    /**
     * Evict multiple cache keys.
     */
    void evict(Collection<String> keys);
}

