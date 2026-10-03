package com.bharath.stacked.modules.execution.exception;

public class DockerExecutionException extends DockerContainerException{
    public DockerExecutionException(String message) {
        super(message);
    }

    public DockerExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
