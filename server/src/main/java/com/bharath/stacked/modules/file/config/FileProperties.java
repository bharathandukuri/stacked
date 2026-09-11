package com.bharath.stacked.modules.file.config;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "file.storage")
public record FileProperties(
        @DefaultValue("uploads/temp") @NonNull String tempPath,
        @DefaultValue("uploads/permanent") @NonNull String permanentPath,
        @DefaultValue("10485760") long maxFileSizeBytes,
        @DefaultValue({
                "image/jpeg",
                "image/png",
                "image/gif",
                "image/webp",
                "image/svg+xml",
                "application/pdf",
                "text/plain",
                "text/csv",
                "application/json",
                "application/zip",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        }) @NonNull List<String> allowedContentTypes,
        @DefaultValue("PT1H") @NonNull Duration tempTtl,
        @DefaultValue("0 0 * * * *") @NonNull String cleanupCron) {
}
