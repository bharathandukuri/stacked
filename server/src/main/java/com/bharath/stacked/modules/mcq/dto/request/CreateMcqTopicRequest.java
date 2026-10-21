package com.bharath.stacked.modules.mcq.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;

public record CreateMcqTopicRequest(
        @NotBlank(message = "Topic name is required.") String name,
        @Nullable String description) {
}

