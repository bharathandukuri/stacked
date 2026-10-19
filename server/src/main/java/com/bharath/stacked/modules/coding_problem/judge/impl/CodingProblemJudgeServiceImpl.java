package com.bharath.stacked.modules.coding_problem.judge.impl;

import com.bharath.stacked.exception.BadRequestException;
import com.bharath.stacked.modules.coding_problem.dto.request.TestcaseRequest;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemType;
import com.bharath.stacked.modules.coding_problem.judge.CodingProblemJudgeService;
import com.bharath.stacked.modules.coding_problem.model.TestCase;
import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import com.bharath.stacked.modules.execution.dto.request.BatchCodeExecutionRequest;
import com.bharath.stacked.modules.execution.dto.request.BatchExecutionItem;
import com.bharath.stacked.modules.execution.dto.response.BatchCodeExecutionResult;
import com.bharath.stacked.modules.execution.dto.response.SimpleCodeExecutionResult;
import com.bharath.stacked.modules.execution.enums.CodeExecutionStatus;
import com.bharath.stacked.modules.execution.service.CodeExecutionService;
import com.bharath.stacked.modules.language.Language;
import com.bharath.stacked.modules.language.LanguageType;
import com.bharath.stacked.modules.language.registry.LanguageFileNameRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class CodingProblemJudgeServiceImpl implements CodingProblemJudgeService {

    private final CodeExecutionService codeExecutionService;
    private final LanguageFileNameRegistry languageFileNameRegistry;

    public CodingProblemJudgeServiceImpl(
            CodeExecutionService codeExecutionService,
            LanguageFileNameRegistry languageFileNameRegistry
    ) {
        this.codeExecutionService = codeExecutionService;
        this.languageFileNameRegistry = languageFileNameRegistry;
    }

    @Override
    public InitializedTestcases initializeExpectedOutputs(JudgeInitializationRequest request) {
        validateRequest(request);

        List<TestcaseRequest> allTestcases = new ArrayList<>(
                request.sampleTestcases().size() + request.hiddenTestcases().size()
        );
        allTestcases.addAll(request.sampleTestcases());
        allTestcases.addAll(request.hiddenTestcases());

        int totalCount = allTestcases.size();
        int sampleCount = request.sampleTestcases().size();

        List<String> finalOutputs;
        if (request.problemType() == CodingProblemType.DATABASE) {
            finalOutputs = executeDatabaseTestcases(request, allTestcases);
        } else {
            finalOutputs = executeGenericTestcases(request, allTestcases);
        }

        if (finalOutputs == null || finalOutputs.size() != totalCount) {
            int actual = finalOutputs != null ? finalOutputs.size() : 0;
            throw new BadRequestException("Expected output count mismatch: expected "
                    + totalCount + " but got " + actual);
        }

        // Partition final outputs into sample and hidden TestCases preserving input ordering
        List<TestCase> sampleTestCases = new ArrayList<>(sampleCount);
        for (int i = 0; i < sampleCount; i++) {
            TestcaseRequest tcReq = request.sampleTestcases().get(i);
            TestCase tc = new TestCase();
            tc.setId(UUID.randomUUID().toString());
            tc.setStdin(tcReq.getStdin());
            tc.setDbInit(tcReq.getDbInit());
            tc.setOutput(finalOutputs.get(i));
            sampleTestCases.add(tc);
        }

        int hiddenCount = request.hiddenTestcases().size();
        List<TestCase> hiddenTestCases = new ArrayList<>(hiddenCount);
        for (int i = 0; i < hiddenCount; i++) {
            int globalIndex = sampleCount + i;
            TestcaseRequest tcReq = request.hiddenTestcases().get(i);
            TestCase tc = new TestCase();
            tc.setId(UUID.randomUUID().toString());
            tc.setStdin(tcReq.getStdin());
            tc.setDbInit(tcReq.getDbInit());
            tc.setOutput(finalOutputs.get(globalIndex));
            hiddenTestCases.add(tc);
        }

        return new InitializedTestcases(sampleTestCases, hiddenTestCases);
    }

    // =========================================================================
    // Generic Workflow (Non-Database)
    // =========================================================================

    private List<String> executeGenericTestcases(JudgeInitializationRequest request, List<TestcaseRequest> testcases) {
        // 1. Run reference solution in batch across all testcases
        String refFileName = languageFileNameRegistry.resolveReferenceSolutionFileName(request.referenceLanguage());
        List<String> refStdins = new ArrayList<>(testcases.size());
        for (TestcaseRequest tc : testcases) {
            refStdins.add(tc.getStdin() != null ? tc.getStdin() : "");
        }

        List<String> referenceOutputs = executeGenericBatch(
                "Reference solution",
                request.referenceLanguage(),
                refFileName,
                request.referenceSolution(),
                request.referenceConstraints(),
                refStdins
        );

        // 2. If verification is not required, reference outputs are the final outputs
        if (!request.requireVerificationScript()) {
            return referenceOutputs;
        }

        // 3. Run verification script in batch across all testcases with formatted delimiter stdin
        String verFileName = languageFileNameRegistry.resolveVerificationScriptFileName(request.verificationScriptLanguage());
        List<String> verStdins = new ArrayList<>(testcases.size());
        for (int i = 0; i < testcases.size(); i++) {
            verStdins.add(formatVerificationInput(testcases.get(i).getStdin(), referenceOutputs.get(i)));
        }

        return executeGenericBatch(
                "Verification script",
                request.verificationScriptLanguage(),
                verFileName,
                request.verificationScript(),
                request.verificationScriptConstraints(),
                verStdins
        );
    }

    private List<String> executeGenericBatch(
            String stage,
            Language language,
            String fileName,
            String code,
            CodeExecutionConstraints constraints,
            List<String> stdins
    ) {
        int count = stdins.size();
        Map<String, String> files = Map.of(fileName, code);

        List<BatchExecutionItem> items = new ArrayList<>(count);
        for (String stdin : stdins) {
            items.add(BatchExecutionItem.builder()
                    .executablePath(fileName)
                    .stdin(stdin)
                    .constraints(constraints)
                    .build());
        }

        BatchCodeExecutionRequest batchRequest = BatchCodeExecutionRequest.builder()
                .language(language)
                .files(files)
                .constraints(constraints)
                .executions(items)
                .build();

        BatchCodeExecutionResult batchResult = codeExecutionService.run(batchRequest);
        if (batchResult == null) {
            throw new BadRequestException(stage + " batch execution returned null result");
        }
        if (batchResult.results() == null || batchResult.results().size() != count) {
            int actual = batchResult.results() != null ? batchResult.results().size() : 0;
            throw new BadRequestException(stage + " batch execution result count mismatch: expected "
                    + count + " but got " + actual);
        }

        List<String> outputs = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            SimpleCodeExecutionResult result = batchResult.results().get(i);
            validateExecutionResult(stage, i + 1, result);
            outputs.add(result.stdout());
        }
        return outputs;
    }

    // =========================================================================
    // Database Workflow (Per-Testcase Container Isolation)
    // =========================================================================

    private List<String> executeDatabaseTestcases(JudgeInitializationRequest request, List<TestcaseRequest> testcases) {
        int count = testcases.size();
        Language dbLanguage = request.referenceLanguage();

        String initFileName = languageFileNameRegistry.resolveDatabaseInitFileName(dbLanguage);
        String solutionFileName = languageFileNameRegistry.resolveDatabaseSolutionFileName(dbLanguage);
        String verifierFileName = request.requireVerificationScript()
                ? languageFileNameRegistry.resolveVerificationScriptFileName(request.verificationScriptLanguage())
                : null;

        List<String> outputs = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            int testcaseNumber = i + 1;
            TestcaseRequest tc = testcases.get(i);
            boolean hasDbInit = tc.getDbInit() != null && !tc.getDbInit().isBlank();

            Map<String, String> files = new LinkedHashMap<>();
            List<BatchExecutionItem> items = new ArrayList<>();

            if (hasDbInit) {
                files.put(initFileName, tc.getDbInit());
                items.add(BatchExecutionItem.builder()
                        .executablePath(initFileName)
                        .constraints(request.referenceConstraints())
                        .build());
            }

            files.put(solutionFileName, request.referenceSolution());
            items.add(BatchExecutionItem.builder()
                        .executablePath(solutionFileName)
                        .constraints(request.referenceConstraints())
                        .build());

            if (request.requireVerificationScript()) {
                files.put(verifierFileName, request.verificationScript());
                items.add(BatchExecutionItem.builder()
                        .executablePath(verifierFileName)
                        .constraints(request.verificationScriptConstraints())
                        .build());
            }

            BatchCodeExecutionRequest batchRequest = BatchCodeExecutionRequest.builder()
                    .language(dbLanguage)
                    .files(files)
                    .constraints(request.referenceConstraints())
                    .executions(items)
                    .build();

            BatchCodeExecutionResult batchResult = codeExecutionService.run(batchRequest);
            if (batchResult == null) {
                throw new BadRequestException("Database batch execution returned null result on testcase " + testcaseNumber);
            }
            if (batchResult.results() == null || batchResult.results().size() != items.size()) {
                int actual = batchResult.results() != null ? batchResult.results().size() : 0;
                throw new BadRequestException("Database execution result count mismatch on testcase "
                        + testcaseNumber + ": expected " + items.size() + " but got " + actual);
            }

            int resultIndex = 0;
            if (hasDbInit) {
                SimpleCodeExecutionResult initResult = batchResult.results().get(resultIndex++);
                validateExecutionResult("Database initialization script", testcaseNumber, initResult);
            }

            SimpleCodeExecutionResult solutionResult = batchResult.results().get(resultIndex++);
            validateExecutionResult("Reference solution", testcaseNumber, solutionResult);

            if (request.requireVerificationScript()) {
                SimpleCodeExecutionResult verResult = batchResult.results().get(resultIndex++);
                validateExecutionResult("Verification script", testcaseNumber, verResult);
                outputs.add(verResult.stdout());
            } else {
                outputs.add(solutionResult.stdout());
            }
        }

        return outputs;
    }

    // =========================================================================
    // Delimiter Formatting Contract
    // =========================================================================

    public static String formatVerificationInput(String stdin, String referenceStdout) {
        String input = stdin != null ? stdin : "";
        String output = referenceStdout != null ? referenceStdout : "";
        return input + "\n" + VERIFICATION_DELIMITER + "\n" + output;
    }

    // =========================================================================
    // Result Validation & Diagnostic Extraction
    // =========================================================================

    private void validateExecutionResult(String stage, int testcaseNumber, SimpleCodeExecutionResult result) {
        if (result == null) {
            throw new BadRequestException(stage + " execution returned null result on testcase " + testcaseNumber);
        }
        if (result.executionStatus() == null) {
            String details = getErrorDetails(result);
            throw new BadRequestException(stage + " execution missing status on testcase " + testcaseNumber
                    + " [exitCode=" + result.exitCode() + "]: " + details);
        }
        boolean nonZeroExit = result.exitCode() != null && result.exitCode() != 0L;
        boolean abnormalSignal = result.exitSignal() != null && result.exitSignal() != 0L;
        if (result.executionStatus() != CodeExecutionStatus.SUCCESS || nonZeroExit || abnormalSignal) {
            String details = getErrorDetails(result);
            StringBuilder sb = new StringBuilder();
            sb.append(stage).append(" failed on testcase ").append(testcaseNumber);
            sb.append(" [status=").append(result.executionStatus());
            if (result.exitCode() != null) {
                sb.append(", exitCode=").append(result.exitCode());
            }
            if (abnormalSignal) {
                sb.append(", signal=").append(result.exitSignal());
            }
            sb.append("]: ").append(details);
            throw new BadRequestException(sb.toString());
        }
    }

    private String getErrorDetails(SimpleCodeExecutionResult result) {
        if (result == null) {
            return "No details available";
        }
        if (result.stderr() != null && !result.stderr().isBlank()) {
            return result.stderr().strip();
        }
        if (result.logs() != null && !result.logs().isEmpty()) {
            return String.join("; ", result.logs());
        }
        if (result.stdout() != null && !result.stdout().isBlank()) {
            return result.stdout().strip();
        }
        return result.executionStatus() != null ? result.executionStatus().name() : "Unknown execution failure";
    }

    // =========================================================================
    // Request Validation
    // =========================================================================

    private void validateRequest(JudgeInitializationRequest request) {
        if (request == null) {
            throw new BadRequestException("Judge initialization request cannot be null.");
        }
        if (request.problemType() == null) {
            throw new BadRequestException("Problem type is required.");
        }
        if (request.referenceLanguage() == null) {
            throw new BadRequestException("Reference solution language is required.");
        }
        if (request.referenceSolution() == null || request.referenceSolution().isBlank()) {
            throw new BadRequestException("Reference solution cannot be empty.");
        }
        if (request.referenceConstraints() == null) {
            throw new BadRequestException("Reference solution execution constraints are required.");
        }

        if (request.problemType() == CodingProblemType.DATABASE && request.referenceLanguage().type() != LanguageType.DATABASE) {
            throw new BadRequestException("Reference solution language '" + request.referenceLanguage().id()
                    + "' is not a database language for a DATABASE problem.");
        }
        if (request.problemType() == CodingProblemType.GENERIC && request.referenceLanguage().type() == LanguageType.DATABASE) {
            throw new BadRequestException("Database language '" + request.referenceLanguage().id()
                    + "' cannot be used as reference solution for a GENERIC problem.");
        }

        if (request.sampleTestcases() == null || request.sampleTestcases().isEmpty()) {
            throw new BadRequestException("At least one sample test case is required.");
        }
        if (request.hiddenTestcases() == null || request.hiddenTestcases().isEmpty()) {
            throw new BadRequestException("At least one hidden test case is required.");
        }

        for (int i = 0; i < request.sampleTestcases().size(); i++) {
            if (request.sampleTestcases().get(i) == null) {
                throw new BadRequestException("Sample testcase at index " + i + " cannot be null.");
            }
        }
        for (int i = 0; i < request.hiddenTestcases().size(); i++) {
            if (request.hiddenTestcases().get(i) == null) {
                throw new BadRequestException("Hidden testcase at index " + i + " cannot be null.");
            }
        }

        if (request.requireVerificationScript()) {
            if (request.verificationScriptLanguage() == null) {
                throw new BadRequestException("Verification script language is required.");
            }
            if (request.verificationScript() == null || request.verificationScript().isBlank()) {
                throw new BadRequestException("Verification script is required.");
            }
            if (request.verificationScriptConstraints() == null) {
                throw new BadRequestException("Verification script execution constraints are required.");
            }

            if (request.problemType() == CodingProblemType.DATABASE) {
                if (request.verificationScriptLanguage().type() != LanguageType.DATABASE) {
                    throw new BadRequestException("Verification script for a DATABASE problem must be a database language.");
                }
                if (!request.verificationScriptLanguage().id().equalsIgnoreCase(request.referenceLanguage().id())) {
                    throw new BadRequestException("Verification script language '" + request.verificationScriptLanguage().id()
                            + "' must match reference solution language '" + request.referenceLanguage().id()
                            + "' for a DATABASE problem.");
                }
            }
            if (request.problemType() == CodingProblemType.GENERIC && request.verificationScriptLanguage().type() == LanguageType.DATABASE) {
                throw new BadRequestException("Database language '" + request.verificationScriptLanguage().id()
                        + "' cannot be used as verification script for a GENERIC problem.");
            }
        }
    }
}
