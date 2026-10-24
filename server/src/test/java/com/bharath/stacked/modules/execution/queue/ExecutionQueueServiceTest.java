package com.bharath.stacked.modules.execution.queue;

import com.bharath.stacked.modules.execution.config.ExecutionProperties;
import com.bharath.stacked.modules.execution.exception.ExecutionQueueFullException;
import com.bharath.stacked.modules.execution.exception.ExecutionQueueTimeoutException;
import com.bharath.stacked.modules.execution.queue.impl.ExecutionQueueServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExecutionQueueService Unit Tests")
class ExecutionQueueServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    private ExecutionProperties properties;
    private ExecutionQueueServiceImpl queueService;

    @BeforeEach
    void setUp() {
        properties = new ExecutionProperties();
        properties.getQueue().setEnabled(true);
        properties.getQueue().setMaxConcurrentWorkers(2);
        properties.getQueue().setMaxQueueCapacity(2);
        properties.getQueue().setQueueWaitTimeoutMs(500L);
        properties.getQueue().setMaxConcurrentPerUser(2);
        properties.getQueue().setDistributedRedisCheck(false);

        queueService = new ExecutionQueueServiceImpl(properties, null);
    }

    @AfterEach
    void tearDown() {
        if (queueService != null) {
            queueService.shutdown();
        }
    }

    @Test
    @DisplayName("Task executes and returns result successfully")
    void execute_Success_ReturnsResult() {
        String result = queueService.execute("user-1", () -> "Hello, World!");
        assertThat(result).isEqualTo("Hello, World!");
    }

    @Test
    @DisplayName("Active concurrent workers never exceed configured maximum")
    void concurrency_DoesNotExceedMaxWorkers() throws Exception {
        int taskCount = 6;
        AtomicInteger runningWorkers = new AtomicInteger(0);
        AtomicInteger maxObservedWorkers = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(taskCount);

        ExecutorService clientThreads = Executors.newFixedThreadPool(taskCount);

        for (int i = 0; i < taskCount; i++) {
            clientThreads.submit(() -> {
                try {
                    queueService.execute(null, () -> {
                        int current = runningWorkers.incrementAndGet();
                        maxObservedWorkers.accumulateAndGet(current, Math::max);
                        Thread.sleep(100);
                        runningWorkers.decrementAndGet();
                        return null;
                    });
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        clientThreads.shutdown();

        assertThat(maxObservedWorkers.get()).isLessThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Deterministic rejection when waiting queue capacity is exceeded")
    void queue_Rejection_WhenCapacityExceeded() throws Exception {
        CountDownLatch blockWorkersLatch = new CountDownLatch(1);
        CountDownLatch startedLatch = new CountDownLatch(2);

        ExecutorService clientThreads = Executors.newFixedThreadPool(5);

        // Occupy 2 workers
        for (int i = 0; i < 2; i++) {
            clientThreads.submit(() -> {
                queueService.execute(null, () -> {
                    startedLatch.countDown();
                    blockWorkersLatch.await(5, TimeUnit.SECONDS);
                    return null;
                });
                return null;
            });
        }

        startedLatch.await(2, TimeUnit.SECONDS);

        // Fill queue with 2 tasks
        for (int i = 0; i < 2; i++) {
            clientThreads.submit(() -> {
                queueService.execute(null, () -> {
                    blockWorkersLatch.await(5, TimeUnit.SECONDS);
                    return null;
                });
                return null;
            });
        }

        // Small pause for queue placement
        Thread.sleep(50);

        // 5th task must be rejected immediately with ExecutionQueueFullException
        assertThatThrownBy(() -> queueService.execute(null, () -> "overflow"))
                .isInstanceOf(ExecutionQueueFullException.class)
                .hasMessageContaining("Execution queue is at maximum capacity");

        blockWorkersLatch.countDown();
        clientThreads.shutdown();
    }

    @Test
    @DisplayName("Per-user concurrency limit rejects excessive requests for same user")
    void perUserLimit_EnforcesUserLimit() throws Exception {
        CountDownLatch blockWorkersLatch = new CountDownLatch(1);
        CountDownLatch startedLatch = new CountDownLatch(2);

        ExecutorService clientThreads = Executors.newFixedThreadPool(3);

        // User "alice" starts 2 concurrent tasks (maxConcurrentPerUser = 2)
        for (int i = 0; i < 2; i++) {
            clientThreads.submit(() -> {
                queueService.execute("alice", () -> {
                    startedLatch.countDown();
                    blockWorkersLatch.await(5, TimeUnit.SECONDS);
                    return null;
                });
                return null;
            });
        }

        startedLatch.await(2, TimeUnit.SECONDS);

        // 3rd task for "alice" must be rejected for per-user limit
        assertThatThrownBy(() -> queueService.execute("alice", () -> "third-task"))
                .isInstanceOf(ExecutionQueueFullException.class)
                .hasMessageContaining("User concurrency limit");

        blockWorkersLatch.countDown();
        clientThreads.shutdown();
    }

    @Test
    @DisplayName("Job that exceeds queue wait timeout is rejected with ExecutionQueueTimeoutException")
    void queueWaitTimeout_Rejection() throws Exception {
        // Set short wait timeout of 100ms
        properties.getQueue().setQueueWaitTimeoutMs(100L);

        CountDownLatch blockWorkersLatch = new CountDownLatch(1);
        CountDownLatch workersBusy = new CountDownLatch(2);

        ExecutorService clientThreads = Executors.newFixedThreadPool(3);

        // Block both worker threads for 400ms
        for (int i = 0; i < 2; i++) {
            clientThreads.submit(() -> {
                queueService.execute(null, () -> {
                    workersBusy.countDown();
                    Thread.sleep(400);
                    return null;
                });
                return null;
            });
        }

        workersBusy.await(2, TimeUnit.SECONDS);

        // Submit task that will wait >100ms in queue
        Future<?> waitingTask = clientThreads.submit(() -> {
            queueService.execute(null, () -> "should-timeout");
            return null;
        });

        assertThatThrownBy(waitingTask::get)
                .hasCauseInstanceOf(ExecutionQueueTimeoutException.class);

        blockWorkersLatch.countDown();
        clientThreads.shutdown();
    }

    @Test
    @DisplayName("Exceptions during execution are propagated and clean up active count")
    void taskException_PropagatedAndCleansUp() {
        assertThatThrownBy(() -> queueService.execute("bob", () -> {
            throw new IllegalArgumentException("Invalid execution code");
        })).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid execution code");

        // Subsequent task for "bob" succeeds (active count was decremented)
        String result = queueService.execute("bob", () -> "recovered");
        assertThat(result).isEqualTo("recovered");
    }

    @Test
    @DisplayName("Redis cluster limit check rejects when cluster lease cannot be acquired")
    void redisClusterLimit_Rejection() {
        properties.getQueue().setDistributedRedisCheck(true);
        ExecutionQueueServiceImpl redisQueueService = new ExecutionQueueServiceImpl(properties, stringRedisTemplate);

        when(stringRedisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                anyString(), anyString()
        )).thenReturn(0L); // 0 means cluster capacity full

        assertThatThrownBy(() -> redisQueueService.execute(null, () -> "task"))
                .isInstanceOf(ExecutionQueueFullException.class)
                .hasMessageContaining("Cluster execution capacity limit");

        redisQueueService.shutdown();
    }

    @Test
    @DisplayName("Redis unavailability falls back gracefully to local queue")
    void redisUnavailability_FallsBackGracefully() {
        properties.getQueue().setDistributedRedisCheck(true);
        ExecutionQueueServiceImpl redisQueueService = new ExecutionQueueServiceImpl(properties, stringRedisTemplate);

        when(stringRedisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                anyString(), anyString()
        )).thenThrow(new RuntimeException("Redis connection timeout"));

        String result = redisQueueService.execute(null, () -> "fallback-success");
        assertThat(result).isEqualTo("fallback-success");

        redisQueueService.shutdown();
    }

    @Test
    @DisplayName("Bursty concurrent submissions from single user respect maxConcurrentPerUser without race conditions")
    void burstConcurrentSubmissions_EnforcesUserLimit() throws Exception {
        int burstSize = 10;
        CountDownLatch blockWorkersLatch = new CountDownLatch(1);
        CountDownLatch submissionsCompleteLatch = new CountDownLatch(burstSize);
        AtomicInteger acceptedCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        ExecutorService clientThreads = Executors.newFixedThreadPool(burstSize);

        for (int i = 0; i < burstSize; i++) {
            clientThreads.submit(() -> {
                try {
                    queueService.execute("charlie", () -> {
                        acceptedCount.incrementAndGet();
                        blockWorkersLatch.await(5, TimeUnit.SECONDS);
                        return null;
                    });
                } catch (ExecutionQueueFullException e) {
                    rejectedCount.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    submissionsCompleteLatch.countDown();
                }
            });
        }

        submissionsCompleteLatch.await(3, TimeUnit.SECONDS);
        blockWorkersLatch.countDown();
        clientThreads.shutdown();

        assertThat(acceptedCount.get() + rejectedCount.get()).isEqualTo(burstSize);
        assertThat(acceptedCount.get()).isLessThanOrEqualTo(2);
        assertThat(rejectedCount.get()).isGreaterThanOrEqualTo(burstSize - 2);
    }
}

