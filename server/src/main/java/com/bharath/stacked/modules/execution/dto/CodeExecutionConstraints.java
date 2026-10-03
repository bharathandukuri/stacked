package com.bharath.stacked.modules.execution.dto;

public record CodeExecutionConstraints(
        long timeLimitMs,
        Long memoryLimitKb
) {
    public CodeExecutionConstraints(long timeLimitMs) {
        this(timeLimitMs, null);
    }
}