package com.bharath.stacked.modules.execution.registry.impl;

import com.bharath.stacked.AbstractIntegrationTest;
import com.bharath.stacked.modules.execution.dto.ManagedContainer;
import com.bharath.stacked.modules.execution.registry.ManagedContainerRegistry;
import com.bharath.stacked.service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RedisManagedContainerRegistry Testcontainers Integration Test")
class RedisManagedContainerRegistryIntegrationTest extends AbstractIntegrationTest {

    private static final String KEY_PREFIX = "stacked:execution:containers:";
    private static final String CREATED_AT_INDEX = "stacked:execution:containers:created-at";

    @Autowired
    private ManagedContainerRegistry containerRegistry;

    @Autowired
    private RedisService redisService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void cleanRedisState() {
        // Clean up created-at index and test keys
        stringRedisTemplate.delete(CREATED_AT_INDEX);
    }

    @Test
    @DisplayName("Should successfully register container and persist metadata and sorted set score in live Redis")
    void shouldRegisterContainerInLiveRedis() {
        String containerId = UUID.randomUUID().toString();
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        ManagedContainer container = new ManagedContainer(
                containerId,
                "test-container-" + containerId.substring(0, 8),
                "execution/java:21",
                now);

        containerRegistry.register(container);

        // Verify container is reported as managed
        assertTrue(containerRegistry.isManaged(containerId), "Container should be recognized as managed in live Redis");

        // Verify key exists directly in Redis
        String expectedKey = KEY_PREFIX + containerId;
        assertTrue(redisService.hasKey(expectedKey), "Redis key must exist for registered container");

        // Verify metadata retrieval and accurate deserialization
        ManagedContainer fetched = containerRegistry.get(containerId);
        assertNotNull(fetched, "Fetched container metadata must not be null");
        assertEquals(containerId, fetched.containerId());
        assertEquals(container.containerName(), fetched.containerName());
        assertEquals("execution/java:21", fetched.imageReference());
        assertEquals(now.toEpochMilli(), fetched.createdAt().toEpochMilli());

        // Verify score in the Redis sorted set
        Double score = stringRedisTemplate.opsForZSet().score(CREATED_AT_INDEX, containerId);
        assertNotNull(score, "Container ID must be indexed in sorted set");
        assertEquals((double) now.toEpochMilli(), score, 0.001);

        // Cleanup
        containerRegistry.unregister(containerId);
    }

    @Test
    @DisplayName("Should return null for non-existent container and report isManaged as false")
    void shouldHandleNonExistentContainer() {
        String nonExistentId = "non-existent-" + UUID.randomUUID();

        assertFalse(containerRegistry.isManaged(nonExistentId));
        assertNull(containerRegistry.get(nonExistentId));
    }

    @Test
    @DisplayName("Should completely unregister container from both Redis key and sorted set index")
    void shouldUnregisterContainerFromLiveRedis() {
        String containerId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        ManagedContainer container = new ManagedContainer(
                containerId,
                "test-cleanup-" + containerId.substring(0, 8),
                "execution/python:3.12",
                now);

        containerRegistry.register(container);
        assertTrue(containerRegistry.isManaged(containerId));

        containerRegistry.unregister(containerId);

        assertFalse(containerRegistry.isManaged(containerId), "Container must no longer be managed after unregister");
        assertNull(containerRegistry.get(containerId), "Container metadata must be null after unregister");

        Double score = stringRedisTemplate.opsForZSet().score(CREATED_AT_INDEX, containerId);
        assertNull(score, "Container ID must be removed from sorted set index");
    }

    @Test
    @DisplayName("Should find containers created before cutoff timestamp using real Redis ZSet queries")
    void shouldFindContainersCreatedBeforeCutoff() {
        Instant baseTime = Instant.now();

        // c1: 40 minutes ago
        ManagedContainer c1 = new ManagedContainer(
                "c1-" + UUID.randomUUID(),
                "name-c1",
                "execution/c:17",
                baseTime.minus(Duration.ofMinutes(40)));
        // c2: 20 minutes ago
        ManagedContainer c2 = new ManagedContainer(
                "c2-" + UUID.randomUUID(),
                "name-c2",
                "execution/cpp:23",
                baseTime.minus(Duration.ofMinutes(20)));
        // c3: 5 minutes ago
        ManagedContainer c3 = new ManagedContainer(
                "c3-" + UUID.randomUUID(),
                "name-c3",
                "execution/python:3.12",
                baseTime.minus(Duration.ofMinutes(5)));

        containerRegistry.register(c1);
        containerRegistry.register(c2);
        containerRegistry.register(c3);

        try {
            // Cutoff at 15 minutes ago -> should find c1 and c2, but NOT c3
            Instant cutoff = baseTime.minus(Duration.ofMinutes(15));
            List<ManagedContainer> expired = containerRegistry.findCreatedBefore(cutoff);

            assertThat(expired).hasSize(2);
            List<String> expiredIds = expired.stream().map(ManagedContainer::containerId).toList();
            assertThat(expiredIds).containsExactlyInAnyOrder(c1.containerId(), c2.containerId());

            // Cutoff before c1 -> should find empty list
            Instant oldCutoff = baseTime.minus(Duration.ofHours(2));
            List<ManagedContainer> none = containerRegistry.findCreatedBefore(oldCutoff);
            assertThat(none).isEmpty();

            // Cutoff in future -> should find all 3
            Instant futureCutoff = baseTime.plus(Duration.ofMinutes(10));
            List<ManagedContainer> all = containerRegistry.findCreatedBefore(futureCutoff);
            assertThat(all).hasSize(3);
        } finally {
            containerRegistry.unregister(c1.containerId());
            containerRegistry.unregister(c2.containerId());
            containerRegistry.unregister(c3.containerId());
        }
    }

    @Test
    @DisplayName("Should ignore orphaned sorted set entries if the corresponding key was deleted")
    void shouldHandleDanglingSortedSetEntries() {
        String containerId = "dangling-" + UUID.randomUUID();
        Instant now = Instant.now();
        ManagedContainer container = new ManagedContainer(containerId, "dangling-name", "execution/node:20", now);

        containerRegistry.register(container);

        // Manually delete the container key in Redis, simulating key expiration or
        // out-of-band removal
        redisService.delete(KEY_PREFIX + containerId);

        // findCreatedBefore should gracefully filter out null objects
        List<ManagedContainer> found = containerRegistry.findCreatedBefore(now.plusSeconds(10));
        List<String> foundIds = found.stream().map(ManagedContainer::containerId).toList();
        assertFalse(foundIds.contains(containerId), "Dangling index entry without metadata key must be skipped");

        // Clean up zset
        stringRedisTemplate.opsForZSet().remove(CREATED_AT_INDEX, containerId);
    }

    @Test
    @DisplayName("Should handle concurrent registrations and unregistrations safely against live Redis")
    void shouldHandleConcurrentRegistrationsInLiveRedis() throws InterruptedException {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        Instant now = Instant.now();
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    String id = "concurrent-" + index + "-" + UUID.randomUUID();
                    ManagedContainer c = new ManagedContainer(id, "container-" + index, "execution/java:21", now);
                    containerRegistry.register(c);

                    if (containerRegistry.isManaged(id) && containerRegistry.get(id) != null) {
                        successCount.incrementAndGet();
                    }

                    containerRegistry.unregister(id);
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All concurrent operations must finish within 10s");
        assertEquals(threadCount, successCount.get(), "All concurrent registrations must succeed");
    }
}
