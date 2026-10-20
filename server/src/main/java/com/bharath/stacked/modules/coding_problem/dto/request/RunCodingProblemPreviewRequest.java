package com.bharath.stacked.modules.coding_problem.dto.request;

import com.bharath.stacked.modules.coding_problem.enums.CodingProblemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RunCodingProblemPreviewRequest {

    @NotNull(message = "Problem type is required")
    private CodingProblemType problemType;

    @NotBlank(message = "Reference solution language is required")
    private String referenceSolutionLanguage;

    @NotBlank(message = "Reference solution cannot be empty")
    private String referenceSolution;

    private boolean requireVerificationScript;

    private String verificationScriptLanguage;

    private String verificationScript;

    @NotEmpty(message = "At least one testcase is required")
    private List<PreviewTestcaseRequest> testcases;

    private Map<String, ExecutionConstraintRequest> executionConstraints;
}
