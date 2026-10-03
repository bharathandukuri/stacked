package com.bharath.stacked.modules.execution.dto;

public record CodeExecutionConstraints(
        long timeLimitMs,
        long memoryLimitKb
) {
}