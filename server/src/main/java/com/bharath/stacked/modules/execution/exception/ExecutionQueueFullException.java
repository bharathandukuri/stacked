package com.bharath.stacked.modules.execution.exception;

import com.bharath.stacked.common.api.ErrorCode;
import com.bharath.stacked.exception.AppException;

public class ExecutionQueueFullException extends AppException {
    public ExecutionQueueFullException(String message) {
        super(message, ErrorCode.SERVICE_UNAVAILABLE);
    }
}
