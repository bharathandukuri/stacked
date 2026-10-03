package com.bharath.stacked.modules.execution.service;

import com.bharath.stacked.modules.execution.exception.*;
import com.bharath.stacked.modules.execution.model.DockerContainerDetails;
import com.bharath.stacked.modules.execution.model.DockerExecutionResult;
import com.bharath.stacked.modules.execution.model.DockerImageDetails;

import java.util.List;

public interface DockerExecutionService {
    boolean isImageExists(DockerImageDetails dockerImageDetails);

    void createImage(DockerImageDetails dockerImageDetails) throws DockerImageCreationException;

    DockerContainerDetails createContainer(DockerImageDetails dockerImageDetails)
            throws DockerContainerCreationException;

    void deleteContainer(String containerId) throws DockerContainerDeletionException;

    boolean isContainerExists(String containerId);

    void startContainer(String containerId) throws DockerContainerNotFoundException;

    void stopContainer(String containerId)
            throws DockerContainerStopException;

    DockerExecutionResult execContainer(String containerId, List<String> command) throws DockerExecutionException;

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
