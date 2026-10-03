package com.bharath.stacked.modules.execution.service;

import com.bharath.stacked.modules.execution.exception.IsolateCleanupException;
import com.bharath.stacked.modules.execution.exception.IsolateExecutionException;
import com.bharath.stacked.modules.execution.exception.IsolateInitializationException;
import com.bharath.stacked.modules.execution.dto.DockerContainerDetails;
import com.bharath.stacked.modules.execution.dto.IsolateExecutionConstraints;
import com.bharath.stacked.modules.execution.dto.response.IsolateExecutionResult;
import com.bharath.stacked.modules.execution.dto.IsolateSandBoxDetails;

import java.util.List;

public interface IsolateExecutionService {

    IsolateSandBoxDetails initialize(DockerContainerDetails dockerContainer) throws IsolateInitializationException;

    void cleanup(IsolateSandBoxDetails sandboxDetailsIsolate) throws IsolateCleanupException;

    IsolateExecutionResult executeWithConstraints(
            IsolateSandBoxDetails sandboxDetailsIsolate,
            List<String> command,
            String stdin,
            IsolateExecutionConstraints executionConstraints
    ) throws IsolateExecutionException;
}
