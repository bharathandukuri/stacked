package com.bharath.stacked.modules.execution.queue.impl;

import com.bharath.stacked.exception.AppException;
import com.bharath.stacked.modules.execution.config.ExecutionProperties;
import com.bharath.stacked.modules.execution.exception.ExecutionQueueFullException;
import com.bharath.stacked.modules.execution.exception.ExecutionQueueTimeoutException;
import com.bharath.stacked.modules.execution.queue.ExecutionQueueService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class ExecutionQueueServiceImpl implements ExecutionQueueService {

    private final ExecutionProperties executionProperties;
    private final StringRedisTemplate stringRedisTemplate;
    private final ThreadPoolExecutor executor;
    private final ConcurrentMap<String, AtomicInteger> userActiveCount = new ConcurrentHashMap<>();

    private static final String CLUSTER_ACTIVE_KEY = "stacked:execution:cluster:active_count";

    private static final String ACQUIRE_LEASE_LUA = """
            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local ttl = tonumber(ARGV[2])
            local current = tonumber(redis.call('get', key) or '0')
            if current < limit then
                redis.call('incr', key)
                redis.call('expire', key, ttl)
                return 1
            else
                return 0
            end
            """;

    private static final String RELEASE_LEASE_LUA = """
            local key = KEYS[1]
            local current = tonumber(redis.call('get', key) or '0')
            if current > 0 then
                redis.call('decr', key)
            end
            return 1
            """;

    private static final DefaultRedisScript<Long> ACQUIRE_SCRIPT;
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT;

    static {
        ACQUIRE_SCRIPT = new DefaultRedisScript<>(ACQUIRE_LEASE_LUA, Long.class);
        RELEASE_SCRIPT = new DefaultRedisScript<>(RELEASE_LEASE_LUA, Long.class);
    }

    @Autowired
    public ExecutionQueueServiceImpl(
            ExecutionProperties executionProperties,
            @Nullable StringRedisTemplate stringRedisTemplate) {
        this.executionProperties = executionProperties;
        this.stringRedisTemplate = stringRedisTemplate;

        ExecutionProperties.Queue queueConfig = executionProperties.getQueue();
        int workers = queueConfig.getMaxConcurrentWorkers();
        int capacity = queueConfig.getMaxQueueCapacity();

        this.executor = new ThreadPoolExecutor(
                workers,
                workers,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(capacity),
                new ThreadFactory() {
                    private final AtomicInteger count = new AtomicInteger(1);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "execution-worker-" + count.getAndIncrement());
                        t.setDaemon(true);
                        return t;
                    }
                },
                (r, exec) -> {
                    throw new ExecutionQueueFullException(
                            "Execution queue is at maximum capacity (" + capacity + " waiting). Please try again shortly.");
                }
        );

        log.info("Initialized ExecutionQueueService: maxWorkers={}, maxCapacity={}, waitTimeoutMs={}",
                workers, capacity, queueConfig.getQueueWaitTimeoutMs());
    }

    @Override
    public <T> T execute(@Nullable String userId, java.util.concurrent.Callable<T> task) {
        ExecutionProperties.Queue queueConfig = executionProperties.getQueue();
        if (!queueConfig.isEnabled()) {
            try {
                return task.call();
            } catch (AppException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        // 1. Per-User concurrency check
        boolean userSlotAcquired = false;
        if (userId != null && !userId.isBlank()) {
            AtomicInteger active = userActiveCount.computeIfAbsent(userId, k -> new AtomicInteger(0));
            if (active.incrementAndGet() > queueConfig.getMaxConcurrentPerUser()) {
                active.decrementAndGet();
                throw new ExecutionQueueFullException(
                        "User concurrency limit (" + queueConfig.getMaxConcurrentPerUser()
                                + ") reached. Please wait for previous jobs to complete.");
            }
            userSlotAcquired = true;
        }

        // 2. Cluster-wide Redis check
        boolean acquiredClusterLease = false;
        if (queueConfig.isDistributedRedisCheck() && stringRedisTemplate != null) {
            acquiredClusterLease = tryAcquireClusterLease(queueConfig.getClusterMaxConcurrent());
            if (!acquiredClusterLease) {
                if (userSlotAcquired) {
                    AtomicInteger active = userActiveCount.get(userId);
                    if (active != null && active.decrementAndGet() <= 0) {
                        userActiveCount.remove(userId);
                    }
                }
                throw new ExecutionQueueFullException(
                        "Cluster execution capacity limit (" + queueConfig.getClusterMaxConcurrent()
                                + ") reached. Please try again shortly.");
            }
        }

        long enqueueTime = System.currentTimeMillis();
        long queueWaitTimeoutMs = queueConfig.getQueueWaitTimeoutMs();
        long maxExecutionTimeMs = executionProperties.getHardLimits().getMaxTimeLimitMs();

        try {
            Future<T> future = executor.submit(() -> {
                long waitDuration = System.currentTimeMillis() - enqueueTime;
                if (waitDuration > queueWaitTimeoutMs) {
                    throw new ExecutionQueueTimeoutException(
                            "Execution request waited " + waitDuration + "ms in queue, exceeding limit of " + queueWaitTimeoutMs + "ms.");
                }

                return task.call();
            });

            try {
                return future.get(queueWaitTimeoutMs + maxExecutionTimeMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                throw new ExecutionQueueTimeoutException(
                        "Execution request timed out after waiting in queue or during execution.");
            } catch (InterruptedException e) {
                future.cancel(true);
                Thread.currentThread().interrupt();
                throw new RuntimeException("Execution task was interrupted.", e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof AppException appEx) {
                    throw appEx;
                } else if (cause instanceof RuntimeException runtimeEx) {
                    throw runtimeEx;
                } else {
                    throw new RuntimeException(cause != null ? cause.getMessage() : "Execution failed", cause);
                }
            }

        } finally {
            if (userSlotAcquired) {
                AtomicInteger active = userActiveCount.get(userId);
                if (active != null && active.decrementAndGet() <= 0) {
                    userActiveCount.remove(userId);
                }
            }
            if (acquiredClusterLease) {
                releaseClusterLease();
            }
        }
    }

    private boolean tryAcquireClusterLease(int clusterLimit) {
        try {
            Long result = stringRedisTemplate.execute(
                    ACQUIRE_SCRIPT,
                    List.of(CLUSTER_ACTIVE_KEY),
                    String.valueOf(clusterLimit),
                    "120"
            );
            return result != null && result == 1L;
        } catch (Exception e) {
            log.warn("Cluster execution queue check failed against Redis, falling back to local queue: {}", e.getMessage());
            return true;
        }
    }

    private void releaseClusterLease() {
        try {
            stringRedisTemplate.execute(
                    RELEASE_SCRIPT,
                    List.of(CLUSTER_ACTIVE_KEY)
            );
        } catch (Exception e) {
            log.warn("Failed to release cluster execution lease in Redis: {}", e.getMessage());
        }
    }

    @Override
    public int getActiveWorkerCount() {
        return executor.getActiveCount();
    }

    @Override
    public int getWaitingQueueSize() {
        return executor.getQueue().size();
    }

    @Override
    public int getRemainingQueueCapacity() {
        return executor.getQueue().remainingCapacity();
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down ExecutionQueueService...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

