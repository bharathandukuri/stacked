package com.bharath.stacked.modules.judge.service;

import com.bharath.stacked.modules.judge.config.JudgeProperties;
import com.bharath.stacked.modules.judge.dto.SampleRunRequest;
import com.bharath.stacked.modules.judge.dto.SampleRunResponse;
import com.bharath.stacked.modules.judge.dto.TestCaseDto;
import com.bharath.stacked.modules.judge.dto.TestcaseExecutionResult;
import com.bharath.stacked.modules.judge.enums.ExecutionStatus;
import com.bharath.stacked.modules.judge.languages.CompiledLanguageDetails;
import com.bharath.stacked.modules.judge.languages.DatabaseLanguageDetails;
import com.bharath.stacked.modules.judge.languages.InterpretedLanguageDetails;
import com.bharath.stacked.modules.judge.languages.LanguageDetails;
import com.bharath.stacked.modules.judge.languages.LanguageRegistry;
import com.bharath.stacked.modules.judge.sandbox.SandboxDriver;
import com.bharath.stacked.modules.judge.sandbox.SandboxDriverRegistry;
import com.bharath.stacked.modules.judge.sandbox.model.SandboxExecutionRequest;
import com.bharath.stacked.modules.judge.sandbox.model.SandboxExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class JudgeService {

    private static final Logger log = LoggerFactory.getLogger(JudgeService.class);
    private static final String DELIMITER = "\n---OUTPUT---\n";

    private final LanguageRegistry languageRegistry;
    private final SandboxDriverRegistry sandboxDriverRegistry;
    private final JudgeProperties judgeProperties;

    public JudgeService(
            LanguageRegistry languageRegistry,
            SandboxDriverRegistry sandboxDriverRegistry,
            JudgeProperties judgeProperties) {
        this.languageRegistry = languageRegistry;
        this.sandboxDriverRegistry = sandboxDriverRegistry;
        this.judgeProperties = judgeProperties;
    }

    public SampleRunResponse executeSampleRun(SampleRunRequest request) {
        LanguageDetails language = languageRegistry.getLanguageOrThrow(request.languageId());
        SandboxDriver driver = sandboxDriverRegistry.getActiveDriver();

        log.info("Executing sample run for language: {} using driver: {}", language.getId(), driver.getType());

        String sourceFileName;
        String validatorFileName;
        String compileCommand = null;
        String runCommand;

        if (language instanceof CompiledLanguageDetails compiled) {
            sourceFileName = compiled.getSolutionFile();
            validatorFileName = compiled.getValidationFile();
            compileCommand = compiled.getCompileCommand(sourceFileName);
            runCommand = compiled.getRunCommand(sourceFileName);
        } else if (language instanceof InterpretedLanguageDetails interpreted) {
            sourceFileName = interpreted.getSourceFile();
            validatorFileName = interpreted.getValidationFile();
            runCommand = interpreted.getRunCommand(sourceFileName);
        } else if (language instanceof DatabaseLanguageDetails database) {
            sourceFileName = database.getSolutionFile();
            validatorFileName = database.getValidationFile();
            runCommand = database.getRunCommand(sourceFileName);
        } else {
            throw new IllegalStateException("Unknown language type hierarchy for: " + language.getId());
        }

        List<TestcaseExecutionResult> testcaseResults = new ArrayList<>();
        int passedCount = 0;
        long totalExecutionTime = 0;
        String compilationError = null;
        ExecutionStatus overallStatus = ExecutionStatus.SUCCESS;

        for (TestCaseDto testCase : request.testCases()) {
            String executionId = UUID.randomUUID().toString();

            Map<String, String> files = new HashMap<>();
            files.put(sourceFileName, request.solutionCode());

            long timeout = language.getDefaultTimeoutMs() > 0
                    ? language.getDefaultTimeoutMs()
                    : judgeProperties.defaultTimeoutMs();
            int memory = language.getDefaultMemoryLimitMb() > 0
                    ? language.getDefaultMemoryLimitMb()
                    : judgeProperties.docker().memoryLimitMb();

            // Execute solution code
            SandboxExecutionRequest execRequest = new SandboxExecutionRequest(
                    executionId,
                    language.getId(),
                    language.getDockerImage(),
                    language.getFirecrackerRootfs(),
                    files,
                    compileCommand,
                    runCommand,
                    testCase.input(),
                    timeout,
                    memory,
                    true);

            SandboxExecutionResult execResult = driver.execute(execRequest);
            totalExecutionTime += execResult.executionTimeMs();

            // If compilation error occurred, abort early and report compilation failure
            if (execResult.status() == ExecutionStatus.COMPILATION_ERROR) {
                compilationError = execResult.stderr().isBlank() ? execResult.stdout() : execResult.stderr();
                testcaseResults.add(new TestcaseExecutionResult(
                        testCase.id(),
                        ExecutionStatus.COMPILATION_ERROR,
                        testCase.input(),
                        testCase.expectedOutput(),
                        "",
                        execResult.executionTimeMs(),
                        execResult.memoryUsedKb(),
                        compilationError));
                overallStatus = ExecutionStatus.COMPILATION_ERROR;
                break;
            }

            // If runtime error or timeout
            if (execResult.status() != ExecutionStatus.SUCCESS) {
                testcaseResults.add(new TestcaseExecutionResult(
                        testCase.id(),
                        execResult.status(),
                        testCase.input(),
                        testCase.expectedOutput(),
                        execResult.stdout(),
                        execResult.executionTimeMs(),
                        execResult.memoryUsedKb(),
                        execResult.stderr().isBlank() ? execResult.errorMessage() : execResult.stderr()));
                if (overallStatus == ExecutionStatus.SUCCESS) {
                    overallStatus = execResult.status();
                }
                continue;
            }

            // Solution executed successfully (exit code 0). Now validate output
            // correctness.
            String actualOutput = execResult.stdout();
            boolean passed;
            String errorMsg = null;

            if (request.validatorCode() != null && !request.validatorCode().isBlank()) {
                // Dual-stream validator execution: input + DELIMITER + actualOutput
                String validatorExecutionId = UUID.randomUUID().toString();
                Map<String, String> validatorFiles = new HashMap<>();
                validatorFiles.put(validatorFileName, request.validatorCode());

                String valCompileCmd = null;
                String valRunCmd;

                if (language instanceof CompiledLanguageDetails compiled) {
                    valCompileCmd = compiled.getCompileCommand(validatorFileName);
                    valRunCmd = compiled.getRunCommand(validatorFileName);
                } else if (language instanceof InterpretedLanguageDetails interpreted) {
                    valRunCmd = interpreted.getRunCommand(validatorFileName);
                } else if (language instanceof DatabaseLanguageDetails database) {
                    valRunCmd = database.getRunCommand(validatorFileName);
                } else {
                    valRunCmd = runCommand;
                }

                String validatorInput = testCase.input() + DELIMITER + actualOutput;

                SandboxExecutionRequest valRequest = new SandboxExecutionRequest(
                        validatorExecutionId,
                        language.getId(),
                        language.getDockerImage(),
                        language.getFirecrackerRootfs(),
                        validatorFiles,
                        valCompileCmd,
                        valRunCmd,
                        validatorInput,
                        timeout,
                        memory,
                        true);

                SandboxExecutionResult valResult = driver.execute(valRequest);
                if (valResult.exitCode() == 0 && valResult.status() == ExecutionStatus.SUCCESS) {
                    passed = true;
                } else {
                    passed = false;
                    errorMsg = valResult.stderr().isBlank()
                            ? "Validator failed with exit code: " + valResult.exitCode()
                            : valResult.stderr();
                }
            } else {
                // Direct expected output comparison with whitespace normalization
                String expected = testCase.expectedOutput() != null ? testCase.expectedOutput().trim() : "";
                passed = actualOutput.trim().equals(expected);
                if (!passed) {
                    errorMsg = "Output mismatch. Expected: [" + expected + "], Got: [" + actualOutput.trim() + "]";
                }
            }

            ExecutionStatus tcStatus = passed ? ExecutionStatus.SUCCESS : ExecutionStatus.WRONG_ANSWER;
            if (passed) {
                passedCount++;
            } else if (overallStatus == ExecutionStatus.SUCCESS) {
                overallStatus = ExecutionStatus.WRONG_ANSWER;
            }

            testcaseResults.add(new TestcaseExecutionResult(
                    testCase.id(),
                    tcStatus,
                    testCase.input(),
                    testCase.expectedOutput(),
                    actualOutput,
                    execResult.executionTimeMs(),
                    execResult.memoryUsedKb(),
                    errorMsg));
        }

        return new SampleRunResponse(
                language.getId(),
                overallStatus,
                request.testCases().size(),
                passedCount,
                totalExecutionTime,
                compilationError,
                testcaseResults);
    }
}
