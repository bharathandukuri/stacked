package com.bharath.stacked.modules.execution.exception;

public class IsolateCleanupException extends IsolateException{
    public IsolateCleanupException(String message) {
        super(message);
    }

    public IsolateCleanupException(String message, Throwable cause) {
        super(message, cause);
    }
}
