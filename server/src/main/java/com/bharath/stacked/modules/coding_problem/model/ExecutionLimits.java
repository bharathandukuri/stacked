package com.bharath.stacked.modules.coding_problem.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ExecutionLimits {

    // Maximum execution time per test case in milliseconds
    private long timeLimitMs;

    // Maximum memory usage in kilobytes (KB)
    private long memoryLimitKb;

    // Maximum output size in bytes
    private long outputLimitBytes;

    // Maximum wait time for the database query result in milliseconds
    private long queryTimeoutMs;

    public static ExecutionLimits defaults() {
        return ExecutionLimits.builder()
                .timeLimitMs(2000L)
                .memoryLimitKb(262144L) // 256 MB in KB
                .outputLimitBytes(10485760L) // 10 MB in bytes
                .queryTimeoutMs(10000L)
                .build();
    }
}