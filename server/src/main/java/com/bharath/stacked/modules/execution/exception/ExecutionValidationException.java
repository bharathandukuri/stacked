package com.bharath.stacked.modules.execution.exception;

public class ExecutionValidationException extends RuntimeException {

    public ExecutionValidationException(String message) {
        super(message);
    }

    public ExecutionValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

