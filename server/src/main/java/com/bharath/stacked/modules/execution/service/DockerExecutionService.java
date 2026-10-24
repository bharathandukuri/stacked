package com.bharath.stacked.modules.execution.service;

import com.bharath.stacked.modules.execution.dto.DockerContainerConstraints;
import com.bharath.stacked.modules.execution.dto.DockerContainerDetails;
import com.bharath.stacked.modules.execution.dto.DockerImageDetails;
import com.bharath.stacked.modules.execution.dto.response.DockerExecutionResult;
import com.bharath.stacked.modules.execution.exception.*;
import com.bharath.stacked.modules.execution.registry.DockerImageRegistry;

import java.util.List;

public interface DockerExecutionService {

    boolean isImageExists(DockerImageDetails dockerImageDetails);


    void createImage(DockerImageDetails dockerImageDetails) throws DockerImageCreationException;

    DockerContainerDetails createContainer(DockerImageDetails dockerImageDetails) throws DockerContainerCreationException;

    DockerContainerDetails createContainer(DockerImageDetails dockerImageDetails, DockerContainerConstraints constraints) throws DockerContainerCreationException;

    default DockerContainerDetails createContainer(DockerImageRegistry registry, DockerContainerConstraints constraints) throws DockerContainerCreationException {
        if (registry == null) {
            throw new DockerContainerCreationException("DockerImageRegistry must not be null.");
        }
        return createContainer(registry.dockerImage(), constraints);
    }

    void deleteContainer(String containerId) throws DockerContainerDeletionException;

    boolean isContainerExists(String containerId);

    boolean isContainerRunning(String containerId);

    String getContainerLogs(String containerId, int tailLines);

    void startContainer(String containerId) throws DockerContainerNotFoundException;

    void stopContainer(String containerId) throws DockerContainerStopException;

    DockerExecutionResult execContainer(String containerId, List<String> command) throws DockerExecutionException;

    DockerExecutionResult execContainer(String containerId, List<String> command, Long timeLimitMs) throws DockerExecutionException;

    default DockerExecutionResult execContainer(String containerId, List<String> command, Long timeLimitMs, Long maxOutputBytes) throws DockerExecutionException {
        return execContainer(containerId, command, timeLimitMs);
    }

    String readFile(String containerId, String path) throws DockerExecutionException;

    void writeFile(String containerId, String path, String content) throws DockerExecutionException;
}
