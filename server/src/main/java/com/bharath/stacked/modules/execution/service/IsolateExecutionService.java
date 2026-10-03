package com.bharath.stacked.modules.execution.service;

import com.bharath.stacked.modules.execution.exception.IsolateCleanupException;
import com.bharath.stacked.modules.execution.exception.IsolateExecutionException;
import com.bharath.stacked.modules.execution.exception.IsolateInitializationException;
import com.bharath.stacked.modules.execution.model.DockerContainerDetails;
import com.bharath.stacked.modules.execution.model.IsolateExecutionConstraints;
import com.bharath.stacked.modules.execution.model.IsolateExecutionResult;
import com.bharath.stacked.modules.execution.model.SandBoxDetails;

import java.util.List;

public interface IsolateExecutionService {

    SandBoxDetails initialize(DockerContainerDetails dockerContainer) throws IsolateInitializationException;

    void cleanup(SandBoxDetails sandboxDetails) throws IsolateCleanupException;

    IsolateExecutionResult executeWithConstraints(
            SandBoxDetails sandboxDetails,
            List<String> command,
            String stdin,
            IsolateExecutionConstraints executionConstraints
    ) throws IsolateExecutionException;
}
