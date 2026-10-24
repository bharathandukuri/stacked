package com.bharath.stacked.modules.security.config;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "admin.default")
public record AdminProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("") @NonNull String email,
        @DefaultValue("") @NonNull String password,
        @DefaultValue("Platform Admin") @NonNull String name) {
}
