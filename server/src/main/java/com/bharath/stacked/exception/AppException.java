package com.bharath.stacked.exception;

import com.bharath.stacked.common.api.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AppException extends RuntimeException {

    private final HttpStatus status;
    private final ErrorCode errorCode;
    private final Object details;

    public AppException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
        this.errorCode = ErrorCode.BAD_REQUEST;
        this.details = null;
    }

    public AppException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.errorCode = ErrorCode.BAD_REQUEST;
        this.details = null;
    }

    public AppException(String message, ErrorCode errorCode) {
        super(message);
        this.status = errorCode.getHttpStatus();
        this.errorCode = errorCode;
        this.details = null;
    }

    public AppException(String message, ErrorCode errorCode, Object details) {
        super(message);
        this.status = errorCode.getHttpStatus();
        this.errorCode = errorCode;
        this.details = details;
    }

    public AppException(String message, Throwable cause, ErrorCode errorCode) {
        super(message, cause);
        this.status = errorCode.getHttpStatus();
        this.errorCode = errorCode;
        this.details = null;
    }
}
