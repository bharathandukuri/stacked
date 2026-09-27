package com.bharath.stacked.modules.judge;

import com.bharath.stacked.modules.judge.config.JudgeProperties;
import com.bharath.stacked.modules.judge.dto.SampleRunRequest;
import com.bharath.stacked.modules.judge.dto.SampleRunResponse;
import com.bharath.stacked.modules.judge.dto.TestCaseDto;
import com.bharath.stacked.modules.judge.enums.ExecutionStatus;
import com.bharath.stacked.modules.judge.enums.SandboxDriverType;
import com.bharath.stacked.modules.judge.languages.LanguageRegistry;
import com.bharath.stacked.modules.judge.sandbox.SandboxDriver;
import com.bharath.stacked.modules.judge.sandbox.SandboxDriverRegistry;
import com.bharath.stacked.modules.judge.sandbox.model.SandboxExecutionRequest;
import com.bharath.stacked.modules.judge.sandbox.model.SandboxExecutionResult;
import com.bharath.stacked.modules.judge.service.JudgeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class JudgeServiceTest {

    private JudgeProperties properties;
    private LanguageRegistry languageRegistry;
    private SandboxDriver mockDriver;
    private SandboxDriverRegistry driverRegistry;
    private JudgeService judgeService;

    @BeforeEach
    void setUp() {
        properties = new JudgeProperties(
                SandboxDriverType.DOCKER, 5000, 10000, 65536, "/tmp/test", null, null, null);
        languageRegistry = new LanguageRegistry(properties);
        mockDriver = Mockito.mock(SandboxDriver.class);
        when(mockDriver.getType()).thenReturn(SandboxDriverType.DOCKER);

        driverRegistry = Mockito.mock(SandboxDriverRegistry.class);
        when(driverRegistry.getActiveDriver()).thenReturn(mockDriver);

        judgeService = new JudgeService(languageRegistry, driverRegistry, properties);
    }

    @Test
    @DisplayName("Should evaluate correct solution against expected output without validator")
    void shouldEvaluateCorrectSolutionDirectComparison() {
        when(mockDriver.execute(any(SandboxExecutionRequest.class)))
                .thenReturn(SandboxExecutionResult.success(0, "42\n", "", 50, 1024));

        SampleRunRequest request = new SampleRunRequest(
                "python-3.12",
                "print(42)",
                null,
                List.of(new TestCaseDto("tc-1", "input", "42", true)));

        SampleRunResponse response = judgeService.executeSampleRun(request);

        assertThat(response.status()).isEqualTo(ExecutionStatus.SUCCESS);
        assertThat(response.totalTestCases()).isEqualTo(1);
        assertThat(response.passedTestCases()).isEqualTo(1);
        assertThat(response.results().getFirst().actualOutput().trim()).isEqualTo("42");
    }

    @Test
    @DisplayName("Should detect wrong answer when output mismatches expected output")
    void shouldDetectWrongAnswer() {
        when(mockDriver.execute(any(SandboxExecutionRequest.class)))
                .thenReturn(SandboxExecutionResult.success(0, "99\n", "", 50, 1024));

        SampleRunRequest request = new SampleRunRequest(
                "python-3.12",
                "print(99)",
                null,
                List.of(new TestCaseDto("tc-1", "input", "42", true)));

        SampleRunResponse response = judgeService.executeSampleRun(request);

        assertThat(response.status()).isEqualTo(ExecutionStatus.WRONG_ANSWER);
        assertThat(response.passedTestCases()).isEqualTo(0);
        assertThat(response.results().getFirst().status()).isEqualTo(ExecutionStatus.WRONG_ANSWER);
        assertThat(response.results().getFirst().errorMessage()).contains("Expected: [42], Got: [99]");
    }

    @Test
    @DisplayName("Should execute dual-stream validator protocol when validator is provided")
    void shouldExecuteDualStreamValidator() {
        // Solution execution returns "84"
        // Validator execution returns exit code 0 (valid)
        when(mockDriver.execute(any(SandboxExecutionRequest.class)))
                .thenReturn(SandboxExecutionResult.success(0, "84\n", "", 40, 1024)) // Solution run
                .thenReturn(SandboxExecutionResult.success(0, "VALID\n", "", 30, 1024)); // Validator run

        SampleRunRequest request = new SampleRunRequest(
                "python-3.12",
                "print(84)",
                "import sys\nsys.exit(0)",
                List.of(new TestCaseDto("tc-1", "42", null, true)));

        SampleRunResponse response = judgeService.executeSampleRun(request);

        assertThat(response.status()).isEqualTo(ExecutionStatus.SUCCESS);
        assertThat(response.passedTestCases()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should detect compilation error and abort subsequent runs")
    void shouldHandleCompilationError() {
        when(mockDriver.execute(any(SandboxExecutionRequest.class)))
                .thenReturn(SandboxExecutionResult.compileError(1, "", "Syntax error: ';' expected", 100));

        SampleRunRequest request = new SampleRunRequest(
                "java-21",
                "class Solution {}",
                null,
                List.of(
                        new TestCaseDto("tc-1", "in1", "out1", true),
                        new TestCaseDto("tc-2", "in2", "out2", false)));

        SampleRunResponse response = judgeService.executeSampleRun(request);

        assertThat(response.status()).isEqualTo(ExecutionStatus.COMPILATION_ERROR);
        assertThat(response.compilationError()).contains("Syntax error");
        // Aborted on first test case due to compile error
        assertThat(response.results()).hasSize(1);
    }
}
