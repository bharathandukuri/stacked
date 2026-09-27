package com.bharath.stacked.modules.judge.dto;

import com.bharath.stacked.modules.judge.enums.ExecutionStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record TestcaseExecutionResult(
        @NonNull String testCaseId,
        @NonNull ExecutionStatus status,
        @NonNull String input,
        @Nullable String expectedOutput,
        @NonNull String actualOutput,
        long executionTimeMs,
        long memoryUsedKb,
        @Nullable String errorMessage) {
}
