package com.bharath.stacked.common.api;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    BAD_REQUEST(HttpStatus.BAD_REQUEST, "The request could not be understood or is missing required parameters."),
    VALIDATION_FAILED(HttpStatus.UNPROCESSABLE_CONTENT, "Validation failed for one or more fields."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication credentials are missing, invalid, or expired."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "You do not have permission to access this resource."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "The requested resource was not found."),
    RESOURCE_ALREADY_EXISTS(HttpStatus.CONFLICT, "The resource already exists or violates uniqueness constraints."),
    CONFLICT(HttpStatus.CONFLICT, "A conflict occurred with the current state of the resource."),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded. Please slow down and try again later."),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "The requested service is temporarily unavailable."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal server error occurred.");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }
}
