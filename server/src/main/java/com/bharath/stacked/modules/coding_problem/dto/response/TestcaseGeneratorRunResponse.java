package com.bharath.stacked.modules.coding_problem.dto.response;

import com.bharath.stacked.modules.execution.enums.CodeExecutionStatus;
import lombok.Builder;

@Builder
public record TestcaseGeneratorRunResponse(
        CodeExecutionStatus status,
        String stdout,
        String stderr,
        Long exitCode,
        Long exitSignal,
        Long executionTimeMs,
        Long memoryUsageKb,
        String error) {
}
