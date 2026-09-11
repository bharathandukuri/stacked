package com.bharath.stacked.service;

import java.time.Duration;

public interface RedisService {

    /**
     * Store a key-value pair in Redis without expiration.
     */
    void set(String key, Object value);

    /**
     * Store a key-value pair in Redis with a time-to-live (TTL).
     */
    void set(String key, Object value, Duration timeout);

    /**
     * Retrieve a value from Redis as an Object.
     */
    Object get(String key);

    /**
     * Retrieve a value from Redis deserialized to a target class.
     */
    <T> T get(String key, Class<T> targetClass);

    /**
     * Check if a key exists in Redis.
     */
    boolean hasKey(String key);

    /**
     * Delete a key from Redis.
     */
    boolean delete(String key);

    /**
     * Set or update expiration timeout on an existing key.
     */
    boolean expire(String key, Duration timeout);

    /**
     * Get the remaining time-to-live of a key in seconds (-1 if no TTL, -2 if key
     * does not exist).
     */
    Long getExpire(String key);

    /**
     * Ping Redis to verify connectivity.
     */
    boolean ping();
}
