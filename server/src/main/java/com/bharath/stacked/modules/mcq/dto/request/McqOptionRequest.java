package com.bharath.stacked.modules.mcq.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;

public record McqOptionRequest(
        @Nullable String id,
        @NotBlank(message = "Option text cannot be empty.") String text,
        @Nullable String explanation) {
}

