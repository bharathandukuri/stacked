package com.bharath.stacked.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        @DefaultValue("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970") String secret,
        @DefaultValue("900000") long accessTokenExpirationMs,
        @DefaultValue("604800000") long refreshTokenExpirationMs) {
}
