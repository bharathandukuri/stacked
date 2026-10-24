package com.bharath.stacked.modules.security.ratelimit;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

@ConfigurationProperties(prefix = "stacked.rate-limit")
public record RateLimitProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("false") boolean trustForwardHeaders,
        @Nullable PolicyConfig auth,
        @Nullable PolicyConfig codeExecution,
        @Nullable PolicyConfig assessmentIntegrity,
        @Nullable PolicyConfig defaultApi) {

    public RateLimitProperties {
        if (auth == null) {
            auth = new PolicyConfig(10, 60, true, List.of(
                    "POST:/api/auth/login",
                    "POST:/api/auth/admin/login",
                    "POST:/api/auth/refresh"
            ));
        }
        if (codeExecution == null) {
            codeExecution = new PolicyConfig(15, 60, true, List.of(
                    "POST:/api/admin/coding-problem/run-preview",
                    "POST:/api/coding-problem/run-preview",
                    "POST:/api/admin/coding-problem/test-run",
                    "POST:/api/coding-problem/test-run",
                    "POST:/api/admin/coding-problem/run-generator",
                    "POST:/api/coding-problem/run-generator",
                    "POST:/api/admin/coding-problem/generate-testcases",
                    "POST:/api/coding-problem/generate-testcases",
                    "POST:/api/assessments/*/run-sample",
                    "POST:/api/assessments/*/run-custom-input",
                    "POST:/api/assessments/*/submit-code"
            ));
        }
        if (assessmentIntegrity == null) {
            assessmentIntegrity = new PolicyConfig(20, 60, false, List.of(
                    "POST:/api/assessments/join-by-code",
                    "POST:/api/assessments/*/start",
                    "POST:/api/assessments/*/submit"
            ));
        }
        if (defaultApi == null) {
            defaultApi = new PolicyConfig(120, 60, false, List.of(
                    "/api/**"
            ));
        }
    }

    public record PolicyConfig(
            @DefaultValue("10") int limit,
            @DefaultValue("60") int windowSeconds,
            @DefaultValue("true") boolean failClosed,
            @DefaultValue("") @NonNull List<String> paths) {
    }
}

