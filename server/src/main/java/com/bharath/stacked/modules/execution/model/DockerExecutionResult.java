package com.bharath.stacked.modules.execution.model;

public record DockerExecutionResult (
        long exitCode,
        String stdout,
        String stderr
) {
}
