package com.bharath.stacked.modules.execution.service;

import com.bharath.stacked.modules.execution.model.DockerContainerDetails;
import com.bharath.stacked.modules.execution.exception.DockerContainerCreationException;
import com.bharath.stacked.modules.execution.exception.DockerContainerDeletionException;
import com.bharath.stacked.modules.execution.exception.DockerImageCreationException;
import com.bharath.stacked.modules.execution.model.DockerImageDetails;
import org.springframework.stereotype.Service;

@Service
public interface DockerExecutionService {
    boolean checkImageExists(DockerImageDetails dockerImageDetails);
    void createImage(DockerImageDetails dockerImageDetails) throws DockerImageCreationException;
    DockerContainerDetails createContainer(DockerImageDetails dockerImageDetails) throws DockerContainerCreationException;
    void deleteContainer(String containerId) throws DockerContainerDeletionException;
}
