package com.bharath.stacked.modules.execution.exception;

public class DockerException extends RuntimeException{
    public DockerException(String message) {
        super(message);
    }

    public DockerException(String message, Throwable cause) {
        super(message, cause);
    }
}
