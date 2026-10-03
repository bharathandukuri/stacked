package com.bharath.stacked.modules.execution.exception;

public class IsolateException extends RuntimeException {
    public IsolateException(String message) {
        super(message);
    }
    public IsolateException(String message, Throwable cause) {
        super(message, cause);
    }
}
