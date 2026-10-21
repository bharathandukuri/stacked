package com.bharath.stacked.modules.mcq.dto.response;

import com.bharath.stacked.modules.mcq.model.McqOption;
import org.jspecify.annotations.NonNull;

public record CandidateMcqOptionResponse(
        @NonNull String id,
        @NonNull String text) {

    public static CandidateMcqOptionResponse fromModel(@NonNull McqOption option) {
        return new CandidateMcqOptionResponse(
                option.getId(),
                option.getText()
        );
    }
}

