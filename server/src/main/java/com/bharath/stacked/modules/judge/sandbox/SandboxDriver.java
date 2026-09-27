package com.bharath.stacked.modules.judge.sandbox;

import com.bharath.stacked.modules.judge.enums.SandboxDriverType;
import com.bharath.stacked.modules.judge.sandbox.model.SandboxExecutionRequest;
import com.bharath.stacked.modules.judge.sandbox.model.SandboxExecutionResult;

public interface SandboxDriver {
    SandboxDriverType getType();

    boolean isAvailable();

    SandboxExecutionResult execute(SandboxExecutionRequest request);
}
