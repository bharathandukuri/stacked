package com.bharath.stacked.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "cookie")
public record CookieProperties(
        @DefaultValue("true") boolean httpOnly,
        @DefaultValue("false") boolean secure,
        @DefaultValue("lax") String sameSite,
        @DefaultValue("") String domain,
        @DefaultValue("/") String path) {
}
