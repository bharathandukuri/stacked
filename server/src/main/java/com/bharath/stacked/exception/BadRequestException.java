package com.bharath.stacked.exception;

import com.bharath.stacked.common.api.ErrorCode;

public class BadRequestException extends AppException {

    public BadRequestException(String message) {
        super(message, ErrorCode.BAD_REQUEST);
    }

    public BadRequestException(String message, Object details) {
        super(message, ErrorCode.BAD_REQUEST, details);
    }
}
