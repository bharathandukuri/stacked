package com.bharath.stacked.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private String errorCode;
    private String message;
    private String path;
    @Builder.Default
    private Instant timestamp = Instant.now();
    private Map<String, String> validationErrors;
    private Object details;

    public static ApiError of(String errorCode, String message, String path) {
        return ApiError.builder()
                .errorCode(errorCode)
                .message(message)
                .path(path)
                .timestamp(Instant.now())
                .build();
    }

    public static ApiError of(String errorCode, String message, String path, Object details) {
        return ApiError.builder()
                .errorCode(errorCode)
                .message(message)
                .path(path)
                .details(details)
                .timestamp(Instant.now())
                .build();
    }

    public static ApiError validation(String path, Map<String, String> validationErrors) {
        return ApiError.builder()
                .errorCode(ErrorCode.VALIDATION_FAILED.name())
                .message("Input validation failed for one or more fields.")
                .path(path)
                .validationErrors(validationErrors)
                .timestamp(Instant.now())
                .build();
    }
}

