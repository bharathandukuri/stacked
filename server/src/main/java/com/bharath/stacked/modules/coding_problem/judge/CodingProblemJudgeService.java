package com.bharath.stacked.modules.coding_problem.judge;

import com.bharath.stacked.modules.coding_problem.dto.request.TestcaseRequest;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemType;
import com.bharath.stacked.modules.coding_problem.model.TestCase;
import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import com.bharath.stacked.modules.language.Language;

import java.util.List;

public interface CodingProblemJudgeService {

    String VERIFICATION_DELIMITER = "---STACKED_INPUT_END---";

    record InitializedTestcases(
            List<TestCase> sampleTestCases,
            List<TestCase> hiddenTestCases
    ) {}

    record JudgeInitializationRequest(
            CodingProblemType problemType,
            Language referenceLanguage,
            String referenceSolution,
            CodeExecutionConstraints referenceConstraints,
            boolean requireVerificationScript,
            Language verificationScriptLanguage,
            String verificationScript,
            CodeExecutionConstraints verificationScriptConstraints,
            List<TestcaseRequest> sampleTestcases,
            List<TestcaseRequest> hiddenTestcases
    ) {}

    InitializedTestcases initializeExpectedOutputs(JudgeInitializationRequest request);
}

