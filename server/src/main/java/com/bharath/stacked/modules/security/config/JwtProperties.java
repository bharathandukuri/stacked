package com.bharath.stacked.modules.security.config;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        @NonNull String secret,
        @DefaultValue("900000") long accessTokenExpirationMs,
        @DefaultValue("604800000") long refreshTokenExpirationMs) {
}
