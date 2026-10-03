package com.bharath.stacked.modules.execution.service;

import com.bharath.stacked.modules.execution.dto.request.SimpleCodeExecutionRequest;
import com.bharath.stacked.modules.execution.dto.response.SimpleCodeExecutionResult;

public interface CodeExecutionService {
    SimpleCodeExecutionResult run(SimpleCodeExecutionRequest request);
}
