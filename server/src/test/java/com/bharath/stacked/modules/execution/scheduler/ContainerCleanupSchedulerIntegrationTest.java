package com.bharath.stacked.modules.execution.scheduler;

import com.bharath.stacked.AbstractIntegrationTest;
import com.bharath.stacked.modules.execution.dto.ManagedContainer;
import com.bharath.stacked.modules.execution.exception.DockerContainerException;
import com.bharath.stacked.modules.execution.registry.ManagedContainerRegistry;
import com.bharath.stacked.modules.execution.service.DockerExecutionService;
import com.bharath.stacked.service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ContainerCleanupScheduler Testcontainers Integration Test")
class ContainerCleanupSchedulerIntegrationTest extends AbstractIntegrationTest {

    private static final String CREATED_AT_INDEX = "stacked:execution:containers:created-at";

    @Autowired
    private ManagedContainerRegistry containerRegistry;

    @Autowired
    private RedisService redisService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @MockitoBean
    private DockerExecutionService dockerExecutionService;

    private ContainerCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        stringRedisTemplate.delete(CREATED_AT_INDEX);
        scheduler = new ContainerCleanupScheduler(containerRegistry, dockerExecutionService);
    }

    @Test
    @DisplayName("Should sweep orphan containers older than 30m and keep active containers intact in live Redis")
    void shouldSweepOrphanContainersAndKeepActiveContainersInLiveRedis() {
        Instant now = Instant.now();

        // c1: 45m ago (orphan)
        String c1Id = "orphan-1-" + UUID.randomUUID();
        ManagedContainer c1 = new ManagedContainer(c1Id, "orphan-name-1", "execution/java:21", now.minus(Duration.ofMinutes(45)));

        // c2: 35m ago (orphan)
        String c2Id = "orphan-2-" + UUID.randomUUID();
        ManagedContainer c2 = new ManagedContainer(c2Id, "orphan-name-2", "execution/python:3.12", now.minus(Duration.ofMinutes(35)));

        // c3: 5m ago (active)
        String c3Id = "active-1-" + UUID.randomUUID();
        ManagedContainer c3 = new ManagedContainer(c3Id, "active-name-1", "execution/c:17", now.minus(Duration.ofMinutes(5)));

        containerRegistry.register(c1);
        containerRegistry.register(c2);
        containerRegistry.register(c3);

        assertTrue(containerRegistry.isManaged(c1Id));
        assertTrue(containerRegistry.isManaged(c2Id));
        assertTrue(containerRegistry.isManaged(c3Id));

        // When deleteContainer is invoked, perform the real unregister as DockerExecutionService would
        doAnswer(invocation -> {
            String id = invocation.getArgument(0);
            containerRegistry.unregister(id);
            return null;
        }).when(dockerExecutionService).deleteContainer(anyString());

        // Execute cleanup
        scheduler.cleanupExpiredContainers();

        // In real Redis: orphans c1 and c2 should be removed
        assertFalse(containerRegistry.isManaged(c1Id), "Orphan c1 must be unregistered from live Redis");
        assertFalse(containerRegistry.isManaged(c2Id), "Orphan c2 must be unregistered from live Redis");
        assertNull(containerRegistry.get(c1Id));
        assertNull(containerRegistry.get(c2Id));

        // In real Redis: active c3 must remain completely intact
        assertTrue(containerRegistry.isManaged(c3Id), "Active container c3 must remain managed in live Redis");
        assertNotNull(containerRegistry.get(c3Id));

        // Sorted set index assertions
        assertNull(stringRedisTemplate.opsForZSet().score(CREATED_AT_INDEX, c1Id));
        assertNull(stringRedisTemplate.opsForZSet().score(CREATED_AT_INDEX, c2Id));
        assertNotNull(stringRedisTemplate.opsForZSet().score(CREATED_AT_INDEX, c3Id));

        // Docker interactions
        verify(dockerExecutionService).deleteContainer(c1Id);
        verify(dockerExecutionService).deleteContainer(c2Id);
        verify(dockerExecutionService, never()).deleteContainer(c3Id);

        // Cleanup
        containerRegistry.unregister(c3Id);
    }

    @Test
    @DisplayName("Should isolate failure per container and continue sweeping remaining orphans in live Redis")
    void shouldIsolateFailurePerContainerAndContinueSweepingRemainingInLiveRedis() {
        Instant now = Instant.now();

        String failingId = "failing-" + UUID.randomUUID();
        ManagedContainer failing = new ManagedContainer(failingId, "failing-name", "execution/java:21", now.minus(Duration.ofMinutes(40)));

        String survivingId = "surviving-" + UUID.randomUUID();
        ManagedContainer surviving = new ManagedContainer(survivingId, "surviving-name", "execution/python:3.12", now.minus(Duration.ofMinutes(40)));

        containerRegistry.register(failing);
        containerRegistry.register(surviving);

        // Simulate Docker error when deleting the first container
        doThrow(new DockerContainerException("Simulated Docker failure", null))
                .when(dockerExecutionService).deleteContainer(failingId);

        doAnswer(invocation -> {
            String id = invocation.getArgument(0);
            containerRegistry.unregister(id);
            return null;
        }).when(dockerExecutionService).deleteContainer(survivingId);

        scheduler.cleanupExpiredContainers();

        // Surviving orphan must still be processed and unregistered from live Redis
        assertFalse(containerRegistry.isManaged(survivingId));
        assertNull(containerRegistry.get(survivingId));
        verify(dockerExecutionService).deleteContainer(survivingId);

        // Failing remains in Redis until handled
        assertTrue(containerRegistry.isManaged(failingId));

        // Clean up failing
        containerRegistry.unregister(failingId);
    }

    @Test
    @DisplayName("Should execute safely when no orphan containers exist in live Redis")
    void shouldHandleZeroOrphansInLiveRedis() {
        scheduler.cleanupExpiredContainers();

        verifyNoInteractions(dockerExecutionService);
    }
}
