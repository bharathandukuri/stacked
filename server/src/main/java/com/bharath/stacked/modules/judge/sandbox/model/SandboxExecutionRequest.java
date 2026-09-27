package com.bharath.stacked.modules.judge.sandbox.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public record SandboxExecutionRequest(
        @NonNull String executionId,
        @NonNull String languageId,
        @Nullable String dockerImage,
        @Nullable String firecrackerRootfs,
        @NonNull Map<String, String> files,
        @Nullable String compileCommand,
        @NonNull String runCommand,
        @Nullable String stdin,
        long timeoutMs,
        int memoryLimitMb,
        boolean readOnly) {
    public SandboxExecutionRequest {
        if (files == null)
            files = Map.of();
        if (stdin == null)
            stdin = "";
        if (timeoutMs <= 0)
            timeoutMs = 5000;
        if (memoryLimitMb <= 0)
            memoryLimitMb = 256;
    }
}
