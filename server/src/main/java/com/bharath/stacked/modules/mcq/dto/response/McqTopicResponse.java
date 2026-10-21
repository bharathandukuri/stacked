package com.bharath.stacked.modules.mcq.dto.response;

import com.bharath.stacked.modules.mcq.model.McqTopic;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

public record McqTopicResponse(
        @NonNull String id,
        @NonNull String name,
        @NonNull String slug,
        @Nullable String description,
        long questionCount,
        @Nullable Instant createdAt,
        @Nullable Instant updatedAt) {

    public static McqTopicResponse fromModel(@NonNull McqTopic topic) {
        return new McqTopicResponse(
                topic.getId(),
                topic.getName(),
                topic.getSlug(),
                topic.getDescription(),
                topic.getQuestionCount(),
                topic.getCreatedAt(),
                topic.getUpdatedAt());
    }
}
