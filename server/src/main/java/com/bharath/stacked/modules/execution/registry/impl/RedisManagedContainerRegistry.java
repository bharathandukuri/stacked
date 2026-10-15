package com.bharath.stacked.modules.execution.registry.impl;

import com.bharath.stacked.modules.execution.dto.ManagedContainer;
import com.bharath.stacked.modules.execution.registry.ManagedContainerRegistry;
import com.bharath.stacked.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RedisManagedContainerRegistry
        implements ManagedContainerRegistry {

    private static final String KEY_PREFIX =
            "stacked:execution:containers:";

    private static final String CREATED_AT_INDEX =
            "stacked:execution:containers:created-at";

    private final RedisService redisService;
    private final StringRedisTemplate stringRedisTemplate;

    private String key(String containerId) {
        return KEY_PREFIX + containerId;
    }

    @Override
    public void register(ManagedContainer container) {
        redisService.set(key(container.containerId()), container);

        try {
            stringRedisTemplate.opsForZSet().add(
                    CREATED_AT_INDEX,
                    container.containerId(),
                    container.createdAt().toEpochMilli()
            );
        } catch (Exception e) {
            // Roll back the metadata registration if indexing fails.
            redisService.delete(key(container.containerId()));
            throw e;
        }
    }

    @Override
    public boolean isManaged(String containerId) {
        return redisService.hasKey(key(containerId));
    }

    @Override
    public ManagedContainer get(String containerId) {
        return redisService.get(
                key(containerId),
                ManagedContainer.class
        );
    }

    @Override
    public void unregister(String containerId) {
        redisService.delete(key(containerId));

        stringRedisTemplate.opsForZSet().remove(
                CREATED_AT_INDEX,
                containerId
        );
    }

    @Override
    public List<ManagedContainer> findCreatedBefore(Instant cutoff) {
        Set<String> containerIds =
                stringRedisTemplate.opsForZSet().rangeByScore(
                        CREATED_AT_INDEX,
                        0,
                        cutoff.toEpochMilli()
                );

        if (containerIds == null || containerIds.isEmpty()) {
            return List.of();
        }

        return containerIds.stream()
                .map(this::get)
                .filter(Objects::nonNull)
                .toList();
    }
}