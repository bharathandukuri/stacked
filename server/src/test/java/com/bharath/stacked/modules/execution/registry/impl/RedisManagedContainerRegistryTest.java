package com.bharath.stacked.modules.execution.registry.impl;

import com.bharath.stacked.modules.execution.dto.ManagedContainer;
import com.bharath.stacked.service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisManagedContainerRegistry Unit Tests")
class RedisManagedContainerRegistryTest {

    @Mock
    private RedisService redisService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @InjectMocks
    private RedisManagedContainerRegistry registry;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    @DisplayName("register successfully saves container in Redis and records timestamp score in ZSet")
    void registerSuccess() {
        Instant now = Instant.now();
        ManagedContainer container = new ManagedContainer("cid-101", "container-101", "execution/isolate:1.0", now);

        registry.register(container);

        verify(redisService).set("stacked:execution:containers:cid-101", container);
        verify(zSetOperations).add("stacked:execution:containers:created-at", "cid-101", (double) now.toEpochMilli());
    }

    @Test
    @DisplayName("register rolls back Redis metadata if ZSet indexing fails")
    void registerRollsBackOnZSetError() {
        Instant now = Instant.now();
        ManagedContainer container = new ManagedContainer("cid-err", "container-err", "execution/isolate:1.0", now);

        doThrow(new RuntimeException("ZSet write failure"))
                .when(zSetOperations).add(anyString(), anyString(), anyDouble());

        assertThatThrownBy(() -> registry.register(container))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ZSet write failure");

        verify(redisService).set("stacked:execution:containers:cid-err", container);
        verify(redisService).delete("stacked:execution:containers:cid-err");
    }

    @Test
    @DisplayName("isManaged delegates to RedisService hasKey")
    void isManagedDelegatesToRedis() {
        when(redisService.hasKey("stacked:execution:containers:cid-chk")).thenReturn(true);
        when(redisService.hasKey("stacked:execution:containers:cid-absent")).thenReturn(false);

        assertThat(registry.isManaged("cid-chk")).isTrue();
        assertThat(registry.isManaged("cid-absent")).isFalse();
    }

    @Test
    @DisplayName("get retrieves ManagedContainer from Redis by container ID")
    void getRetrievesContainer() {
        ManagedContainer container = new ManagedContainer("cid-find", "c-find", "img:1", Instant.now());
        when(redisService.get("stacked:execution:containers:cid-find", ManagedContainer.class)).thenReturn(container);

        ManagedContainer result = registry.get("cid-find");

        assertThat(result).isNotNull();
        assertThat(result.containerId()).isEqualTo("cid-find");
        assertThat(result.containerName()).isEqualTo("c-find");
    }

    @Test
    @DisplayName("unregister deletes container metadata and removes ID from ZSet index")
    void unregisterDeletesFromRedisAndZSet() {
        registry.unregister("cid-del");

        verify(redisService).delete("stacked:execution:containers:cid-del");
        verify(zSetOperations).remove("stacked:execution:containers:created-at", "cid-del");
    }

    @Test
    @DisplayName("findCreatedBefore queries ZSet by score and maps found IDs to ManagedContainers")
    void findCreatedBeforeQueriesAndFilters() {
        Instant cutoff = Instant.now();
        when(zSetOperations.rangeByScore("stacked:execution:containers:created-at", 0, cutoff.toEpochMilli()))
                .thenReturn(Set.of("cid-1", "cid-2", "cid-stale"));

        ManagedContainer c1 = new ManagedContainer("cid-1", "name-1", "img:1", cutoff.minusSeconds(100));
        ManagedContainer c2 = new ManagedContainer("cid-2", "name-2", "img:2", cutoff.minusSeconds(200));

        when(redisService.get("stacked:execution:containers:cid-1", ManagedContainer.class)).thenReturn(c1);
        when(redisService.get("stacked:execution:containers:cid-2", ManagedContainer.class)).thenReturn(c2);
        when(redisService.get("stacked:execution:containers:cid-stale", ManagedContainer.class)).thenReturn(null);

        List<ManagedContainer> expired = registry.findCreatedBefore(cutoff);

        assertThat(expired).containsExactlyInAnyOrder(c1, c2);
    }

    @Test
    @DisplayName("findCreatedBefore returns empty list when no expired IDs in ZSet")
    void findCreatedBeforeReturnsEmptyWhenNoneFound() {
        Instant cutoff = Instant.now();
        when(zSetOperations.rangeByScore("stacked:execution:containers:created-at", 0, cutoff.toEpochMilli()))
                .thenReturn(Set.of());

        List<ManagedContainer> expired = registry.findCreatedBefore(cutoff);

        assertThat(expired).isEmpty();
        verify(redisService, never()).get(anyString(), any());
    }
}
