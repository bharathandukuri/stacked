package com.bharath.stacked.modules.judge.sandbox.model;

import com.bharath.stacked.modules.judge.enums.ExecutionStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record SandboxExecutionResult(
        int exitCode,
        @NonNull String stdout,
        @NonNull String stderr,
        long executionTimeMs,
        long memoryUsedKb,
        @NonNull ExecutionStatus status,
        @Nullable String errorMessage) {
    public SandboxExecutionResult {
        if (stdout == null)
            stdout = "";
        if (stderr == null)
            stderr = "";
        if (status == null)
            status = ExecutionStatus.SUCCESS;
    }

    public static SandboxExecutionResult success(int exitCode, String stdout, String stderr, long timeMs,
            long memoryKb) {
        return new SandboxExecutionResult(exitCode, stdout != null ? stdout : "", stderr != null ? stderr : "", timeMs,
                memoryKb, ExecutionStatus.SUCCESS, null);
    }

    public static SandboxExecutionResult compileError(int exitCode, String stdout, String stderr, long timeMs) {
        return new SandboxExecutionResult(exitCode, stdout != null ? stdout : "", stderr != null ? stderr : "", timeMs,
                0, ExecutionStatus.COMPILATION_ERROR, "Compilation failed with exit code " + exitCode);
    }

    public static SandboxExecutionResult timeout(long timeoutMs) {
        return new SandboxExecutionResult(-1, "", "Execution timed out after " + timeoutMs + "ms", timeoutMs, 0,
                ExecutionStatus.TIME_LIMIT_EXCEEDED, "Time limit exceeded");
    }

    public static SandboxExecutionResult memoryLimitExceeded(long memoryKb) {
        return new SandboxExecutionResult(-1, "", "Memory limit exceeded", 0, memoryKb,
                ExecutionStatus.MEMORY_LIMIT_EXCEEDED, "Memory limit exceeded");
    }

    public static SandboxExecutionResult runtimeError(int exitCode, String stdout, String stderr, long timeMs) {
        return new SandboxExecutionResult(exitCode, stdout != null ? stdout : "", stderr != null ? stderr : "", timeMs,
                0, ExecutionStatus.RUNTIME_ERROR, "Runtime error with exit code " + exitCode);
    }

    public static SandboxExecutionResult failure(String errorMessage) {
        return new SandboxExecutionResult(-1, "", errorMessage != null ? errorMessage : "Sandbox execution error", 0, 0,
                ExecutionStatus.SANDBOX_FAILURE, errorMessage);
    }
}
