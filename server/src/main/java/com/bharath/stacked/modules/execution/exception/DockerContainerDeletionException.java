package com.bharath.stacked.modules.execution.exception;

public class DockerContainerDeletionException extends DockerContainerException {
    public DockerContainerDeletionException(String message) {
        super(message);
    }

    public DockerContainerDeletionException(String message, Throwable cause) {
        super(message, cause);
    }
}
