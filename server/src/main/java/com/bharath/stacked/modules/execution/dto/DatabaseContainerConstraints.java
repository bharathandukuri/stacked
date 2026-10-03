package com.bharath.stacked.modules.execution.dto;

import lombok.Builder;

@Builder
public record DatabaseContainerConstraints(
        long cpuLimit,
        long memoryLimitKb,
        long pidsLimit,
        boolean networkDisabled) {
    public static DatabaseContainerConstraints defaults() {
        return new DatabaseContainerConstraints(
                1L,
                262144L,
                100L,
                true);
    }
}
