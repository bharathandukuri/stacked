package com.bharath.stacked.modules.mcq.dto.response;

import com.bharath.stacked.modules.mcq.enums.McqDifficulty;
import com.bharath.stacked.modules.mcq.enums.McqStatus;
import com.bharath.stacked.modules.mcq.enums.McqType;
import com.bharath.stacked.modules.mcq.model.McqQuestion;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;

public record McqQuestionSummaryResponse(
        @NonNull String id,
        @NonNull String title,
        @NonNull String slug,
        @NonNull String contentSnippet,
        @NonNull McqType type,
        @NonNull McqDifficulty difficulty,
        @NonNull McqStatus status,
        @NonNull List<String> topics,
        @NonNull List<String> tags,
        int optionsCount,
        int correctOptionsCount,
        double defaultPoints,
        double negativePoints,
        @Nullable Instant createdAt,
        @Nullable Instant updatedAt) {

    public static McqQuestionSummaryResponse fromModel(@NonNull McqQuestion question) {
        String snippet = question.getContent() != null ? question.getContent() : "";
        if (snippet.length() > 140) {
            snippet = snippet.substring(0, 140) + "...";
        }

        return new McqQuestionSummaryResponse(
                question.getId(),
                question.getTitle(),
                question.getSlug(),
                snippet,
                question.getType(),
                question.getDifficulty(),
                question.getStatus(),
                question.getTopics() != null ? question.getTopics() : List.of(),
                question.getTags() != null ? question.getTags() : List.of(),
                question.getOptions() != null ? question.getOptions().size() : 0,
                question.getCorrectOptionIds() != null ? question.getCorrectOptionIds().size() : 0,
                question.getDefaultPoints(),
                question.getNegativePoints(),
                question.getCreatedAt(),
                question.getUpdatedAt());
    }
}
