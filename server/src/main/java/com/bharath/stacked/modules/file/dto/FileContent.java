package com.bharath.stacked.modules.file.dto;

import org.jspecify.annotations.NonNull;
import org.springframework.core.io.Resource;

public record FileContent(
        @NonNull Resource resource,
        @NonNull String contentType,
        @NonNull String originalFileName,
        long contentLength) {
}
