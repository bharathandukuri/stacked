package com.bharath.stacked.modules.coding_problem.judge.impl;

import com.bharath.stacked.exception.BadRequestException;
import com.bharath.stacked.modules.coding_problem.dto.request.PreviewTestcaseRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.TestcaseRequest;
import com.bharath.stacked.modules.coding_problem.dto.response.CodingProblemExecutionResponse;
import com.bharath.stacked.modules.coding_problem.dto.response.CodingProblemTestcaseResultResponse;
import com.bharath.stacked.modules.coding_problem.dto.response.PreviewRunResponse;
import com.bharath.stacked.modules.coding_problem.dto.response.PreviewTestcaseResultResponse;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemType;
import com.bharath.stacked.modules.coding_problem.judge.CodingProblemJudgeService;
import com.bharath.stacked.modules.coding_problem.model.TestCase;
import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import com.bharath.stacked.modules.coding_problem.dto.response.TestcaseGeneratorRunResponse;
import com.bharath.stacked.modules.execution.dto.request.SimpleCodeExecutionRequest;
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
import java.util.HashMap;
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

        // 3. Run verification script in batch across all testcases with 3-file args
        List<String> stdins = new ArrayList<>(testcases.size());
        for (TestcaseRequest tc : testcases) {
            stdins.add(tc.getStdin() != null ? tc.getStdin() : "");
        }

        BatchCodeExecutionResult verBatchResult = runVerificationBatch(
                request.verificationScriptLanguage(),
                request.verificationScript(),
                request.verificationScriptConstraints(),
                stdins,
                referenceOutputs,
                referenceOutputs
        );

        if (verBatchResult == null || verBatchResult.results() == null) {
            throw new BadRequestException("Verification script batch execution returned null result");
        }

        for (int i = 0; i < testcases.size(); i++) {
            SimpleCodeExecutionResult verResult = verBatchResult.results().get(i);
            validateExecutionResult("Verification script", i + 1, verResult);
        }

        // Expected output is always the reference solution output (never the verification script's output)
        return referenceOutputs;
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
    // Verification Batch Execution (3-File Arguments)
    // =========================================================================

    private BatchCodeExecutionResult runVerificationBatch(
            Language verLanguage,
            String verCode,
            CodeExecutionConstraints verConstraints,
            List<String> inputs,
            List<String> candidates,
            List<String> expecteds
    ) {
        int count = inputs.size();
        String verFileName = languageFileNameRegistry.resolveVerificationScriptFileName(verLanguage);
        Map<String, String> files = new HashMap<>();
        files.put(verFileName, verCode);

        List<BatchExecutionItem> items = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String inputFile = "input_" + i + ".txt";
            String candFile = "candidate_" + i + ".txt";
            String expectedFile = "expected_" + i + ".txt";

            files.put(inputFile, inputs.get(i) != null ? inputs.get(i) : "");
            files.put(candFile, candidates.get(i) != null ? candidates.get(i) : "");
            files.put(expectedFile, expecteds.get(i) != null ? expecteds.get(i) : "");

            items.add(BatchExecutionItem.builder()
                    .executablePath(verFileName)
                    .args(List.of(inputFile, candFile, expectedFile))
                    .stdin("")
                    .constraints(verConstraints)
                    .build());
        }

        BatchCodeExecutionRequest batchRequest = BatchCodeExecutionRequest.builder()
                .language(verLanguage)
                .files(files)
                .constraints(verConstraints)
                .executions(items)
                .build();

        return codeExecutionService.run(batchRequest);
    }

    public static boolean compareOutputs(String actual, String expected) {
        if (actual == null && expected == null) return true;
        if (actual == null || expected == null) return false;
        return normalizeOutput(actual).equals(normalizeOutput(expected));
    }

    public static String normalizeOutput(String s) {
        if (s == null) return "";
        String normalized = s.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalized.split("\n", -1);
        List<String> trimmed = new ArrayList<>(lines.length);
        for (String line : lines) {
            trimmed.add(line.stripTrailing());
        }
        while (!trimmed.isEmpty() && trimmed.get(trimmed.size() - 1).isEmpty()) {
            trimmed.remove(trimmed.size() - 1);
        }
        return String.join("\n", trimmed);
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

    // =========================================================================
    // Preview Execution Workflow (Non-throwing diagnostic evaluation)
    // =========================================================================

    @Override
    public PreviewRunResponse previewExecution(JudgePreviewRequest request) {
        validatePreviewRequest(request);

        if (request.problemType() == CodingProblemType.DATABASE) {
            return previewDatabaseExecution(request);
        } else {
            return previewGenericExecution(request);
        }
    }

    private PreviewRunResponse previewGenericExecution(JudgePreviewRequest request) {
        int count = request.testcases().size();
        String refFileName = languageFileNameRegistry.resolveReferenceSolutionFileName(request.referenceLanguage());

        List<BatchExecutionItem> items = new ArrayList<>(count);
        for (PreviewTestcaseRequest tc : request.testcases()) {
            items.add(BatchExecutionItem.builder()
                    .executablePath(refFileName)
                    .stdin(tc.getStdin() != null ? tc.getStdin() : "")
                    .constraints(request.referenceConstraints())
                    .build());
        }

        BatchCodeExecutionRequest batchRequest = BatchCodeExecutionRequest.builder()
                .language(request.referenceLanguage())
                .files(Map.of(refFileName, request.referenceSolution()))
                .constraints(request.referenceConstraints())
                .executions(items)
                .build();

        BatchCodeExecutionResult refBatchResult = codeExecutionService.run(batchRequest);
        if (refBatchResult == null || refBatchResult.results() == null) {
            return buildErrorResponse("Execution service returned null batch result", count);
        }

        // If verification script is NOT enabled:
        if (!request.requireVerificationScript()) {
            List<PreviewTestcaseResultResponse> results = new ArrayList<>(count);
            int passedCount = 0;
            long totalTimeMs = 0L;
            boolean hasCompileError = false;

            for (int i = 0; i < count; i++) {
                PreviewTestcaseRequest tc = request.testcases().get(i);
                SimpleCodeExecutionResult r = i < refBatchResult.results().size()
                        ? refBatchResult.results().get(i)
                        : null;

                PreviewTestcaseResultResponse res = mapSimpleResultToResponse(
                        tc != null && tc.getId() != null ? tc.getId() : "case-" + (i + 1),
                        i + 1,
                        r,
                        null
                );
                results.add(res);
                if ("SUCCESS".equals(res.getStatus()) || "PASSED".equals(res.getStatus())) {
                    passedCount++;
                }
                if ("COMPILATION_ERROR".equals(res.getStatus())) {
                    hasCompileError = true;
                }
                if (res.getRuntimeMs() != null) {
                    totalTimeMs += res.getRuntimeMs();
                }
            }

            String overallStatus = hasCompileError
                    ? "COMPILATION_ERROR"
                    : (passedCount == count ? "SUCCESS" : "FAILED");

            return PreviewRunResponse.builder()
                    .overallStatus(overallStatus)
                    .totalExecutionTimeMs(totalTimeMs)
                    .totalTestcases(count)
                    .passedTestcases(passedCount)
                    .failedTestcases(count - passedCount)
                    .logs(buildLogsText(request.referenceLanguage().name(), count, passedCount, totalTimeMs, overallStatus, false, null))
                    .results(results)
                    .build();
        }

        // If verification script IS enabled:
        // First check if reference solution failed with compilation error
        boolean refHasCompileError = false;
        for (SimpleCodeExecutionResult r : refBatchResult.results()) {
            if (r != null && r.executionStatus() == CodeExecutionStatus.COMPILATION_ERROR) {
                refHasCompileError = true;
                break;
            }
        }

        if (refHasCompileError) {
            List<PreviewTestcaseResultResponse> results = new ArrayList<>(count);
            long totalTimeMs = 0L;
            for (int i = 0; i < count; i++) {
                PreviewTestcaseRequest tc = request.testcases().get(i);
                SimpleCodeExecutionResult r = i < refBatchResult.results().size() ? refBatchResult.results().get(i) : null;
                PreviewTestcaseResultResponse res = mapSimpleResultToResponse(
                        tc != null && tc.getId() != null ? tc.getId() : "case-" + (i + 1),
                        i + 1,
                        r,
                        null
                );
                results.add(res);
                if (res.getRuntimeMs() != null) totalTimeMs += res.getRuntimeMs();
            }
            return PreviewRunResponse.builder()
                    .overallStatus("COMPILATION_ERROR")
                    .totalExecutionTimeMs(totalTimeMs)
                    .totalTestcases(count)
                    .passedTestcases(0)
                    .failedTestcases(count)
                    .logs(buildLogsText(request.referenceLanguage().name(), count, 0, totalTimeMs, "COMPILATION_ERROR", true, request.verificationScriptLanguage().name()))
                    .results(results)
                    .build();
        }

        // Run verification script in batch across all testcases
        List<String> stdins = new ArrayList<>(count);
        List<String> candOutputs = new ArrayList<>(count);
        List<String> expectedOutputs = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            PreviewTestcaseRequest tc = request.testcases().get(i);
            SimpleCodeExecutionResult refRes = i < refBatchResult.results().size() ? refBatchResult.results().get(i) : null;
            String refOut = refRes != null && refRes.stdout() != null ? refRes.stdout() : "";
            stdins.add(tc != null && tc.getStdin() != null ? tc.getStdin() : "");
            candOutputs.add(refOut);
            expectedOutputs.add(refOut);
        }

        BatchCodeExecutionResult verBatchResult = runVerificationBatch(
                request.verificationScriptLanguage(),
                request.verificationScript(),
                request.verificationScriptConstraints(),
                stdins,
                candOutputs,
                expectedOutputs
        );

        if (verBatchResult == null || verBatchResult.results() == null) {
            return buildErrorResponse("Verification script execution returned null batch result", count);
        }

        List<PreviewTestcaseResultResponse> results = new ArrayList<>(count);
        int passedCount = 0;
        long totalTimeMs = 0L;
        boolean hasVerifierCompileError = false;

        for (int i = 0; i < count; i++) {
            PreviewTestcaseRequest tc = request.testcases().get(i);
            SimpleCodeExecutionResult refRes = i < refBatchResult.results().size() ? refBatchResult.results().get(i) : null;
            SimpleCodeExecutionResult verRes = i < verBatchResult.results().size()
                    ? verBatchResult.results().get(i)
                    : null;

            String tcId = tc != null && tc.getId() != null ? tc.getId() : "case-" + (i + 1);

            // If reference solution failed for this testcase (runtime error, timeout, etc.)
            boolean refFailed = refRes == null || refRes.executionStatus() != CodeExecutionStatus.SUCCESS
                    || (refRes.exitCode() != null && refRes.exitCode() != 0L);

            if (refFailed) {
                PreviewTestcaseResultResponse res = mapSimpleResultToResponse(tcId, i + 1, refRes, null);
                results.add(res);
                if (res.getRuntimeMs() != null) totalTimeMs += res.getRuntimeMs();
                continue;
            }

            // Reference solution succeeded. Evaluate verifier output
            long execTime = (refRes.executionTimeMs() != null ? refRes.executionTimeMs() : 0L)
                    + (verRes != null && verRes.executionTimeMs() != null ? verRes.executionTimeMs() : 0L);
            totalTimeMs += execTime;

            Double memoryMb = refRes.memoryUsageKb() != null
                    ? Math.round(refRes.memoryUsageKb() / 1024.0 * 10.0) / 10.0
                    : null;

            if (verRes == null) {
                results.add(PreviewTestcaseResultResponse.builder()
                        .testCaseId(tcId)
                        .caseIndex(i + 1)
                        .status("FAILED")
                        .stdout(refRes.stdout())
                        .stderr("Verifier produced no result")
                        .error("Verifier produced no result")
                        .runtimeMs(execTime)
                        .memoryMb(memoryMb)
                        .build());
                continue;
            }

            if (verRes.executionStatus() == CodeExecutionStatus.COMPILATION_ERROR) {
                hasVerifierCompileError = true;
                results.add(PreviewTestcaseResultResponse.builder()
                        .testCaseId(tcId)
                        .caseIndex(i + 1)
                        .status("COMPILATION_ERROR")
                        .stdout(refRes.stdout())
                        .stderr(verRes.stderr())
                        .error("Verification script compilation failed")
                        .runtimeMs(execTime)
                        .memoryMb(memoryMb)
                        .logs(verRes.logs())
                        .build());
                continue;
            }

            boolean verPass = verRes.executionStatus() == CodeExecutionStatus.SUCCESS
                    && (verRes.exitCode() == null || verRes.exitCode() == 0L);

            if (verPass) {
                passedCount++;
                results.add(PreviewTestcaseResultResponse.builder()
                        .testCaseId(tcId)
                        .caseIndex(i + 1)
                        .status("SUCCESS")
                        .stdout(refRes.stdout())
                        .stderr(verRes.stderr())
                        .exitCode(0L)
                        .runtimeMs(execTime)
                        .memoryMb(memoryMb)
                        .expectedOutput(verRes.stdout() != null && !verRes.stdout().isBlank() ? verRes.stdout().strip() : "Verifier passed (exit code 0)")
                        .logs(refRes.logs())
                        .build());
            } else {
                results.add(PreviewTestcaseResultResponse.builder()
                        .testCaseId(tcId)
                        .caseIndex(i + 1)
                        .status("FAILED")
                        .stdout(refRes.stdout())
                        .stderr(verRes.stderr() != null && !verRes.stderr().isBlank() ? verRes.stderr().strip() : "Assertion failed in verification script")
                        .exitCode(verRes.exitCode())
                        .runtimeMs(execTime)
                        .memoryMb(memoryMb)
                        .expectedOutput(verRes.stdout() != null && !verRes.stdout().isBlank() ? verRes.stdout().strip() : null)
                        .error("Verifier assertion failed [exitCode=" + verRes.exitCode() + "]")
                        .logs(verRes.logs())
                        .build());
            }
        }

        String overallStatus = hasVerifierCompileError
                ? "COMPILATION_ERROR"
                : (passedCount == count ? "SUCCESS" : "FAILED");

        return PreviewRunResponse.builder()
                .overallStatus(overallStatus)
                .totalExecutionTimeMs(totalTimeMs)
                .totalTestcases(count)
                .passedTestcases(passedCount)
                .failedTestcases(count - passedCount)
                .logs(buildLogsText(request.referenceLanguage().name(), count, passedCount, totalTimeMs, overallStatus, true, request.verificationScriptLanguage().name()))
                .results(results)
                .build();
    }

    private PreviewRunResponse previewDatabaseExecution(JudgePreviewRequest request) {
        int count = request.testcases().size();
        Language dbLanguage = request.referenceLanguage();
        String initFileName = languageFileNameRegistry.resolveDatabaseInitFileName(dbLanguage);
        String solutionFileName = languageFileNameRegistry.resolveDatabaseSolutionFileName(dbLanguage);
        String verifierFileName = request.requireVerificationScript()
                ? languageFileNameRegistry.resolveVerificationScriptFileName(dbLanguage)
                : null;

        List<PreviewTestcaseResultResponse> results = new ArrayList<>(count);
        int passedCount = 0;
        long totalTimeMs = 0L;

        for (int i = 0; i < count; i++) {
            PreviewTestcaseRequest tc = request.testcases().get(i);
            String tcId = tc != null && tc.getId() != null ? tc.getId() : "case-" + (i + 1);
            boolean hasDbInit = tc != null && tc.getDbInit() != null && !tc.getDbInit().isBlank();

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
            if (batchResult == null || batchResult.results() == null || batchResult.results().size() != items.size()) {
                results.add(PreviewTestcaseResultResponse.builder()
                        .testCaseId(tcId)
                        .caseIndex(i + 1)
                        .status("FAILED")
                        .stderr("Database container execution returned unexpected result")
                        .error("Database execution returned unexpected result")
                        .build());
                continue;
            }

            int resultIndex = 0;
            SimpleCodeExecutionResult initResult = null;
            if (hasDbInit) {
                initResult = batchResult.results().get(resultIndex++);
            }
            SimpleCodeExecutionResult solutionResult = batchResult.results().get(resultIndex++);
            SimpleCodeExecutionResult verResult = request.requireVerificationScript()
                    ? batchResult.results().get(resultIndex++)
                    : null;

            long caseTime = (solutionResult.executionTimeMs() != null ? solutionResult.executionTimeMs() : 0L);
            totalTimeMs += caseTime;

            Double memoryMb = solutionResult.memoryUsageKb() != null
                    ? Math.round(solutionResult.memoryUsageKb() / 1024.0 * 10.0) / 10.0
                    : null;

            // Check init failure
            if (initResult != null && (initResult.executionStatus() != CodeExecutionStatus.SUCCESS || (initResult.exitCode() != null && initResult.exitCode() != 0L))) {
                results.add(PreviewTestcaseResultResponse.builder()
                        .testCaseId(tcId)
                        .caseIndex(i + 1)
                        .status("RUNTIME_ERROR")
                        .stderr("Database initialization failed: " + (initResult.stderr() != null ? initResult.stderr().strip() : ""))
                        .error("Database initialization failed")
                        .exitCode(initResult.exitCode())
                        .runtimeMs(caseTime)
                        .memoryMb(memoryMb)
                        .logs(initResult.logs())
                        .build());
                continue;
            }

            // Check solution failure
            if (solutionResult.executionStatus() != CodeExecutionStatus.SUCCESS || (solutionResult.exitCode() != null && solutionResult.exitCode() != 0L)) {
                results.add(PreviewTestcaseResultResponse.builder()
                        .testCaseId(tcId)
                        .caseIndex(i + 1)
                        .status(solutionResult.executionStatus() != null ? solutionResult.executionStatus().name() : "RUNTIME_ERROR")
                        .stdout(solutionResult.stdout())
                        .stderr(solutionResult.stderr())
                        .error("SQL query execution failed")
                        .exitCode(solutionResult.exitCode())
                        .runtimeMs(caseTime)
                        .memoryMb(memoryMb)
                        .logs(solutionResult.logs())
                        .build());
                continue;
            }

            // Check verifier failure if required
            if (request.requireVerificationScript() && verResult != null) {
                boolean verPass = verResult.executionStatus() == CodeExecutionStatus.SUCCESS
                        && (verResult.exitCode() == null || verResult.exitCode() == 0L);
                if (verPass) {
                    passedCount++;
                    results.add(PreviewTestcaseResultResponse.builder()
                            .testCaseId(tcId)
                            .caseIndex(i + 1)
                            .status("SUCCESS")
                            .stdout(solutionResult.stdout())
                            .stderr(verResult.stderr())
                            .expectedOutput(verResult.stdout() != null && !verResult.stdout().isBlank() ? verResult.stdout().strip() : "Verifier passed (exit code 0)")
                            .exitCode(0L)
                            .runtimeMs(caseTime)
                            .memoryMb(memoryMb)
                            .logs(solutionResult.logs())
                            .build());
                } else {
                    results.add(PreviewTestcaseResultResponse.builder()
                            .testCaseId(tcId)
                            .caseIndex(i + 1)
                            .status("FAILED")
                            .stdout(solutionResult.stdout())
                            .stderr(verResult.stderr() != null && !verResult.stderr().isBlank() ? verResult.stderr().strip() : "Assertion failed in verification script")
                            .expectedOutput(verResult.stdout())
                            .error("Verifier assertion failed [exitCode=" + verResult.exitCode() + "]")
                            .exitCode(verResult.exitCode())
                            .runtimeMs(caseTime)
                            .memoryMb(memoryMb)
                            .logs(verResult.logs())
                            .build());
                }
            } else {
                // No verifier, solution succeeded
                passedCount++;
                results.add(PreviewTestcaseResultResponse.builder()
                        .testCaseId(tcId)
                        .caseIndex(i + 1)
                        .status("SUCCESS")
                        .stdout(solutionResult.stdout())
                        .stderr(solutionResult.stderr())
                        .exitCode(0L)
                        .runtimeMs(caseTime)
                        .memoryMb(memoryMb)
                        .logs(solutionResult.logs())
                        .build());
            }
        }

        String overallStatus = passedCount == count ? "SUCCESS" : "FAILED";

        return PreviewRunResponse.builder()
                .overallStatus(overallStatus)
                .totalExecutionTimeMs(totalTimeMs)
                .totalTestcases(count)
                .passedTestcases(passedCount)
                .failedTestcases(count - passedCount)
                .logs(buildLogsText(dbLanguage.name(), count, passedCount, totalTimeMs, overallStatus, request.requireVerificationScript(), dbLanguage.name()))
                .results(results)
                .build();
    }

    // =========================================================================
    // Candidate Execution Workflow (Sample Runs and Submissions)
    // =========================================================================

    @Override
    public CodingProblemExecutionResponse executeCandidate(JudgeCandidateExecutionRequest request) {
        validateCandidateExecutionRequest(request);

        if (request.problemType() == CodingProblemType.DATABASE) {
            return executeCandidateDatabase(request);
        } else {
            return executeCandidateGeneric(request);
        }
    }

    private CodingProblemExecutionResponse executeCandidateGeneric(JudgeCandidateExecutionRequest request) {
        int count = request.testcases().size();
        Language candLang = request.candidateLanguage();
        String candFileName = languageFileNameRegistry.resolveReferenceSolutionFileName(candLang);

        // 1. Check if reference solution outputs are needed:
        // - Always needed if verification script is required (as fallback expected output on failure).
        // - Needed for any testcase where expectedOutput is null (e.g. custom testcases).
        boolean hasRef = request.referenceLanguage() != null && request.referenceSolution() != null && !request.referenceSolution().isBlank();
        boolean needsRefRun = hasRef && (request.requireVerificationScript()
                || request.testcases().stream().anyMatch(tc -> tc.expectedOutput() == null));

        List<String> referenceOutputs = new ArrayList<>(count);
        if (needsRefRun) {
            String refFileName = languageFileNameRegistry.resolveReferenceSolutionFileName(request.referenceLanguage());
            List<BatchExecutionItem> refItems = new ArrayList<>(count);
            for (CandidateTestcaseItem tc : request.testcases()) {
                refItems.add(BatchExecutionItem.builder()
                        .executablePath(refFileName)
                        .stdin(tc.stdin() != null ? tc.stdin() : "")
                        .constraints(request.referenceConstraints())
                        .build());
            }

            BatchCodeExecutionRequest refBatchRequest = BatchCodeExecutionRequest.builder()
                    .language(request.referenceLanguage())
                    .files(Map.of(refFileName, request.referenceSolution()))
                    .constraints(request.referenceConstraints())
                    .executions(refItems)
                    .build();

            BatchCodeExecutionResult refBatchResult = codeExecutionService.run(refBatchRequest);
            if (refBatchResult != null && refBatchResult.results() != null) {
                for (int i = 0; i < count; i++) {
                    if (i < refBatchResult.results().size()) {
                        SimpleCodeExecutionResult r = refBatchResult.results().get(i);
                        if (r != null && r.executionStatus() == CodeExecutionStatus.SUCCESS && (r.exitCode() == null || r.exitCode() == 0L)) {
                            referenceOutputs.add(r.stdout());
                        } else {
                            String err = r != null && r.stderr() != null && !r.stderr().isBlank()
                                    ? r.stderr().strip()
                                    : (r != null ? "Process exited with code " + r.exitCode() : "Execution failed");
                            referenceOutputs.add("(Reference solution error: " + err + ")");
                        }
                    } else {
                        referenceOutputs.add(null);
                    }
                }
            }
        }

        // 2. Run candidate code in batch across all testcases
        List<BatchExecutionItem> candItems = new ArrayList<>(count);
        for (CandidateTestcaseItem tc : request.testcases()) {
            candItems.add(BatchExecutionItem.builder()
                    .executablePath(candFileName)
                    .stdin(tc.stdin() != null ? tc.stdin() : "")
                    .constraints(request.candidateConstraints())
                    .build());
        }

        BatchCodeExecutionRequest candBatchRequest = BatchCodeExecutionRequest.builder()
                .language(candLang)
                .files(Map.of(candFileName, request.candidateCode()))
                .constraints(request.candidateConstraints())
                .executions(candItems)
                .build();

        BatchCodeExecutionResult candBatchResult = codeExecutionService.run(candBatchRequest);
        if (candBatchResult == null || candBatchResult.results() == null) {
            throw new BadRequestException("Execution service returned null result for candidate code");
        }

        // Check if candidate code had compilation error
        boolean candCompileError = false;
        String compileStderr = null;
        for (SimpleCodeExecutionResult r : candBatchResult.results()) {
            if (r != null && r.executionStatus() == CodeExecutionStatus.COMPILATION_ERROR) {
                candCompileError = true;
                compileStderr = r.stderr();
                break;
            }
        }

        if (candCompileError) {
            List<CodingProblemTestcaseResultResponse> results = new ArrayList<>(count);
            long totalTime = 0L;
            for (int i = 0; i < count; i++) {
                CandidateTestcaseItem tc = request.testcases().get(i);
                SimpleCodeExecutionResult r = i < candBatchResult.results().size() ? candBatchResult.results().get(i) : null;
                long execTime = r != null && r.executionTimeMs() != null ? r.executionTimeMs() : 0L;
                totalTime += execTime;

                String expectedOut = tc.expectedOutput() != null
                        ? tc.expectedOutput()
                        : (i < referenceOutputs.size() ? referenceOutputs.get(i) : null);

                results.add(CodingProblemTestcaseResultResponse.builder()
                        .testCaseId(tc.id())
                        .caseIndex(tc.caseIndex())
                        .status("COMPILATION_ERROR")
                        .stdin(tc.stdin())
                        .dbInit(tc.dbInit())
                        .actualOutput(null)
                        .expectedOutput(expectedOut)
                        .stderr(compileStderr)
                        .exitCode(r != null ? r.exitCode() : null)
                        .executionTimeMs(execTime)
                        .runtimeMs(execTime)
                        .memoryMb(r != null && r.memoryUsageKb() != null ? Math.round(r.memoryUsageKb() / 1024.0 * 10.0) / 10.0 : null)
                        .error(compileStderr != null && !compileStderr.isBlank() ? compileStderr.strip() : "Compilation failed")
                        .isCustom(tc.isCustom())
                        .isSample(tc.isSample())
                        .logs(r != null ? r.logs() : null)
                        .build());
            }

            return CodingProblemExecutionResponse.builder()
                    .overallStatus("COMPILATION_ERROR")
                    .totalExecutionTimeMs(totalTime)
                    .totalTestcases(count)
                    .passedTestcases(0)
                    .failedTestcases(count)
                    .results(results)
                    .build();
        }

        // 3. For testcases where candidate succeeded (exitCode 0) and verification script is required, run verifier:
        Map<Integer, SimpleCodeExecutionResult> verifierResults = new HashMap<>();
        if (request.requireVerificationScript()) {
            List<String> verInputs = new ArrayList<>();
            List<String> verCands = new ArrayList<>();
            List<String> verExpecteds = new ArrayList<>();
            List<Integer> verIndices = new ArrayList<>();

            for (int i = 0; i < count; i++) {
                CandidateTestcaseItem tc = request.testcases().get(i);
                SimpleCodeExecutionResult candRes = i < candBatchResult.results().size() ? candBatchResult.results().get(i) : null;
                boolean candPassed = candRes != null
                        && candRes.executionStatus() == CodeExecutionStatus.SUCCESS
                        && (candRes.exitCode() == null || candRes.exitCode() == 0L);

                if (candPassed) {
                    String candStdout = candRes.stdout() != null ? candRes.stdout() : "";
                    String expectedOut = tc.expectedOutput() != null ? tc.expectedOutput() : "";
                    String stdinData = tc.stdin() != null ? tc.stdin() : "";

                    verInputs.add(stdinData);
                    verCands.add(candStdout);
                    verExpecteds.add(expectedOut);
                    verIndices.add(i);
                }
            }

            if (!verInputs.isEmpty()) {
                BatchCodeExecutionResult verBatchResult = runVerificationBatch(
                        request.verificationScriptLanguage(),
                        request.verificationScript(),
                        request.verificationScriptConstraints(),
                        verInputs,
                        verCands,
                        verExpecteds
                );

                if (verBatchResult != null && verBatchResult.results() != null) {
                    for (int v = 0; v < verIndices.size(); v++) {
                        int originalIndex = verIndices.get(v);
                        if (v < verBatchResult.results().size()) {
                            verifierResults.put(originalIndex, verBatchResult.results().get(v));
                        }
                    }
                }
            }
        }

        // 4. Build individual testcase results
        List<CodingProblemTestcaseResultResponse> results = new ArrayList<>(count);
        int passedCount = 0;
        long totalExecutionTimeMs = 0L;
        String firstFailureStatus = null;

        for (int i = 0; i < count; i++) {
            CandidateTestcaseItem tc = request.testcases().get(i);
            SimpleCodeExecutionResult candRes = i < candBatchResult.results().size() ? candBatchResult.results().get(i) : null;

            long execTime = candRes != null && candRes.executionTimeMs() != null ? candRes.executionTimeMs() : 0L;
            totalExecutionTimeMs += execTime;

            Double memoryMb = candRes != null && candRes.memoryUsageKb() != null
                    ? Math.round(candRes.memoryUsageKb() / 1024.0 * 10.0) / 10.0
                    : null;

            String expectedOut = request.requireVerificationScript()
                    ? (i < referenceOutputs.size() && referenceOutputs.get(i) != null ? referenceOutputs.get(i) : tc.expectedOutput())
                    : (tc.expectedOutput() != null ? tc.expectedOutput() : (i < referenceOutputs.size() ? referenceOutputs.get(i) : null));

            if (candRes == null) {
                results.add(CodingProblemTestcaseResultResponse.builder()
                        .testCaseId(tc.id())
                        .caseIndex(tc.caseIndex())
                        .status("RUNTIME_ERROR")
                        .stdin(tc.stdin())
                        .dbInit(tc.dbInit())
                        .actualOutput(null)
                        .expectedOutput(expectedOut)
                        .error("Execution produced null result")
                        .isCustom(tc.isCustom())
                        .isSample(tc.isSample())
                        .build());
                if (firstFailureStatus == null) firstFailureStatus = "RUNTIME_ERROR";
                continue;
            }

            boolean candSuccess = candRes.executionStatus() == CodeExecutionStatus.SUCCESS
                    && (candRes.exitCode() == null || candRes.exitCode() == 0L);

            if (!candSuccess) {
                String status = candRes.executionStatus() != null && candRes.executionStatus() != CodeExecutionStatus.SUCCESS
                        ? candRes.executionStatus().name()
                        : "RUNTIME_ERROR";
                String err = candRes.stderr() != null && !candRes.stderr().isBlank()
                        ? candRes.stderr().strip()
                        : (candRes.exitCode() != null && candRes.exitCode() != 0L ? "Process exited with code " + candRes.exitCode() : "Execution failed");

                results.add(CodingProblemTestcaseResultResponse.builder()
                        .testCaseId(tc.id())
                        .caseIndex(tc.caseIndex())
                        .status(status)
                        .stdin(tc.stdin())
                        .dbInit(tc.dbInit())
                        .actualOutput(candRes.stdout())
                        .expectedOutput(expectedOut)
                        .stderr(candRes.stderr())
                        .exitCode(candRes.exitCode())
                        .executionTimeMs(execTime)
                        .runtimeMs(execTime)
                        .memoryMb(memoryMb)
                        .error(err)
                        .isCustom(tc.isCustom())
                        .isSample(tc.isSample())
                        .logs(candRes.logs())
                        .build());

                if (firstFailureStatus == null) firstFailureStatus = status;
                continue;
            }

            // Candidate execution had exit code 0
            if (tc.isCustom()) {
                boolean match = expectedOut != null && compareOutputs(candRes.stdout(), expectedOut);
                String status = match ? "PASSED" : (expectedOut != null ? "WRONG_ANSWER" : "SUCCESS");
                passedCount++;

                results.add(CodingProblemTestcaseResultResponse.builder()
                        .testCaseId(tc.id())
                        .caseIndex(tc.caseIndex())
                        .status(status)
                        .passed(match || expectedOut == null)
                        .stdin(tc.stdin())
                        .dbInit(tc.dbInit())
                        .actualOutput(candRes.stdout())
                        .expectedOutput(expectedOut)
                        .stderr(candRes.stderr())
                        .exitCode(candRes.exitCode())
                        .executionTimeMs(execTime)
                        .runtimeMs(execTime)
                        .memoryMb(memoryMb)
                        .isCustom(true)
                        .isSample(false)
                        .logs(candRes.logs())
                        .build());
            } else if (!request.requireVerificationScript()) {
                // Direct comparison
                boolean match = compareOutputs(candRes.stdout(), expectedOut);
                String status = match ? "PASSED" : "WRONG_ANSWER";
                if (match) {
                    passedCount++;
                } else if (firstFailureStatus == null) {
                    firstFailureStatus = "WRONG_ANSWER";
                }

                results.add(CodingProblemTestcaseResultResponse.builder()
                        .testCaseId(tc.id())
                        .caseIndex(tc.caseIndex())
                        .status(status)
                        .passed(match)
                        .stdin(tc.stdin())
                        .dbInit(tc.dbInit())
                        .actualOutput(candRes.stdout())
                        .expectedOutput(expectedOut)
                        .stderr(candRes.stderr())
                        .exitCode(candRes.exitCode())
                        .executionTimeMs(execTime)
                        .runtimeMs(execTime)
                        .memoryMb(memoryMb)
                        .isCustom(tc.isCustom())
                        .isSample(tc.isSample())
                        .logs(candRes.logs())
                        .build());
            } else {
                // Verification script evaluation
                SimpleCodeExecutionResult verRes = verifierResults.get(i);
                boolean verPass = verRes != null
                        && verRes.executionStatus() == CodeExecutionStatus.SUCCESS
                        && (verRes.exitCode() == null || verRes.exitCode() == 0L);

                if (verPass) {
                    passedCount++;
                    results.add(CodingProblemTestcaseResultResponse.builder()
                            .testCaseId(tc.id())
                            .caseIndex(tc.caseIndex())
                            .status("PASSED")
                            .passed(true)
                            .stdin(tc.stdin())
                            .dbInit(tc.dbInit())
                            .actualOutput(candRes.stdout())
                            .expectedOutput(candRes.stdout()) // Verified and accepted
                            .stderr(candRes.stderr())
                            .exitCode(0L)
                            .executionTimeMs(execTime)
                            .runtimeMs(execTime)
                            .memoryMb(memoryMb)
                            .isCustom(tc.isCustom())
                            .isSample(tc.isSample())
                            .logs(candRes.logs())
                            .build());
                } else {
                    if (firstFailureStatus == null) firstFailureStatus = "WRONG_ANSWER";
                    String err = verRes != null && verRes.stderr() != null && !verRes.stderr().isBlank()
                            ? verRes.stderr().strip()
                            : "Verification script assertion failed";

                    // USER RULE: if verification script is on and candidate is wrong, give output of reference solution as expected output!
                    // Do NOT give verification script output!
                    results.add(CodingProblemTestcaseResultResponse.builder()
                            .testCaseId(tc.id())
                            .caseIndex(tc.caseIndex())
                            .status("WRONG_ANSWER")
                            .stdin(tc.stdin())
                            .dbInit(tc.dbInit())
                            .actualOutput(candRes.stdout())
                            .expectedOutput(expectedOut) // Reference solution output
                            .stderr(verRes != null ? verRes.stderr() : null)
                            .exitCode(verRes != null ? verRes.exitCode() : 1L)
                            .executionTimeMs(execTime)
                            .runtimeMs(execTime)
                            .memoryMb(memoryMb)
                            .error(err)
                            .isCustom(tc.isCustom())
                            .isSample(tc.isSample())
                            .logs(candRes.logs())
                            .build());
                }
            }
        }

        String overallStatus = passedCount == count
                ? "ACCEPTED"
                : (firstFailureStatus != null ? firstFailureStatus : "WRONG_ANSWER");

        return CodingProblemExecutionResponse.builder()
                .overallStatus(overallStatus)
                .totalExecutionTimeMs(totalExecutionTimeMs)
                .totalTestcases(count)
                .passedTestcases(passedCount)
                .failedTestcases(count - passedCount)
                .results(results)
                .build();
    }

    private CodingProblemExecutionResponse executeCandidateDatabase(JudgeCandidateExecutionRequest request) {
        int count = request.testcases().size();
        Language dbLanguage = request.candidateLanguage();

        String initFileName = languageFileNameRegistry.resolveDatabaseInitFileName(dbLanguage);
        String solutionFileName = languageFileNameRegistry.resolveDatabaseSolutionFileName(dbLanguage);
        String verifierFileName = request.requireVerificationScript()
                ? languageFileNameRegistry.resolveVerificationScriptFileName(request.verificationScriptLanguage())
                : null;

        List<CodingProblemTestcaseResultResponse> results = new ArrayList<>(count);
        int passedCount = 0;
        long totalExecutionTimeMs = 0L;
        String firstFailureStatus = null;

        for (int i = 0; i < count; i++) {
            CandidateTestcaseItem tc = request.testcases().get(i);
            boolean hasDbInit = tc.dbInit() != null && !tc.dbInit().isBlank();

            // 1. Run container with init + candidate solution + (optional verifier)
            Map<String, String> candFiles = new LinkedHashMap<>();
            List<BatchExecutionItem> candItems = new ArrayList<>();

            if (hasDbInit) {
                candFiles.put(initFileName, tc.dbInit());
                candItems.add(BatchExecutionItem.builder()
                        .executablePath(initFileName)
                        .constraints(request.candidateConstraints())
                        .build());
            }

            candFiles.put(solutionFileName, request.candidateCode());
            candItems.add(BatchExecutionItem.builder()
                    .executablePath(solutionFileName)
                    .constraints(request.candidateConstraints())
                    .build());

            if (request.requireVerificationScript()) {
                candFiles.put(verifierFileName, request.verificationScript());
                candItems.add(BatchExecutionItem.builder()
                        .executablePath(verifierFileName)
                        .constraints(request.verificationScriptConstraints())
                        .build());
            }

            BatchCodeExecutionRequest candBatchRequest = BatchCodeExecutionRequest.builder()
                    .language(dbLanguage)
                    .files(candFiles)
                    .constraints(request.candidateConstraints())
                    .executions(candItems)
                    .build();

            BatchCodeExecutionResult candBatchResult = codeExecutionService.run(candBatchRequest);

            // 2. Obtain reference solution output if needed (for expectedOutput)
            String expectedOut = tc.expectedOutput();
            if (expectedOut == null) {
                Map<String, String> refFiles = new LinkedHashMap<>();
                List<BatchExecutionItem> refItems = new ArrayList<>();

                if (hasDbInit) {
                    refFiles.put(initFileName, tc.dbInit());
                    refItems.add(BatchExecutionItem.builder()
                            .executablePath(initFileName)
                            .constraints(request.referenceConstraints())
                            .build());
                }

                refFiles.put(solutionFileName, request.referenceSolution());
                refItems.add(BatchExecutionItem.builder()
                        .executablePath(solutionFileName)
                        .constraints(request.referenceConstraints())
                        .build());

                if (request.requireVerificationScript()) {
                    refFiles.put(verifierFileName, request.verificationScript());
                    refItems.add(BatchExecutionItem.builder()
                            .executablePath(verifierFileName)
                            .constraints(request.verificationScriptConstraints())
                            .build());
                }

                BatchCodeExecutionRequest refBatchRequest = BatchCodeExecutionRequest.builder()
                        .language(request.referenceLanguage())
                        .files(refFiles)
                        .constraints(request.referenceConstraints())
                        .executions(refItems)
                        .build();

                BatchCodeExecutionResult refBatchResult = codeExecutionService.run(refBatchRequest);
                if (refBatchResult != null && refBatchResult.results() != null && !refBatchResult.results().isEmpty()) {
                    SimpleCodeExecutionResult refSolResult = refBatchResult.results().get(refBatchResult.results().size() - 1);
                    if (refSolResult != null && refSolResult.stdout() != null) {
                        expectedOut = refSolResult.stdout();
                    }
                }
            }

            if (candBatchResult == null || candBatchResult.results() == null || candBatchResult.results().size() != candItems.size()) {
                results.add(CodingProblemTestcaseResultResponse.builder()
                        .testCaseId(tc.id())
                        .caseIndex(tc.caseIndex())
                        .status("RUNTIME_ERROR")
                        .stdin(tc.stdin())
                        .dbInit(tc.dbInit())
                        .actualOutput(null)
                        .expectedOutput(expectedOut)
                        .error("Database execution returned unexpected result")
                        .isCustom(tc.isCustom())
                        .isSample(tc.isSample())
                        .build());
                if (firstFailureStatus == null) firstFailureStatus = "RUNTIME_ERROR";
                continue;
            }

            int resultIndex = 0;
            SimpleCodeExecutionResult initResult = null;
            if (hasDbInit) {
                initResult = candBatchResult.results().get(resultIndex++);
            }
            SimpleCodeExecutionResult solutionResult = candBatchResult.results().get(resultIndex++);
            SimpleCodeExecutionResult verResult = request.requireVerificationScript()
                    ? candBatchResult.results().get(resultIndex++)
                    : null;

            long caseTime = solutionResult != null && solutionResult.executionTimeMs() != null
                    ? solutionResult.executionTimeMs()
                    : 0L;
            totalExecutionTimeMs += caseTime;

            Double memoryMb = solutionResult != null && solutionResult.memoryUsageKb() != null
                    ? Math.round(solutionResult.memoryUsageKb() / 1024.0 * 10.0) / 10.0
                    : null;

            // Check init failure
            if (initResult != null && (initResult.executionStatus() != CodeExecutionStatus.SUCCESS || (initResult.exitCode() != null && initResult.exitCode() != 0L))) {
                results.add(CodingProblemTestcaseResultResponse.builder()
                        .testCaseId(tc.id())
                        .caseIndex(tc.caseIndex())
                        .status("RUNTIME_ERROR")
                        .stdin(tc.stdin())
                        .dbInit(tc.dbInit())
                        .actualOutput(null)
                        .expectedOutput(expectedOut)
                        .stderr("Database initialization failed: " + (initResult.stderr() != null ? initResult.stderr().strip() : ""))
                        .error("Database initialization failed")
                        .exitCode(initResult.exitCode())
                        .executionTimeMs(caseTime)
                        .runtimeMs(caseTime)
                        .memoryMb(memoryMb)
                        .isCustom(tc.isCustom())
                        .isSample(tc.isSample())
                        .logs(initResult.logs())
                        .build());
                if (firstFailureStatus == null) firstFailureStatus = "RUNTIME_ERROR";
                continue;
            }

            // Check solution failure
            if (solutionResult.executionStatus() != CodeExecutionStatus.SUCCESS || (solutionResult.exitCode() != null && solutionResult.exitCode() != 0L)) {
                String status = solutionResult.executionStatus() != null && solutionResult.executionStatus() != CodeExecutionStatus.SUCCESS
                        ? solutionResult.executionStatus().name()
                        : "RUNTIME_ERROR";
                results.add(CodingProblemTestcaseResultResponse.builder()
                        .testCaseId(tc.id())
                        .caseIndex(tc.caseIndex())
                        .status(status)
                        .stdin(tc.stdin())
                        .dbInit(tc.dbInit())
                        .actualOutput(solutionResult.stdout())
                        .expectedOutput(expectedOut)
                        .stderr(solutionResult.stderr())
                        .error("SQL query execution failed: " + (solutionResult.stderr() != null ? solutionResult.stderr().strip() : "Exit code " + solutionResult.exitCode()))
                        .exitCode(solutionResult.exitCode())
                        .executionTimeMs(caseTime)
                        .runtimeMs(caseTime)
                        .memoryMb(memoryMb)
                        .isCustom(tc.isCustom())
                        .isSample(tc.isSample())
                        .logs(solutionResult.logs())
                        .build());
                if (firstFailureStatus == null) firstFailureStatus = status;
                continue;
            }

            // Solution executed with exit code 0
            if (!request.requireVerificationScript()) {
                boolean match = compareOutputs(solutionResult.stdout(), expectedOut);
                String status = match ? "PASSED" : "WRONG_ANSWER";
                if (match) {
                    passedCount++;
                } else if (firstFailureStatus == null) {
                    firstFailureStatus = "WRONG_ANSWER";
                }

                results.add(CodingProblemTestcaseResultResponse.builder()
                        .testCaseId(tc.id())
                        .caseIndex(tc.caseIndex())
                        .status(status)
                        .stdin(tc.stdin())
                        .dbInit(tc.dbInit())
                        .actualOutput(solutionResult.stdout())
                        .expectedOutput(expectedOut)
                        .stderr(solutionResult.stderr())
                        .exitCode(0L)
                        .executionTimeMs(caseTime)
                        .runtimeMs(caseTime)
                        .memoryMb(memoryMb)
                        .isCustom(tc.isCustom())
                        .isSample(tc.isSample())
                        .logs(solutionResult.logs())
                        .build());
            } else {
                boolean verExecOk = verResult != null
                        && verResult.executionStatus() == CodeExecutionStatus.SUCCESS
                        && (verResult.exitCode() == null || verResult.exitCode() == 0L);

                boolean outputMatches = true;
                if (verResult != null && verResult.stdout() != null && !verResult.stdout().isBlank()) {
                    outputMatches = compareOutputs(verResult.stdout(), expectedOut);
                }

                boolean verPass = verExecOk && outputMatches;

                String displayActual = (verResult != null && verResult.stdout() != null && !verResult.stdout().isBlank())
                        ? verResult.stdout()
                        : solutionResult.stdout();

                if (verPass) {
                    passedCount++;
                    results.add(CodingProblemTestcaseResultResponse.builder()
                            .testCaseId(tc.id())
                            .caseIndex(tc.caseIndex())
                            .status("PASSED")
                            .passed(true)
                            .stdin(tc.stdin())
                            .dbInit(tc.dbInit())
                            .actualOutput(displayActual)
                            .expectedOutput(expectedOut)
                            .stderr(solutionResult.stderr())
                            .exitCode(0L)
                            .executionTimeMs(caseTime)
                            .runtimeMs(caseTime)
                            .memoryMb(memoryMb)
                            .isCustom(tc.isCustom())
                            .isSample(tc.isSample())
                            .logs(solutionResult.logs())
                            .build());
                } else {
                    if (firstFailureStatus == null) firstFailureStatus = "WRONG_ANSWER";
                    String err = verResult != null && verResult.stderr() != null && !verResult.stderr().isBlank()
                            ? verResult.stderr().strip()
                            : (outputMatches ? "Verification script assertion failed" : "Database state does not match expected output");

                    // USER RULE: if verification script is on and candidate is wrong, give output of reference solution as expected output!
                    results.add(CodingProblemTestcaseResultResponse.builder()
                            .testCaseId(tc.id())
                            .caseIndex(tc.caseIndex())
                            .status("WRONG_ANSWER")
                            .stdin(tc.stdin())
                            .dbInit(tc.dbInit())
                            .actualOutput(displayActual)
                            .expectedOutput(expectedOut) // Reference solution output
                            .stderr(verResult != null ? verResult.stderr() : null)
                            .exitCode(verResult != null && verResult.exitCode() != null && verResult.exitCode() != 0L ? verResult.exitCode() : 1L)
                            .executionTimeMs(caseTime)
                            .runtimeMs(caseTime)
                            .memoryMb(memoryMb)
                            .error(err)
                            .isCustom(tc.isCustom())
                            .isSample(tc.isSample())
                            .logs(solutionResult.logs())
                            .build());
                }
            }
        }

        String overallStatus = passedCount == count
                ? "ACCEPTED"
                : (firstFailureStatus != null ? firstFailureStatus : "WRONG_ANSWER");

        return CodingProblemExecutionResponse.builder()
                .overallStatus(overallStatus)
                .totalExecutionTimeMs(totalExecutionTimeMs)
                .totalTestcases(count)
                .passedTestcases(passedCount)
                .failedTestcases(count - passedCount)
                .results(results)
                .build();
    }

    private void validateCandidateExecutionRequest(JudgeCandidateExecutionRequest request) {
        if (request == null) {
            throw new BadRequestException("Candidate execution request cannot be null.");
        }
        if (request.problemType() == null) {
            throw new BadRequestException("Problem type is required.");
        }
        if (request.candidateLanguage() == null) {
            throw new BadRequestException("Candidate language is required.");
        }
        if (request.candidateCode() == null || request.candidateCode().isBlank()) {
            throw new BadRequestException("Candidate code cannot be empty.");
        }
        if (request.candidateConstraints() == null) {
            throw new BadRequestException("Candidate execution constraints are required.");
        }
        if (request.testcases() == null || request.testcases().isEmpty()) {
            throw new BadRequestException("At least one testcase is required.");
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
        }
    }

    private PreviewTestcaseResultResponse mapSimpleResultToResponse(
            String testCaseId,
            int caseIndex,
            SimpleCodeExecutionResult r,
            String expectedOutput
    ) {
        if (r == null) {
            return PreviewTestcaseResultResponse.builder()
                    .testCaseId(testCaseId)
                    .caseIndex(caseIndex)
                    .status("FAILED")
                    .stderr("Execution produced null result")
                    .error("Execution produced null result")
                    .build();
        }

        String status;
        String error = null;
        if (r.executionStatus() == CodeExecutionStatus.SUCCESS) {
            if (r.exitCode() != null && r.exitCode() != 0L) {
                status = "RUNTIME_ERROR";
                error = "Process exited with code " + r.exitCode();
            } else {
                status = "SUCCESS";
            }
        } else {
            status = r.executionStatus() != null ? r.executionStatus().name() : "FAILED";
            error = r.stderr() != null && !r.stderr().isBlank() ? r.stderr().strip() : "Execution failed";
        }

        Double memoryMb = r.memoryUsageKb() != null
                ? Math.round(r.memoryUsageKb() / 1024.0 * 10.0) / 10.0
                : null;

        return PreviewTestcaseResultResponse.builder()
                .testCaseId(testCaseId)
                .caseIndex(caseIndex)
                .status(status)
                .stdout(r.stdout())
                .stderr(r.stderr())
                .exitCode(r.exitCode())
                .runtimeMs(r.executionTimeMs())
                .memoryMb(memoryMb)
                .expectedOutput(expectedOutput)
                .logs(r.logs())
                .error(error)
                .build();
    }

    private String buildLogsText(
            String languageName,
            int total,
            int passed,
            long totalTimeMs,
            String status,
            boolean verifierEnabled,
            String verifierLangName
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("[Execution Summary]\n");
        sb.append("Language: ").append(languageName).append("\n");
        sb.append("Status: ").append(status).append("\n");
        sb.append("Testcases: ").append(passed).append("/").append(total).append(" passed\n");
        sb.append("Total Execution Time: ").append(totalTimeMs).append(" ms\n");
        sb.append("Verification Script: ").append(verifierEnabled ? "ENABLED (" + verifierLangName + ")" : "DISABLED (Direct comparison)").append("\n");
        return sb.toString();
    }

    private PreviewRunResponse buildErrorResponse(String message, int count) {
        List<PreviewTestcaseResultResponse> results = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            results.add(PreviewTestcaseResultResponse.builder()
                    .testCaseId("case-" + (i + 1))
                    .caseIndex(i + 1)
                    .status("FAILED")
                    .stderr(message)
                    .error(message)
                    .build());
        }
        return PreviewRunResponse.builder()
                .overallStatus("FAILED")
                .totalExecutionTimeMs(0L)
                .totalTestcases(count)
                .passedTestcases(0)
                .failedTestcases(count)
                .logs("[Error] " + message)
                .results(results)
                .build();
    }

    private void validatePreviewRequest(JudgePreviewRequest request) {
        if (request == null) {
            throw new BadRequestException("Judge preview request cannot be null.");
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
        if (request.testcases() == null || request.testcases().isEmpty()) {
            throw new BadRequestException("At least one testcase is required for preview execution.");
        }
    }

    @Override
    public TestcaseGeneratorRunResponse executeGenerator(
            Language language,
            String script,
            String stdin,
            CodeExecutionConstraints constraints
    ) {
        if (language == null) {
            throw new BadRequestException("Generator language is required.");
        }
        if (script == null || script.isBlank()) {
            throw new BadRequestException("Generator script cannot be empty.");
        }
        if (language.type() == LanguageType.DATABASE) {
            throw new BadRequestException("Database languages cannot be used for testcase generation.");
        }

        String fileName = languageFileNameRegistry.resolveDefaultFileName(language);
        if (fileName == null || fileName.isBlank()) {
            fileName = languageFileNameRegistry.resolveReferenceSolutionFileName(language);
        }

        SimpleCodeExecutionRequest execReq = SimpleCodeExecutionRequest.builder()
                .language(language)
                .code(script)
                .fileName(fileName)
                .stdin(stdin != null ? stdin : "")
                .constraints(constraints)
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(execReq);

        String error = null;
        if (result.executionStatus() != CodeExecutionStatus.SUCCESS) {
            if (result.stderr() != null && !result.stderr().isBlank()) {
                error = result.stderr();
            } else {
                error = "Execution failed with status: " + result.executionStatus();
            }
        }

        return TestcaseGeneratorRunResponse.builder()
                .status(result.executionStatus())
                .stdout(result.stdout())
                .stderr(result.stderr())
                .exitCode(result.exitCode())
                .exitSignal(result.exitSignal())
                .executionTimeMs(result.executionTimeMs())
                .memoryUsageKb(result.memoryUsageKb())
                .error(error)
                .build();
    }
}
