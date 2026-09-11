package com.bharath.stacked.exception;

import com.bharath.stacked.common.api.ErrorCode;

public class ForbiddenException extends AppException {

    public ForbiddenException(String message) {
        super(message, ErrorCode.FORBIDDEN);
    }

    public ForbiddenException() {
        super(ErrorCode.FORBIDDEN.getDefaultMessage(), ErrorCode.FORBIDDEN);
    }
}
