package com.bharath.stacked.modules.execution.exception;

public class DockerImageCreationException extends DockerException{
    public DockerImageCreationException(String message) {
        super(message);
    }
    public DockerImageCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
