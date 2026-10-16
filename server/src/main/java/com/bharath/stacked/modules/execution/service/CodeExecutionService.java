package com.bharath.stacked.modules.execution.service;

import com.bharath.stacked.modules.execution.dto.request.BatchCodeExecutionRequest;
import com.bharath.stacked.modules.execution.dto.request.SimpleCodeExecutionRequest;
import com.bharath.stacked.modules.execution.dto.response.BatchCodeExecutionResult;
import com.bharath.stacked.modules.execution.dto.response.SimpleCodeExecutionResult;

public interface CodeExecutionService {
    SimpleCodeExecutionResult run(SimpleCodeExecutionRequest request);
    BatchCodeExecutionResult run(BatchCodeExecutionRequest request);
}
