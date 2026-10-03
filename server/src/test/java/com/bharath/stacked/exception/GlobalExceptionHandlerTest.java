package com.bharath.stacked.exception;

import com.bharath.stacked.common.api.ApiResponse;
import com.bharath.stacked.common.api.ErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest("GET", "/api/test");
    }

    @Test
    @DisplayName("handleAppException handles AppException and maps ErrorCode and details")
    void handleAppException() {
        AppException ex = new AppException("Custom failure", ErrorCode.RESOURCE_NOT_FOUND, Map.of("key", "val"));

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleAppException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("Custom failure");
        assertThat(response.getBody().getError()).isNotNull();
        assertThat(response.getBody().getError().getErrorCode()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.getBody().getError().getPath()).isEqualTo("/api/test");
        assertThat(response.getBody().getError().getDetails()).isEqualTo(Map.of("key", "val"));
    }

    @Test
    @DisplayName("handleMethodArgumentNotValid extracts field errors")
    void handleMethodArgumentNotValid() throws NoSuchMethodException {
        Method method = this.getClass().getDeclaredMethod("setUp");
        MethodParameter parameter = new MethodParameter(method, -1);

        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "email", "must not be blank"));
        bindingResult.addError(new FieldError("target", "password", "must be at least 6 chars"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleMethodArgumentNotValid(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError().getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.getBody().getError().getValidationErrors())
                .containsEntry("email", "must not be blank")
                .containsEntry("password", "must be at least 6 chars");
    }

    @Test
    @DisplayName("handleHandlerMethodValidation handles HandlerMethodValidationException with FieldError and generic errors")
    void handleHandlerMethodValidation() {
        HandlerMethodValidationException ex = mock(HandlerMethodValidationException.class);
        FieldError fieldError = new FieldError("object", "paramName", "cannot be null");
        ObjectError genericError = new ObjectError("object", "generic parameter error");

        doReturn(List.of(fieldError, genericError)).when(ex).getAllErrors();

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleHandlerMethodValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getValidationErrors())
                .containsEntry("paramName", "cannot be null")
                .containsEntry("parameter", "generic parameter error");
    }

    @Test
    @DisplayName("handleConstraintViolation handles ConstraintViolationException")
    void handleConstraintViolation() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("user.age");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must be greater than 18");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleConstraintViolation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getValidationErrors())
                .containsEntry("user.age", "must be greater than 18");
    }

    @Test
    @DisplayName("handleHttpMessageNotReadable handles malformed JSON")
    void handleHttpMessageNotReadable() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error", null);

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleHttpMessageNotReadable(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST.name());
        assertThat(response.getBody().getError().getMessage()).contains("Malformed JSON request body");
    }

    @Test
    @DisplayName("handleMethodArgumentTypeMismatch handles type mismatch with known and unknown type")
    void handleMethodArgumentTypeMismatch() {
        MethodArgumentTypeMismatchException exKnown = new MethodArgumentTypeMismatchException(
                "abc", Integer.class, "id", null, null);

        ResponseEntity<ApiResponse<Void>> resKnown = exceptionHandler.handleMethodArgumentTypeMismatch(exKnown, request);

        assertThat(resKnown.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resKnown.getBody()).isNotNull();
        assertThat(resKnown.getBody().getMessage()).isEqualTo("Parameter 'id' should be of type 'Integer'");

        MethodArgumentTypeMismatchException exUnknown = new MethodArgumentTypeMismatchException(
                "abc", null, "id", null, null);

        ResponseEntity<ApiResponse<Void>> resUnknown = exceptionHandler.handleMethodArgumentTypeMismatch(exUnknown, request);
        assertThat(resUnknown.getBody()).isNotNull();
        assertThat(resUnknown.getBody().getMessage()).isEqualTo("Parameter 'id' should be of type 'unknown'");
    }

    @Test
    @DisplayName("handleMethodNotSupported handles 405 Method Not Allowed")
    void handleMethodNotSupported() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("POST");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleMethodNotSupported(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("HTTP method 'POST' is not supported for this endpoint.");
    }

    @Test
    @DisplayName("handleMediaTypeNotSupported handles 415 Unsupported Media Type")
    void handleMediaTypeNotSupported() {
        HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException(
                MediaType.APPLICATION_XML,
                List.of(MediaType.APPLICATION_JSON)
        );

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleMediaTypeNotSupported(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("Content-Type");
    }

    @Test
    @DisplayName("handleNoResourceFound handles 404 NoResourceFoundException")
    void handleNoResourceFound() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/unknown/path", "Not found");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleNoResourceFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.name());
        assertThat(response.getBody().getMessage()).contains("The requested path '/api/test' was not found on this server.");
    }

    @Test
    @DisplayName("handleMaxUploadSizeExceeded handles 413 CONTENT_TOO_LARGE")
    void handleMaxUploadSizeExceeded() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(1024);

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleMaxUploadSizeExceeded(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("Uploaded file exceeds the maximum allowed size limit.");
    }

    @Test
    @DisplayName("handleGenericException catches unhandled exceptions and returns 500")
    void handleGenericException() {
        Exception ex = new NullPointerException("Simulated crash");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleGenericException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.name());
        assertThat(response.getBody().getMessage()).contains("An unexpected internal error occurred");
    }
}
