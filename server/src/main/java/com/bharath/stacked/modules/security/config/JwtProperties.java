package com.bharath.stacked.modules.security.config;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.nio.charset.StandardCharsets;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        @NonNull String secret,
        @DefaultValue("900000") long accessTokenExpirationMs,
        @DefaultValue("604800000") long refreshTokenExpirationMs) {

    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT signing secret key must be configured and not blank. Set the JWT_SECRET environment variable.");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException(
                    "JWT signing secret key must be at least 256 bits (32 bytes) long. Provided length: "
                            + secret.getBytes(StandardCharsets.UTF_8).length + " bytes.");
        }
    }
}
