package com.bharath.stacked.modules.execution.exception;

import com.bharath.stacked.common.api.ErrorCode;
import com.bharath.stacked.exception.AppException;

public class ExecutionQueueTimeoutException extends AppException {
    public ExecutionQueueTimeoutException(String message) {
        super(message, ErrorCode.SERVICE_UNAVAILABLE);
    }
}
