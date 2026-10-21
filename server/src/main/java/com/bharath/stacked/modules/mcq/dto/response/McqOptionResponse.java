package com.bharath.stacked.modules.mcq.dto.response;

import com.bharath.stacked.modules.mcq.model.McqOption;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record McqOptionResponse(
        @NonNull String id,
        @NonNull String text,
        @Nullable String explanation) {

    public static McqOptionResponse fromModel(@NonNull McqOption option) {
        return new McqOptionResponse(
                option.getId(),
                option.getText(),
                option.getExplanation()
        );
    }
}

