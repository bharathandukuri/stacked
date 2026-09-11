package com.bharath.stacked.config;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @DefaultValue("http://localhost:3000") @NonNull String frontendUrl,
        @DefaultValue("http://localhost:8080") @NonNull String backendUrl,
        @DefaultValue({
                "http://localhost:3000",
                "http://localhost:5173",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:5173"
        }) @NonNull List<String> corsAllowedOrigins) {
}
