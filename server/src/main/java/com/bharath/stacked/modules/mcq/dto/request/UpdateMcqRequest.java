package com.bharath.stacked.modules.mcq.dto.request;

import com.bharath.stacked.modules.mcq.enums.McqDifficulty;
import com.bharath.stacked.modules.mcq.enums.McqStatus;
import com.bharath.stacked.modules.mcq.enums.McqType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record UpdateMcqRequest(
        @NotBlank(message = "Title is required.") String title,
        @NotBlank(message = "Content / question stem is required.") String content,
        @NotNull(message = "Question type is required.") McqType type,
        @NotNull(message = "Difficulty is required.") McqDifficulty difficulty,
        @NotNull(message = "Status is required.") McqStatus status,
        @Nullable List<String> topics,
        @Nullable List<String> tags,
        @NotNull(message = "Options list cannot be null.")
        @Size(min = 2, message = "At least two options are required.")
        List<@Valid @NonNull McqOptionRequest> options,
        @NotEmpty(message = "At least one correct option must be specified.")
        List<String> correctOptionIds,
        @Nullable String explanation,
        @Nullable Boolean shuffleOptions,
        @Nullable List<String> imageFileIds) {
}

