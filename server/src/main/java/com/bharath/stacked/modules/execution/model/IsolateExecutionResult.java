package com.bharath.stacked.modules.execution.model;

import com.bharath.stacked.modules.execution.enums.IsolateExecutionStatus;

public record IsolateExecutionResult(
        IsolateExecutionStatus status,

        String stdout,
        String stderr,

        Double cpuTimeSeconds,
        Double wallTimeSeconds,

        Long memoryKb,

        Long exitCode,
        Long exitSignal,

        Boolean killed,

        Long contextSwitchesVoluntary,
        Long contextSwitchesForced
) {
}