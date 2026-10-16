package com.bharath.stacked.modules.execution.dto;

import lombok.Builder;

@Builder
public record DockerContainerConstraints(
        long cpuLimit,
        long memoryLimitKb,
        long pidsLimit,
        boolean networkDisabled) {
    public static DockerContainerConstraints defaults() {
        return new DockerContainerConstraints(
                1L,
                262144L,
                100L,
                true);
    }
}
