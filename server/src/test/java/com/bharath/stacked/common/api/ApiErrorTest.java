package com.bharath.stacked.common.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiError Unit Tests")
class ApiErrorTest {

    @Test
    @DisplayName("of(errorCode, message, path) creates basic ApiError")
    void ofBasic() {
        ApiError error = ApiError.of("TEST_ERR", "Something failed", "/api/items");

        assertThat(error.getErrorCode()).isEqualTo("TEST_ERR");
        assertThat(error.getMessage()).isEqualTo("Something failed");
        assertThat(error.getPath()).isEqualTo("/api/items");
        assertThat(error.getTimestamp()).isNotNull();
        assertThat(error.getValidationErrors()).isNull();
        assertThat(error.getDetails()).isNull();
    }

    @Test
    @DisplayName("of(errorCode, message, path, details) creates ApiError with details")
    void ofWithDetails() {
        Map<String, Object> details = Map.of("reason", "timeout", "retryAfter", 5);
        ApiError error = ApiError.of("TIMEOUT", "Request timed out", "/api/execute", details);

        assertThat(error.getErrorCode()).isEqualTo("TIMEOUT");
        assertThat(error.getMessage()).isEqualTo("Request timed out");
        assertThat(error.getPath()).isEqualTo("/api/execute");
        assertThat(error.getDetails()).isEqualTo(details);
    }

    @Test
    @DisplayName("validation(path, validationErrors) creates validation error with default message and code")
    void validationError() {
        Map<String, String> fieldErrors = Map.of(
                "email", "must be a valid email",
                "password", "must not be blank"
        );

        ApiError error = ApiError.validation("/api/register", fieldErrors);

        assertThat(error.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(error.getMessage()).isEqualTo("Input validation failed for one or more fields.");
        assertThat(error.getPath()).isEqualTo("/api/register");
        assertThat(error.getValidationErrors()).containsEntry("email", "must be a valid email");
        assertThat(error.getValidationErrors()).containsEntry("password", "must not be blank");
    }

    @Test
    @DisplayName("Builder, setters, equals, hashCode, and toString")
    void builderAndGettersSetters() {
        Instant now = Instant.now();
        ApiError error1 = ApiError.builder()
                .errorCode("CUSTOM")
                .message("custom msg")
                .path("/path")
                .timestamp(now)
                .validationErrors(Map.of("field", "err"))
                .details("extra")
                .build();

        ApiError error2 = new ApiError();
        error2.setErrorCode("CUSTOM");
        error2.setMessage("custom msg");
        error2.setPath("/path");
        error2.setTimestamp(now);
        error2.setValidationErrors(Map.of("field", "err"));
        error2.setDetails("extra");

        assertThat(error1).isEqualTo(error2);
        assertThat(error1.hashCode()).isEqualTo(error2.hashCode());
        assertThat(error1.toString()).contains("CUSTOM").contains("custom msg");
    }
}
