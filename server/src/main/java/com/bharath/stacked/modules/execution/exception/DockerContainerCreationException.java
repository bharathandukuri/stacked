package com.bharath.stacked.modules.execution.exception;

public class DockerContainerCreationException extends DockerContainerException {

    public DockerContainerCreationException(String message) {
        super(message);
    }

    public DockerContainerCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
