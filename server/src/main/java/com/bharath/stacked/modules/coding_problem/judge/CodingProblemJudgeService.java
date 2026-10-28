package com.bharath.stacked.modules.coding_problem.judge;

import com.bharath.stacked.modules.coding_problem.dto.request.PreviewTestcaseRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.TestcaseRequest;
import com.bharath.stacked.modules.coding_problem.dto.response.CodingProblemExecutionResponse;
import com.bharath.stacked.modules.coding_problem.dto.response.PreviewRunResponse;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemType;
import com.bharath.stacked.modules.coding_problem.model.TestCase;
import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import com.bharath.stacked.modules.language.Language;

import com.bharath.stacked.modules.coding_problem.dto.response.TestcaseGeneratorRunResponse;

import java.util.List;

public interface CodingProblemJudgeService {

    String DEFAULT_TESTCASE_DELIMITER = "---STACKED_TESTCASE_DELIMITER---";

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

    record JudgePreviewRequest(
            CodingProblemType problemType,
            Language referenceLanguage,
            String referenceSolution,
            CodeExecutionConstraints referenceConstraints,
            boolean requireVerificationScript,
            Language verificationScriptLanguage,
            String verificationScript,
            CodeExecutionConstraints verificationScriptConstraints,
            List<PreviewTestcaseRequest> testcases
    ) {}

    record CandidateTestcaseItem(
            String id,
            int caseIndex,
            String stdin,
            String dbInit,
            String expectedOutput,
            boolean isCustom,
            boolean isSample
    ) {}

    record JudgeCandidateExecutionRequest(
            CodingProblemType problemType,
            Language candidateLanguage,
            String candidateCode,
            CodeExecutionConstraints candidateConstraints,
            Language referenceLanguage,
            String referenceSolution,
            CodeExecutionConstraints referenceConstraints,
            boolean requireVerificationScript,
            Language verificationScriptLanguage,
            String verificationScript,
            CodeExecutionConstraints verificationScriptConstraints,
            List<CandidateTestcaseItem> testcases
    ) {}

    InitializedTestcases initializeExpectedOutputs(JudgeInitializationRequest request);

    PreviewRunResponse previewExecution(JudgePreviewRequest request);

    CodingProblemExecutionResponse executeCandidate(JudgeCandidateExecutionRequest request);

    TestcaseGeneratorRunResponse executeGenerator(
            Language language,
            String script,
            String stdin,
            CodeExecutionConstraints constraints
    );
}


