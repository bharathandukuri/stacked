package com.bharath.stacked.service.impl;

import com.bharath.stacked.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.error("Failed to set Redis key [{}]: {}", key, e.getMessage());
            throw e;
        }
    }

    @Override
    public void set(String key, Object value, Duration timeout) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout);
        } catch (Exception e) {
            log.error("Failed to set Redis key [{}] with TTL: {}", key, e.getMessage());
            throw e;
        }
    }

    @Override
    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Failed to get Redis key [{}]: {}", key, e.getMessage());
            return null;
        }
    }

    @Override
    public <T> T get(String key, Class<T> targetClass) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            if (targetClass.isInstance(value)) {
                return targetClass.cast(value);
            }
            return objectMapper.convertValue(value, targetClass);
        } catch (Exception e) {
            log.error("Failed to convert Redis key [{}] to {}: {}", key, targetClass.getSimpleName(), e.getMessage());
            return null;
        }
    }

    @Override
    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Failed to check Redis key [{}]: {}", key, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.delete(key));
        } catch (Exception e) {
            log.error("Failed to delete Redis key [{}]: {}", key, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean expire(String key, Duration timeout) {
        try {
            return Boolean.TRUE.equals(redisTemplate.expire(key, timeout));
        } catch (Exception e) {
            log.error("Failed to set expiration on Redis key [{}]: {}", key, e.getMessage());
            return false;
        }
    }

    @Override
    public Long getExpire(String key) {
        try {
            return redisTemplate.getExpire(key, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Failed to get expiration for Redis key [{}]: {}", key, e.getMessage());
            return -1L;
        }
    }

    @Override
    public boolean ping() {
        try {
            return Boolean.TRUE.equals(redisTemplate.execute((RedisConnection connection) -> {
                String ping = connection.ping();
                return "PONG".equalsIgnoreCase(ping);
            }));
        } catch (Exception e) {
            log.warn("Redis ping failed: {}", e.getMessage());
            return false;
        }
    }
}
