package com.bharath.stacked.exception;

import com.bharath.stacked.common.api.ErrorCode;

public class ConflictException extends AppException {

    public ConflictException(String message) {
        super(message, ErrorCode.CONFLICT);
    }

    public ConflictException(String resourceName, String fieldName, Object fieldValue) {
        super(
                String.format("%s already exists with %s: '%s'", resourceName, fieldName, fieldValue),
                ErrorCode.RESOURCE_ALREADY_EXISTS);
    }
}
