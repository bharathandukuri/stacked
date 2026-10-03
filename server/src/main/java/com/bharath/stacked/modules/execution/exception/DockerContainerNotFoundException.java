package com.bharath.stacked.modules.execution.exception;

public class DockerContainerNotFoundException extends DockerContainerException {
    public DockerContainerNotFoundException(String message) {
        super(message);
    }

    public DockerContainerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
