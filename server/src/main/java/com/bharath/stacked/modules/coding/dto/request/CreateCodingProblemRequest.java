package com.bharath.stacked.modules.coding.dto.request;

import com.bharath.stacked.modules.coding.enums.CodingProblemDifficulty;
import com.bharath.stacked.modules.coding.enums.CodingProblemType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreateCodingProblemRequest {

    @NotBlank(message = "Title cannot be empty")
    @Size(max = 150, message = "Title must not exceed 150 characters")
    private String title;

    @NotBlank(message = "Slug cannot be empty")
    @Size(max = 180, message = "Slug must not exceed 180 characters")
    @Pattern(
            regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
            message = "Slug must contain lowercase letters, numbers, and single hyphens"
    )
    private String slug;

    @NotNull(message = "Problem type is required")
    private CodingProblemType problemType;

    @NotNull(message = "Difficulty is required")
    private CodingProblemDifficulty difficulty;

    @NotEmpty(message = "At least one supported language is required")
    private List<@NotBlank(message = "Language ID cannot be blank") String>
            supportedLanguageIds;

    @NotBlank(message = "Problem statement cannot be empty")
    private String statement;

    @NotEmpty(message = "At least one topic is required")
    private List<@NotBlank(message = "Topic cannot be blank") String> topics;

    private List<@NotBlank(message = "Hint cannot be blank") String> hints;

    private Map<
            @NotBlank(message = "Starter code language cannot be blank") String,
            String
            > starterCode;

    @NotBlank(message = "Reference solution language is required")
    private String referenceSolutionLanguage;

    @NotBlank(message = "Reference solution cannot be empty")
    private String referenceSolution;

    private boolean requireVerificationScript = false;

    private String verificationScriptLanguage;

    private String verificationScript;

    @Valid
    @NotEmpty(message = "At least one sample test case is required")
    @Size(max = 10, message = "Sample testcases must not exceed 10")
    private List<TestcaseRequest> sampleTestcases;

    @Valid
    @NotEmpty(message = "At least one hidden test case is required")
    @Size(max = 10, message = "Hidden testcases must not exceed 10")
    private List<TestcaseRequest> hiddenTestcases;

    @NotNull(message = "Execution limits are required")
    @Valid
    private ExecutionLimitsRequest executionLimits;
}