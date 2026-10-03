package com.bharath.stacked.common.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErrorCode Unit Tests")
class ErrorCodeTest {

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    @DisplayName("Every ErrorCode has non-null HttpStatus and non-blank default message")
    void allConstantsHaveValidStatusAndMessage(ErrorCode errorCode) {
        assertThat(errorCode.getHttpStatus()).isNotNull();
        assertThat(errorCode.getDefaultMessage()).isNotBlank();
    }

    @Test
    @DisplayName("Verify mappings of specific ErrorCodes to HTTP status codes")
    void specificHttpStatusMappings() {
        assertThat(ErrorCode.BAD_REQUEST.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ErrorCode.VALIDATION_FAILED.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(ErrorCode.UNAUTHORIZED.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ErrorCode.FORBIDDEN.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ErrorCode.RESOURCE_NOT_FOUND.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ErrorCode.RESOURCE_ALREADY_EXISTS.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ErrorCode.CONFLICT.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ErrorCode.RATE_LIMIT_EXCEEDED.getHttpStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(ErrorCode.SERVICE_UNAVAILABLE.getHttpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("ErrorCode valueOf and values work correctly")
    void enumStandardMethods() {
        assertThat(ErrorCode.valueOf("BAD_REQUEST")).isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(ErrorCode.values()).hasSize(10);
    }
}
