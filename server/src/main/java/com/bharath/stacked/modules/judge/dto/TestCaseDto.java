package com.bharath.stacked.modules.judge.dto;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record TestCaseDto(
        @NonNull String id,
        @NonNull String input,
        @Nullable String expectedOutput,
        boolean isSample) {
}
