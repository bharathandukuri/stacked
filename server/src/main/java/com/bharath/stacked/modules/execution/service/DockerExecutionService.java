package com.bharath.stacked.modules.execution.service;

import com.bharath.stacked.modules.execution.exception.DockerImageCreationFailedException;
import com.bharath.stacked.modules.execution.registry.DockerImageDetails;
import org.springframework.stereotype.Service;

@Service
public interface DockerExecutionService {
    boolean checkImageExists(DockerImageDetails dockerImageDetails);
    void createImage(DockerImageDetails dockerImageDetails) throws DockerImageCreationFailedException;
}
