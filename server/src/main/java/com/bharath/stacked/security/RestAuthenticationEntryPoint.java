package com.bharath.stacked.security;

import com.bharath.stacked.common.api.ApiError;
import com.bharath.stacked.common.api.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Slf4j
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        log.warn("Unauthorized access attempt to [{} {}]: {}",
                request.getMethod(), request.getRequestURI(), authException.getMessage());

        ApiError apiError = ApiError.of(
                ErrorCode.UNAUTHORIZED.name(),
                "Full authentication is required to access this resource.",
                request.getRequestURI());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String json = String.format(
                "{\"success\":false,\"message\":\"%s\",\"error\":{\"errorCode\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"},\"timestamp\":\"%s\"}",
                escapeJson(apiError.getMessage()),
                escapeJson(apiError.getErrorCode()),
                escapeJson(apiError.getMessage()),
                escapeJson(apiError.getPath()),
                Instant.now().toString());

        response.getWriter().write(json);
    }

    private String escapeJson(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
