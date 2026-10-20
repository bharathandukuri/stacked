package com.bharath.stacked.modules.coding_problem.dto.request;

import com.bharath.stacked.modules.coding_problem.enums.CodingProblemDifficulty;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;
import java.util.Map;

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

    private boolean requireVerificationScript;

    private String verificationScriptLanguage;

    private String verificationScript;

    @Valid
    @NotEmpty(message = "At least one sample test case is required")
    @Size(max = 20, message = "Sample testcases must not exceed 20")
    private List<TestcaseRequest> sampleTestcases;

    @Valid
    @NotEmpty(message = "At least one hidden test case is required")
    @Size(max = 20, message = "Hidden testcases must not exceed 20")
    private List<TestcaseRequest> hiddenTestcases;

    private Map<
            @NotBlank(message = "Language ID cannot be blank") String,
            @Valid ExecutionConstraintRequest
    > executionConstraints;

    private String generatorLanguage;

    private String generatorScript;

    private String generatorStdin;

    private String generatorDelimiter;

    private List<String> imageFileIds;

    public CreateCodingProblemRequest() {
    }

    public CreateCodingProblemRequest(
            String title,
            String slug,
            CodingProblemType problemType,
            CodingProblemDifficulty difficulty,
            List<String> supportedLanguageIds,
            String statement,
            List<String> topics,
            List<String> hints,
            Map<String, String> starterCode,
            String referenceSolutionLanguage,
            String referenceSolution,
            boolean requireVerificationScript,
            String verificationScriptLanguage,
            String verificationScript,
            List<TestcaseRequest> sampleTestcases,
            List<TestcaseRequest> hiddenTestcases,
            Map<String, ExecutionConstraintRequest> executionConstraints,
            String generatorLanguage,
            String generatorScript,
            String generatorStdin,
            String generatorDelimiter,
            List<String> imageFileIds
    ) {
        this.title = title;
        this.slug = slug;
        this.problemType = problemType;
        this.difficulty = difficulty;
        this.supportedLanguageIds = supportedLanguageIds;
        this.statement = statement;
        this.topics = topics;
        this.hints = hints;
        this.starterCode = starterCode;
        this.referenceSolutionLanguage = referenceSolutionLanguage;
        this.referenceSolution = referenceSolution;
        this.requireVerificationScript = requireVerificationScript;
        this.verificationScriptLanguage = verificationScriptLanguage;
        this.verificationScript = verificationScript;
        this.sampleTestcases = sampleTestcases;
        this.hiddenTestcases = hiddenTestcases;
        this.executionConstraints = executionConstraints;
        this.generatorLanguage = generatorLanguage;
        this.generatorScript = generatorScript;
        this.generatorStdin = generatorStdin;
        this.generatorDelimiter = generatorDelimiter;
        this.imageFileIds = imageFileIds;
    }
}
