package com.bharath.stacked.modules.coding_problem.model;

import com.bharath.stacked.modules.coding_problem.enums.CodingProblemDifficulty;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemStatus;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemType;
import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Document(collection = "coding_problem")
public class CodingProblem {

    @Id
    private String id;

    private String title;

    @Indexed(unique = true)
    private String slug;

    @Builder.Default
    private CodingProblemStatus status = CodingProblemStatus.UNPUBLISHED;

    private CodingProblemType problemType;

    private CodingProblemDifficulty difficulty;

    private List<String> supportedLanguageIds;

    private String statement;

    private List<String> topics;

    private List<String> hints;

    // language, code
    private Map<String, String> starterCode;

    // since we need reference solution for judging, one solution is enough
    private String referenceSolutionLanguage;

    private String referenceSolution;

    // if there are multiple valid answers then we are also considering verification
    // script
    private boolean requireVerificationScript;

    private String verificationScriptLanguage;

    private String verificationScript;

    private List<TestCase> sampleTestCases;

    private List<TestCase> hiddenTestCases;

    // languageId -> CodeExecutionConstraints
    private Map<String, CodeExecutionConstraints> executionConstraints;
}