package com.bharath.stacked.modules.execution.exception;

public class IsolateInitializationException extends IsolateException{
    public IsolateInitializationException(String message) {
        super(message);
    }

    public IsolateInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
