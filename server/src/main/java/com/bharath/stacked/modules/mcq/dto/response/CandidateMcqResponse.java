package com.bharath.stacked.modules.mcq.dto.response;

import com.bharath.stacked.modules.mcq.enums.McqDifficulty;
import com.bharath.stacked.modules.mcq.enums.McqType;
import com.bharath.stacked.modules.mcq.model.McqQuestion;
import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public record CandidateMcqResponse(
        @NonNull String id,
        @NonNull String title,
        @NonNull String content,
        @NonNull McqType type,
        @NonNull McqDifficulty difficulty,
        @NonNull List<String> topics,
        @NonNull List<CandidateMcqOptionResponse> options,
        double defaultPoints,
        double negativePoints,
        boolean shuffleOptions) {

    public static CandidateMcqResponse fromModel(@NonNull McqQuestion question, boolean doShuffle) {
        List<CandidateMcqOptionResponse> optionResponses = question.getOptions() != null
                ? question.getOptions().stream().map(CandidateMcqOptionResponse::fromModel).collect(Collectors.toList())
                : List.of();

        if (doShuffle && question.isShuffleOptions() && optionResponses.size() > 1) {
            Collections.shuffle(optionResponses);
        }

        return new CandidateMcqResponse(
                question.getId(),
                question.getTitle(),
                question.getContent(),
                question.getType(),
                question.getDifficulty(),
                question.getTopics() != null ? question.getTopics() : List.of(),
                optionResponses,
                question.getDefaultPoints(),
                question.getNegativePoints(),
                question.isShuffleOptions()
        );
    }
}

