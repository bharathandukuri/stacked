package com.bharath.stacked.exception;

import com.bharath.stacked.common.api.ApiError;
import com.bharath.stacked.common.api.ApiResponse;
import com.bharath.stacked.common.api.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles all custom application business exceptions.
     */
    @ExceptionHandler(AppException.class)
    @NonNull
    public ResponseEntity<ApiResponse<Void>> handleAppException(@NonNull AppException ex,
            @NonNull HttpServletRequest request) {
        log.warn("Application exception at [{} {}]: {} ({})",
                request.getMethod(), request.getRequestURI(), ex.getMessage(), ex.getErrorCode().name());

        ApiError apiError = ApiError.builder()
                .errorCode(ex.getErrorCode().name())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .details(ex.getDetails())
                .build();

        return ApiResponse.error(apiError, ex.getStatus());
    }

    /**
     * Handles Spring Boot @Valid / @Validated @RequestBody payload validation
     * failures.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @NonNull
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
            @NonNull MethodArgumentNotValidException ex, @NonNull HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        log.warn("Payload validation failed at [{} {}]: {}", request.getMethod(), request.getRequestURI(), fieldErrors);

        ApiError apiError = ApiError.validation(request.getRequestURI(), fieldErrors);
        return ApiResponse.error(apiError, HttpStatus.UNPROCESSABLE_CONTENT);
    }

    /**
     * Handles Spring 6 / Spring Boot 3+ method parameter validation failures
     * (@RequestParam, @PathVariable).
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    @NonNull
    public ResponseEntity<ApiResponse<Void>> handleHandlerMethodValidation(
            @NonNull HandlerMethodValidationException ex, @NonNull HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getAllErrors().forEach(error -> {
            String field = error instanceof FieldError fe ? fe.getField() : "parameter";
            fieldErrors.put(field, error.getDefaultMessage());
        });

        log.warn("Method parameter validation failed at [{} {}]: {}", request.getMethod(), request.getRequestURI(),
                fieldErrors);

        ApiError apiError = ApiError.validation(request.getRequestURI(), fieldErrors);
        return ApiResponse.error(apiError, HttpStatus.UNPROCESSABLE_CONTENT);
    }

    /**
     * Handles Jakarta Bean Validation ConstraintViolationException (e.g. from
     * service layer or @Validated controllers).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @NonNull
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            @NonNull ConstraintViolationException ex, @NonNull HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String propertyPath = violation.getPropertyPath().toString();
            fieldErrors.put(propertyPath, violation.getMessage());
        });

        log.warn("Constraint violation at [{} {}]: {}", request.getMethod(), request.getRequestURI(), fieldErrors);

        ApiError apiError = ApiError.validation(request.getRequestURI(), fieldErrors);
        return ApiResponse.error(apiError, HttpStatus.UNPROCESSABLE_CONTENT);
    }

    /**
     * Handles malformed or unparseable JSON payloads.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @NonNull
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(
            @NonNull HttpMessageNotReadableException ex, @NonNull HttpServletRequest request) {
        log.warn("Malformed HTTP message at [{} {}]: {}", request.getMethod(), request.getRequestURI(),
                ex.getMessage());

        ApiError apiError = ApiError.of(
                ErrorCode.BAD_REQUEST.name(),
                "Malformed JSON request body or unparseable payload.",
                request.getRequestURI());

        return ApiResponse.error(apiError, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles URL parameter type mismatch (e.g. passing 'abc' when integer/UUID
     * expected).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @NonNull
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(
            @NonNull MethodArgumentTypeMismatchException ex, @NonNull HttpServletRequest request) {
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String message = String.format("Parameter '%s' should be of type '%s'", ex.getName(), requiredType);
        log.warn("Parameter type mismatch at [{} {}]: {}", request.getMethod(), request.getRequestURI(), message);

        ApiError apiError = ApiError.of(ErrorCode.BAD_REQUEST.name(), message, request.getRequestURI());
        return ApiResponse.error(apiError, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles HTTP method not supported (405).
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @NonNull
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            @NonNull HttpRequestMethodNotSupportedException ex, @NonNull HttpServletRequest request) {
        String message = String.format("HTTP method '%s' is not supported for this endpoint.", ex.getMethod());
        log.warn("Method not supported at [{} {}]: {}", request.getMethod(), request.getRequestURI(), message);

        ApiError apiError = ApiError.of(ErrorCode.BAD_REQUEST.name(), message, request.getRequestURI());
        return ApiResponse.error(apiError, HttpStatus.METHOD_NOT_ALLOWED);
    }

    /**
     * Handles unsupported media types (415).
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @NonNull
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(
            @NonNull HttpMediaTypeNotSupportedException ex, @NonNull HttpServletRequest request) {
        String message = String.format("Content-Type '%s' is not supported.", ex.getContentType());
        log.warn("Media type not supported at [{} {}]: {}", request.getMethod(), request.getRequestURI(), message);

        ApiError apiError = ApiError.of(ErrorCode.BAD_REQUEST.name(), message, request.getRequestURI());
        return ApiResponse.error(apiError, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    /**
     * Handles Spring 404 NoResourceFoundException when a path is not matched.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @NonNull
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(
            @NonNull NoResourceFoundException ex, @NonNull HttpServletRequest request) {
        String message = String.format("The requested path '%s' was not found on this server.",
                request.getRequestURI());
        log.warn("No resource found for [{} {}]", request.getMethod(), request.getRequestURI());

        ApiError apiError = ApiError.of(ErrorCode.RESOURCE_NOT_FOUND.name(), message, request.getRequestURI());
        return ApiResponse.error(apiError, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles multipart file uploads exceeding the configured maximum size limit.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @NonNull
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(
            @NonNull MaxUploadSizeExceededException ex, @NonNull HttpServletRequest request) {
        String message = "Uploaded file exceeds the maximum allowed size limit.";
        log.warn("Max upload size exceeded at [{} {}]: {}", request.getMethod(), request.getRequestURI(),
                ex.getMessage());

        ApiError apiError = ApiError.of(ErrorCode.BAD_REQUEST.name(), message, request.getRequestURI());
        return ApiResponse.error(apiError, HttpStatus.CONTENT_TOO_LARGE);
    }

    /**
     * Catch-all handler for unexpected internal server errors.
     */
    @ExceptionHandler(Exception.class)
    @NonNull
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            @NonNull Exception ex, @NonNull HttpServletRequest request) {
        log.error("Unhandled internal server error at [{} {}]: ", request.getMethod(), request.getRequestURI(), ex);

        ApiError apiError = ApiError.of(
                ErrorCode.INTERNAL_SERVER_ERROR.name(),
                "An unexpected internal error occurred. Please contact support if the issue persists.",
                request.getRequestURI());

        return ApiResponse.error(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}