package com.bharath.stacked.modules.execution.dto.response;

public record DockerExecutionResult (
        long exitCode,
        String stdout,
        String stderr
) {
}
