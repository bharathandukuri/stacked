package com.bharath.stacked.modules.assessment.model;

import com.bharath.stacked.modules.coding_problem.enums.CodingProblemType;
import com.bharath.stacked.modules.coding_problem.model.TestCase;
import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import com.bharath.stacked.modules.mcq.enums.McqType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionSnapshot {
    private String title;

    // MCQ specific snapshot fields
    private String mcqContent;
    private McqType mcqType;
    @Builder.Default
    private List<OptionSnapshot> options = new ArrayList<>();
    @Builder.Default
    private List<String> correctOptionIds = new ArrayList<>(); // Kept server-side

    // Coding Problem specific snapshot fields
    private String codingStatement;
    private CodingProblemType codingProblemType;
    @Builder.Default
    private List<String> supportedLanguageIds = new ArrayList<>();
    private Map<String, String> starterCode;
    private CodeExecutionConstraints executionConstraints;
    @Builder.Default
    private List<TestCase> sampleTestCases = new ArrayList<>();
    @Builder.Default
    private List<TestCase> hiddenTestCases = new ArrayList<>(); // Kept server-side
    private String referenceSolutionLanguage;
    private String referenceSolution;
    private boolean requireVerificationScript;
    private String verificationScriptLanguage;
    private String verificationScript;
}

