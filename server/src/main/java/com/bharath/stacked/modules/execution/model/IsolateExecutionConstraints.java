package com.bharath.stacked.modules.execution.model;

public record IsolateExecutionConstraints(
        Double cpuTimeSeconds,
        Double wallTimeSeconds,
        Long memoryKb,
        Integer processLimit,
        Long fileSizeKb
) {

    public IsolateExecutionConstraints() {
        this(
                2.0,
                5.0,
                262144L,
                50,
                10240L
        );
    }
}