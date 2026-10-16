package com.bharath.stacked.modules.execution.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record BatchCodeExecutionResult(
        List<SimpleCodeExecutionResult> results,
        Long totalExecutionTimeMs,
        Integer totalExecutions,
        Integer successfulExecutions,
        Integer failedExecutions
) {
}

