package com.bharath.stacked.exception;

import com.bharath.stacked.common.api.ErrorCode;
import lombok.Getter;

import java.util.Map;

@Getter
public class ValidationException extends AppException {

    private final Map<String, String> fieldErrors;

    public ValidationException(Map<String, String> fieldErrors) {
        super("Validation failed for request parameters", ErrorCode.VALIDATION_FAILED, fieldErrors);
        this.fieldErrors = fieldErrors;
    }

    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(message, ErrorCode.VALIDATION_FAILED, fieldErrors);
        this.fieldErrors = fieldErrors;
    }
}
