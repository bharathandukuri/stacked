package com.bharath.stacked.exception;

import com.bharath.stacked.common.api.ErrorCode;

public class InternalServerException extends AppException {

    public InternalServerException(String message) {
        super(message, ErrorCode.INTERNAL_SERVER_ERROR);
    }

    public InternalServerException(String message, Throwable cause) {
        super(message, cause, ErrorCode.INTERNAL_SERVER_ERROR);
    }

    public InternalServerException() {
        super(ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage(), ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
