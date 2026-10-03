package com.bharath.stacked.modules.execution.exception;

public class IsolateExecutionException extends IsolateException{
    public IsolateExecutionException(String message) {
        super(message);
    }

    public IsolateExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
