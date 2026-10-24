package com.bharath.stacked.modules.execution.queue;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.Callable;

public interface ExecutionQueueService {

    /**
     * Executes the given task under admission control, queue bounding, and concurrency limits.
     *
     * @param userId optional authenticated user ID for per-user concurrency limiting
     * @param task the execution task to run
     * @param <T> return type
     * @return the execution result
     */
    <T> T execute(@Nullable String userId, Callable<T> task);

    /**
     * Number of currently running worker threads executing jobs.
     */
    int getActiveWorkerCount();

    /**
     * Number of tasks waiting in the queue.
     */
    int getWaitingQueueSize();

    /**
     * Remaining capacity in the waiting queue before new tasks will be rejected.
     */
    int getRemainingQueueCapacity();
}

