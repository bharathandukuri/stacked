package com.bharath.stacked.modules.mcq.dto.response;

import com.bharath.stacked.modules.mcq.enums.McqDifficulty;
import com.bharath.stacked.modules.mcq.enums.McqStatus;
import com.bharath.stacked.modules.mcq.enums.McqType;
import com.bharath.stacked.modules.mcq.model.McqQuestion;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;

public record McqQuestionDetailResponse(
        @NonNull String id,
        @NonNull String title,
        @NonNull String content,
        @NonNull McqType type,
        @NonNull McqDifficulty difficulty,
        @NonNull McqStatus status,
        @NonNull List<String> topics,
        @NonNull List<String> tags,
        @NonNull List<McqOptionResponse> options,
        @NonNull List<String> correctOptionIds,
        @Nullable String explanation,
        boolean shuffleOptions,
        @NonNull List<String> imageFileIds,
        @Nullable String authorId,
        @Nullable Instant createdAt,
        @Nullable Instant updatedAt) {

    public static McqQuestionDetailResponse fromModel(@NonNull McqQuestion question) {
        List<McqOptionResponse> optionResponses = question.getOptions() != null
                ? question.getOptions().stream().map(McqOptionResponse::fromModel).toList()
                : List.of();

        return new McqQuestionDetailResponse(
                question.getId(),
                question.getTitle(),
                question.getContent(),
                question.getType(),
                question.getDifficulty(),
                question.getStatus(),
                question.getTopics() != null ? question.getTopics() : List.of(),
                question.getTags() != null ? question.getTags() : List.of(),
                optionResponses,
                question.getCorrectOptionIds() != null ? question.getCorrectOptionIds() : List.of(),
                question.getExplanation(),
                question.isShuffleOptions(),
                question.getImageFileIds() != null ? question.getImageFileIds() : List.of(),
                question.getAuthorId(),
                question.getCreatedAt(),
                question.getUpdatedAt()
        );
    }
}

