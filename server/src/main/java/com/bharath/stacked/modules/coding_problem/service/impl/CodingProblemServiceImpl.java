package com.bharath.stacked.modules.coding_problem.service.impl;

import com.bharath.stacked.exception.BadRequestException;
import com.bharath.stacked.exception.ConflictException;
import com.bharath.stacked.modules.coding_problem.dto.request.CreateCodingProblemRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.TestcaseRequest;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemType;
import com.bharath.stacked.modules.coding_problem.model.CodingProblem;
import com.bharath.stacked.modules.coding_problem.model.ExecutionLimits;
import com.bharath.stacked.modules.coding_problem.model.TestCase;
import com.bharath.stacked.modules.coding_problem.repository.CodingProblemRepository;
import com.bharath.stacked.modules.coding_problem.service.CodingProblemService;
import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import com.bharath.stacked.modules.execution.dto.request.SimpleCodeExecutionRequest;
import com.bharath.stacked.modules.execution.dto.response.SimpleCodeExecutionResult;
import com.bharath.stacked.modules.execution.enums.CodeExecutionStatus;
import com.bharath.stacked.modules.execution.service.CodeExecutionService;
import com.bharath.stacked.modules.language.Language;
import com.bharath.stacked.modules.language.LanguageType;
import com.bharath.stacked.modules.language.registry.LanguageRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class CodingProblemServiceImpl implements CodingProblemService {

    private final CodingProblemRepository codingProblemRepository;
    private final LanguageRegistry languageRegistry;
    private final CodeExecutionService codeExecutionService;

    public CodingProblemServiceImpl(CodingProblemRepository codingProblemRepository,
                                    LanguageRegistry languageRegistry,
                                    CodeExecutionService codeExecutionService) {
        super();
        this.codingProblemRepository = codingProblemRepository;
        this.languageRegistry = languageRegistry;
        this.codeExecutionService = codeExecutionService;
    }

    @Override
    public CodingProblem create(CreateCodingProblemRequest request) {
        // 1. Validate uniqueness of slug
        if (codingProblemRepository.findBySlug(request.getSlug()).isPresent()) {
            throw new ConflictException("CodingProblem", "slug", request.getSlug());
        }

        // 2. Validate supported languages and problem type compatibility
        for (String langId : request.getSupportedLanguageIds()) {
            if (!languageRegistry.isSupported(langId)) {
                throw new BadRequestException("Language '" + langId + "' is not supported by the platform.");
            }
            Language lang = languageRegistry.get(langId);
            if (request.getProblemType() == CodingProblemType.DATABASE && lang.type() != LanguageType.DATABASE) {
                throw new BadRequestException("Language '" + langId + "' is not a database language for a DATABASE problem.");
            }
            if (request.getProblemType() == CodingProblemType.GENERIC && lang.type() == LanguageType.DATABASE) {
                throw new BadRequestException("Database language '" + langId + "' cannot be used for a GENERIC problem.");
            }
        }

        // 3. Validate reference solution language
        if (!request.getSupportedLanguageIds().contains(request.getReferenceSolutionLanguage())) {
            throw new BadRequestException("Reference solution language '" + request.getReferenceSolutionLanguage()
                    + "' must be included in supportedLanguageIds.");
        }
        Language refLanguage = languageRegistry.get(request.getReferenceSolutionLanguage());

        // 4. Validate starter code mapping
        if (request.getStarterCode() != null) {
            for (String langId : request.getStarterCode().keySet()) {
                if (!request.getSupportedLanguageIds().contains(langId)) {
                    throw new BadRequestException("Starter code provided for unsupported language: '" + langId + "'.");
                }
            }
        }

        // 5. Validate verification script if multiple valid answers exist
        if (request.isRequireVerificationScript()) {
            if (request.getVerificationScriptLanguage() == null || request.getVerificationScriptLanguage().isBlank()) {
                throw new BadRequestException("Verification script language is required.");
            }
            if (request.getVerificationScript() == null || request.getVerificationScript().isBlank()) {
                throw new BadRequestException("Verification script is required.");
            }
            if (!languageRegistry.isSupported(request.getVerificationScriptLanguage())) {
                throw new BadRequestException("Verification script language '" + request.getVerificationScriptLanguage()
                        + "' is not supported.");
            }
        }

        // 6. Resolve execution limits
        ExecutionLimits limits = request.getExecutionLimits() != null
                ? ExecutionLimits.builder()
                        .timeLimitMs(request.getExecutionLimits().getTimeLimitMs())
                        .memoryLimitKb(request.getExecutionLimits().getMemoryLimitKb())
                        .outputLimitBytes(request.getExecutionLimits().getOutputLimitBytes())
                        .queryTimeoutMs(request.getExecutionLimits().getQueryTimeoutMs())
                        .build()
                : ExecutionLimits.defaults();

        CodeExecutionConstraints constraints = new CodeExecutionConstraints(
                limits.getTimeLimitMs(),
                limits.getMemoryLimitKb()
        );

        // 7. Validate and generate expected outputs for sample and hidden testcases
        List<TestCase> sampleTestCases = executeAndBuildTestCases(
                request.getSampleTestcases(),
                request.getProblemType(),
                refLanguage,
                request.getReferenceSolution(),
                constraints
        );

        List<TestCase> hiddenTestCases = executeAndBuildTestCases(
                request.getHiddenTestcases(),
                request.getProblemType(),
                refLanguage,
                request.getReferenceSolution(),
                constraints
        );

        // 8. Build entity and persist
        CodingProblem codingProblem = CodingProblem.builder()
                .title(request.getTitle())
                .slug(request.getSlug())
                .problemType(request.getProblemType())
                .difficulty(request.getDifficulty())
                .supportedLanguageIds(request.getSupportedLanguageIds())
                .statement(request.getStatement())
                .topics(request.getTopics())
                .hints(request.getHints())
                .starterCode(request.getStarterCode())
                .referenceSolutionLanguage(request.getReferenceSolutionLanguage())
                .referenceSolution(request.getReferenceSolution())
                .multipleValidAnswers(request.isRequireVerificationScript())
                .verificationScriptLanguage(request.getVerificationScriptLanguage())
                .verificationScript(request.getVerificationScript())
                .sampleTestCases(sampleTestCases)
                .hiddenTestCases(hiddenTestCases)
                .executionLimits(limits)
                .build();

        return codingProblemRepository.save(codingProblem);
    }

    private List<TestCase> executeAndBuildTestCases(
            List<TestcaseRequest> testcaseRequests,
            CodingProblemType problemType,
            Language refLanguage,
            String referenceSolution,
            CodeExecutionConstraints constraints
    ) {
        List<TestCase> testCases = new ArrayList<>();
        if (testcaseRequests == null) {
            return testCases;
        }

        for (TestcaseRequest tcReq : testcaseRequests) {
            String codeToRun;
            if (problemType == CodingProblemType.DATABASE) {
                if (tcReq.getDbInit() != null && !tcReq.getDbInit().isBlank()) {
                    codeToRun = tcReq.getDbInit() + "\n" + referenceSolution;
                } else {
                    codeToRun = referenceSolution;
                }
            } else {
                codeToRun = referenceSolution;
            }

            SimpleCodeExecutionRequest execRequest = SimpleCodeExecutionRequest.builder()
                    .language(refLanguage)
                    .code(codeToRun)
                    .stdin(tcReq.getStdin())
                    .constraints(constraints)
                    .build();

            SimpleCodeExecutionResult result = codeExecutionService.run(execRequest);
            if (result.executionStatus() != CodeExecutionStatus.SUCCESS || (result.exitCode() != null && result.exitCode() != 0)) {
                log.warn("Reference solution execution failed for problem. Status: {}, exitCode: {}, stderr: {}",
                        result.executionStatus(), result.exitCode(), result.stderr());
                throw new BadRequestException("Reference solution failed execution: " +
                        (result.stderr() != null && !result.stderr().isBlank()
                                ? result.stderr()
                                : result.executionStatus().name()));
            }

            String expectedOutput = result.stdout() != null ? result.stdout().trim() : "";

            TestCase testCase = new TestCase();
            testCase.setId(UUID.randomUUID().toString());
            testCase.setStdin(tcReq.getStdin());
            testCase.setDbInit(tcReq.getDbInit());
            testCase.setOutput(expectedOutput);
            testCases.add(testCase);
        }

        return testCases;
    }
}
