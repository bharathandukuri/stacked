package com.bharath.stacked.modules.judge.dto;

import com.bharath.stacked.modules.judge.enums.ExecutionStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record SampleRunResponse(
        @NonNull String languageId,
        @NonNull ExecutionStatus status,
        int totalTestCases,
        int passedTestCases,
        long totalExecutionTimeMs,
        @Nullable String compilationError,
        @NonNull List<TestcaseExecutionResult> results) {
}
