package com.bharath.stacked.modules.security.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Security Handlers Unit Tests")
class SecurityHandlersTest {

    @Test
    @DisplayName("RestAuthenticationEntryPoint returns 401 with JSON ApiError response")
    void commenceAuthenticationEntryPoint() throws IOException {
        RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/protected");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("Invalid token \"signature\"\n"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");

        String content = response.getContentAsString();
        assertThat(content)
                .contains("\"success\":false")
                .contains("\"errorCode\":\"UNAUTHORIZED\"")
                .contains("\"path\":\"/api/protected\"")
                .contains("Full authentication is required to access this resource.");
    }

    @Test
    @DisplayName("RestAccessDeniedHandler returns 403 with JSON ApiError response")
    void handleAccessDenied() throws IOException {
        RestAccessDeniedHandler handler = new RestAccessDeniedHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/users");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("Access is denied\twith\\details\r"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");

        String content = response.getContentAsString();
        assertThat(content)
                .contains("\"success\":false")
                .contains("\"errorCode\":\"FORBIDDEN\"")
                .contains("\"path\":\"/api/admin/users\"")
                .contains("Access denied: You do not have permission to access this resource.");
    }
}
