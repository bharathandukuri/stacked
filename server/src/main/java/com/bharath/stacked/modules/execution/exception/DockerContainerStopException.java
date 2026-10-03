package com.bharath.stacked.modules.execution.exception;

public class DockerContainerStopException extends DockerContainerException{
    public DockerContainerStopException(String message) {
        super(message);
    }

    public DockerContainerStopException(String message, Throwable cause) {
        super(message, cause);
    }
}
