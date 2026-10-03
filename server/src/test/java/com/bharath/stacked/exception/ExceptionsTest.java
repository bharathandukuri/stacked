package com.bharath.stacked.exception;

import com.bharath.stacked.common.api.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Custom Exceptions Unit Tests")
class ExceptionsTest {

    @Test
    @DisplayName("AppException constructors and properties")
    void appExceptionConstructors() {
        AppException e1 = new AppException("simple error");
        assertThat(e1.getMessage()).isEqualTo("simple error");
        assertThat(e1.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(e1.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(e1.getDetails()).isNull();

        AppException e2 = new AppException("status error", HttpStatus.I_AM_A_TEAPOT);
        assertThat(e2.getMessage()).isEqualTo("status error");
        assertThat(e2.getStatus()).isEqualTo(HttpStatus.I_AM_A_TEAPOT);
        assertThat(e2.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);

        AppException e3 = new AppException("not found error", ErrorCode.RESOURCE_NOT_FOUND);
        assertThat(e3.getMessage()).isEqualTo("not found error");
        assertThat(e3.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(e3.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);

        Object details = Map.of("key", "val");
        AppException e4 = new AppException("with details", ErrorCode.CONFLICT, details);
        assertThat(e4.getMessage()).isEqualTo("with details");
        assertThat(e4.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(e4.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
        assertThat(e4.getDetails()).isEqualTo(details);

        Throwable cause = new RuntimeException("root cause");
        AppException e5 = new AppException("with cause", cause, ErrorCode.SERVICE_UNAVAILABLE);
        assertThat(e5.getMessage()).isEqualTo("with cause");
        assertThat(e5.getCause()).isEqualTo(cause);
        assertThat(e5.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(e5.getErrorCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("BadRequestException constructors")
    void badRequestException() {
        BadRequestException e1 = new BadRequestException("bad input");
        assertThat(e1.getMessage()).isEqualTo("bad input");
        assertThat(e1.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(e1.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(e1.getDetails()).isNull();

        Map<String, String> details = Map.of("field", "bad");
        BadRequestException e2 = new BadRequestException("bad input details", details);
        assertThat(e2.getMessage()).isEqualTo("bad input details");
        assertThat(e2.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(e2.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(e2.getDetails()).isEqualTo(details);
    }

    @Test
    @DisplayName("ConflictException constructors")
    void conflictException() {
        ConflictException e1 = new ConflictException("conflict occurred");
        assertThat(e1.getMessage()).isEqualTo("conflict occurred");
        assertThat(e1.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(e1.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);

        ConflictException e2 = new ConflictException("User", "email", "test@test.com");
        assertThat(e2.getMessage()).isEqualTo("User already exists with email: 'test@test.com'");
        assertThat(e2.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(e2.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("ForbiddenException constructors")
    void forbiddenException() {
        ForbiddenException e1 = new ForbiddenException();
        assertThat(e1.getMessage()).isEqualTo(ErrorCode.FORBIDDEN.getDefaultMessage());
        assertThat(e1.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(e1.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);

        ForbiddenException e2 = new ForbiddenException("No permission");
        assertThat(e2.getMessage()).isEqualTo("No permission");
        assertThat(e2.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(e2.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("InternalServerException constructors")
    void internalServerException() {
        InternalServerException e1 = new InternalServerException();
        assertThat(e1.getMessage()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage());
        assertThat(e1.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(e1.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);

        InternalServerException e2 = new InternalServerException("Crash");
        assertThat(e2.getMessage()).isEqualTo("Crash");
        assertThat(e2.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        Throwable cause = new IllegalStateException("db down");
        InternalServerException e3 = new InternalServerException("Failed", cause);
        assertThat(e3.getMessage()).isEqualTo("Failed");
        assertThat(e3.getCause()).isEqualTo(cause);
        assertThat(e3.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("ResourceNotFoundException constructors")
    void resourceNotFoundException() {
        ResourceNotFoundException e1 = new ResourceNotFoundException("File", "id", "file-123");
        assertThat(e1.getMessage()).isEqualTo("File not found with id: 'file-123'");
        assertThat(e1.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(e1.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);

        ResourceNotFoundException e2 = new ResourceNotFoundException("Direct message not found");
        assertThat(e2.getMessage()).isEqualTo("Direct message not found");
        assertThat(e2.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(e2.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("UnauthorizedException constructors")
    void unauthorizedException() {
        UnauthorizedException e1 = new UnauthorizedException();
        assertThat(e1.getMessage()).isEqualTo(ErrorCode.UNAUTHORIZED.getDefaultMessage());
        assertThat(e1.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(e1.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);

        UnauthorizedException e2 = new UnauthorizedException("Bad token");
        assertThat(e2.getMessage()).isEqualTo("Bad token");
        assertThat(e2.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("ValidationException constructors and getters")
    void validationException() {
        Map<String, String> errors = Map.of("username", "cannot be null");
        ValidationException e1 = new ValidationException(errors);
        assertThat(e1.getMessage()).isEqualTo("Validation failed for request parameters");
        assertThat(e1.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(e1.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
        assertThat(e1.getFieldErrors()).isEqualTo(errors);
        assertThat(e1.getDetails()).isEqualTo(errors);

        ValidationException e2 = new ValidationException("Custom validation error", errors);
        assertThat(e2.getMessage()).isEqualTo("Custom validation error");
        assertThat(e2.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(e2.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
        assertThat(e2.getFieldErrors()).isEqualTo(errors);
    }
}
