package com.bharath.stacked.exception;

import com.bharath.stacked.common.api.ErrorCode;

public class UnauthorizedException extends AppException {

    public UnauthorizedException(String message) {
        super(message, ErrorCode.UNAUTHORIZED);
    }

    public UnauthorizedException() {
        super(ErrorCode.UNAUTHORIZED.getDefaultMessage(), ErrorCode.UNAUTHORIZED);
    }
}
