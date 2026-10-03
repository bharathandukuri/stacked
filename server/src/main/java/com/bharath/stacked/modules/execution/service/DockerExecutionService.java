package com.bharath.stacked.modules.execution.service;

import com.bharath.stacked.modules.execution.dto.DatabaseContainerConstraints;
import com.bharath.stacked.modules.execution.dto.DockerContainerDetails;
import com.bharath.stacked.modules.execution.dto.DockerImageDetails;
import com.bharath.stacked.modules.execution.dto.response.DockerExecutionResult;
import com.bharath.stacked.modules.execution.exception.*;

import java.util.List;

public interface DockerExecutionService {
    boolean isImageExists(DockerImageDetails dockerImageDetails);

    default boolean isImageExists(com.bharath.stacked.modules.execution.registry.DockerImageRegistry registry) {
        return registry != null && isImageExists(registry.dockerImage());
    }

    void createImage(DockerImageDetails dockerImageDetails) throws DockerImageCreationException;

    DockerContainerDetails createContainer(DockerImageDetails dockerImageDetails)
            throws DockerContainerCreationException;

    DockerContainerDetails createContainer(
            DockerImageDetails dockerImageDetails,
            DatabaseContainerConstraints constraints
    ) throws DockerContainerCreationException;

    default DockerContainerDetails createContainer(com.bharath.stacked.modules.execution.registry.DockerImageRegistry registry)
            throws DockerContainerCreationException {
        if (registry == null) {
            throw new DockerContainerCreationException("DockerImageRegistry must not be null.");
        }
        return createContainer(registry.dockerImage());
    }

    default DockerContainerDetails createContainer(
            com.bharath.stacked.modules.execution.registry.DockerImageRegistry registry,
            DatabaseContainerConstraints constraints
    ) throws DockerContainerCreationException {
        if (registry == null) {
            throw new DockerContainerCreationException("DockerImageRegistry must not be null.");
        }
        return createContainer(registry.dockerImage(), constraints);
    }

    void deleteContainer(String containerId) throws DockerContainerDeletionException;

    boolean isContainerExists(String containerId);

    void startContainer(String containerId) throws DockerContainerNotFoundException;

    void stopContainer(String containerId)
            throws DockerContainerStopException;

    DockerExecutionResult execContainer(String containerId, List<String> command) throws DockerExecutionException;

    DockerExecutionResult execContainer(
            String containerId,
            List<String> command,
            Long timeLimitMs
    ) throws DockerExecutionException;

    String readFile(
            String containerId,
            String path
    ) throws DockerExecutionException;

    void writeFile(
            String containerId,
            String path,
            String content
    ) throws DockerExecutionException;
}
