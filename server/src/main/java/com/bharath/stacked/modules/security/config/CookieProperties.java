package com.bharath.stacked.modules.security.config;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "cookie")
public record CookieProperties(
        @DefaultValue("true") boolean httpOnly,
        @DefaultValue("false") boolean secure,
        @DefaultValue("lax") @NonNull String sameSite,
        @Nullable String domain,
        @DefaultValue("/") @NonNull String path) {
}
