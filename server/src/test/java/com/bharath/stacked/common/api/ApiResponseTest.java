package com.bharath.stacked.common.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiResponse Unit Tests")
class ApiResponseTest {

    @Test
    @DisplayName("success(data) creates 200 OK response with default message")
    void successWithDataOnly() {
        ResponseEntity<ApiResponse<String>> response = ApiResponse.success("test-payload");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("Success");
        assertThat(response.getBody().getData()).isEqualTo("test-payload");
        assertThat(response.getBody().getError()).isNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("success(data, message) creates 200 OK response with custom message")
    void successWithDataAndMessage() {
        ResponseEntity<ApiResponse<Integer>> response = ApiResponse.success(42, "Custom OK");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("Custom OK");
        assertThat(response.getBody().getData()).isEqualTo(42);
        assertThat(response.getBody().getError()).isNull();
    }

    @Test
    @DisplayName("created(data) creates 201 Created response with default message")
    void createdWithDataOnly() {
        ResponseEntity<ApiResponse<String>> response = ApiResponse.created("new-item");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("Resource created successfully");
        assertThat(response.getBody().getData()).isEqualTo("new-item");
        assertThat(response.getBody().getError()).isNull();
    }

    @Test
    @DisplayName("created(data, message) creates 201 Created response with custom message")
    void createdWithDataAndMessage() {
        ResponseEntity<ApiResponse<String>> response = ApiResponse.created("item", "Item created");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("Item created");
        assertThat(response.getBody().getData()).isEqualTo("item");
    }

    @Test
    @DisplayName("noContent() creates 204 No Content response")
    void noContentReturns204() {
        ResponseEntity<ApiResponse<Void>> response = ApiResponse.noContent();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @DisplayName("ok(data) and ok(message, data) aliases function identically to success")
    void okAliases() {
        ResponseEntity<ApiResponse<String>> res1 = ApiResponse.ok("data1");
        assertThat(res1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res1.getBody()).isNotNull();
        assertThat(res1.getBody().getData()).isEqualTo("data1");

        ResponseEntity<ApiResponse<String>> res2 = ApiResponse.ok("msg", "data2");
        assertThat(res2.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res2.getBody()).isNotNull();
        assertThat(res2.getBody().getMessage()).isEqualTo("msg");
        assertThat(res2.getBody().getData()).isEqualTo("data2");
    }

    @Test
    @DisplayName("error(apiError, status) creates error response with provided status")
    void errorWithApiErrorAndStatus() {
        ApiError apiError = ApiError.of("ERR_CODE", "Something went wrong", "/api/test");
        ResponseEntity<ApiResponse<Void>> response = ApiResponse.error(apiError, HttpStatus.BAD_REQUEST);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("Something went wrong");
        assertThat(response.getBody().getError()).isEqualTo(apiError);
        assertThat(response.getBody().getData()).isNull();
    }

    @Test
    @DisplayName("error(errorCode, message, path) creates error response from ErrorCode")
    void errorWithErrorCodeMessageAndPath() {
        ResponseEntity<ApiResponse<Void>> response = ApiResponse.error(
                ErrorCode.RESOURCE_NOT_FOUND,
                "User not found",
                "/api/users/123"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("User not found");
        assertThat(response.getBody().getError()).isNotNull();
        assertThat(response.getBody().getError().getErrorCode()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.getBody().getError().getPath()).isEqualTo("/api/users/123");
    }

    @Test
    @DisplayName("Builder, setters, equals, and hashCode test")
    void builderAndGettersSetters() {
        Instant now = Instant.now();
        ApiError err = ApiError.of("BAD", "error", "/test");

        ApiResponse<String> res = ApiResponse.<String>builder()
                .success(true)
                .message("OK")
                .data("sample")
                .error(err)
                .timestamp(now)
                .build();

        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getMessage()).isEqualTo("OK");
        assertThat(res.getData()).isEqualTo("sample");
        assertThat(res.getError()).isEqualTo(err);
        assertThat(res.getTimestamp()).isEqualTo(now);

        ApiResponse<String> res2 = new ApiResponse<>();
        res2.setSuccess(true);
        res2.setMessage("OK");
        res2.setData("sample");
        res2.setError(err);
        res2.setTimestamp(now);

        assertThat(res).isEqualTo(res2);
        assertThat(res.hashCode()).isEqualTo(res2.hashCode());
        assertThat(res.toString()).contains("sample");
    }
}
