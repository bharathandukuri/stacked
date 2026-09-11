package com.bharath.stacked.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private ApiError error;
    @Builder.Default
    private Instant timestamp = Instant.now();

    // =========================================================================
    // Success Response Entity Factory Methods
    // =========================================================================

    public static <T> ResponseEntity<ApiResponse<T>> success(T data) {
        return success(data, "Success");
    }

    public static <T> ResponseEntity<ApiResponse<T>> success(T data, String message) {
        return ResponseEntity.ok(ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build());
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return created(data, "Resource created successfully");
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(T data, String message) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build());
    }

    public static ResponseEntity<ApiResponse<Void>> noContent() {
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // Compatibility aliases
    public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return success(data);
    }

    public static <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
        return success(data, message);
    }

    // =========================================================================
    // Error Response Entity Factory Methods (Used by Exception Handlers)
    // =========================================================================

    public static <T> ResponseEntity<ApiResponse<T>> error(ApiError apiError, HttpStatus status) {
        return ResponseEntity.status(status).body(ApiResponse.<T>builder()
                .success(false)
                .message(apiError.getMessage())
                .error(apiError)
                .timestamp(Instant.now())
                .build());
    }

    public static <T> ResponseEntity<ApiResponse<T>> error(ErrorCode errorCode, String message, String path) {
        ApiError apiError = ApiError.of(errorCode.name(), message, path);
        return error(apiError, errorCode.getHttpStatus());
    }
}
