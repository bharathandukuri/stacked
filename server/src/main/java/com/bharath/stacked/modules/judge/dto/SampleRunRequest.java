package com.bharath.stacked.modules.judge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record SampleRunRequest(
        @NotBlank(message = "Language identifier is required") @NonNull String languageId,

        @NotBlank(message = "Solution code is required") @NonNull String solutionCode,

        @Nullable String validatorCode,

        @NotEmpty(message = "At least one testcase is required for execution") @NonNull List<TestCaseDto> testCases) {
}
