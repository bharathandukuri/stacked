package com.bharath.stacked.modules.execution.dto.response;

import com.bharath.stacked.modules.execution.enums.CodeExecutionStatus;
import lombok.Builder;

import java.util.List;

@Builder
public record SimpleCodeExecutionResult(
        String stdout,
        String stderr,
        Long exitCode,
        Long exitSignal,
        CodeExecutionStatus executionStatus,
        List<String> logs
) {
}