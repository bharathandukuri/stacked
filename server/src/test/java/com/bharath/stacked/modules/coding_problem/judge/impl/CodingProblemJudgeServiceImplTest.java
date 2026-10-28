package com.bharath.stacked.modules.coding_problem.judge.impl;

import com.bharath.stacked.exception.BadRequestException;
import com.bharath.stacked.modules.coding_problem.dto.request.PreviewTestcaseRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.TestcaseRequest;
import com.bharath.stacked.modules.coding_problem.dto.response.CodingProblemExecutionResponse;
import com.bharath.stacked.modules.coding_problem.dto.response.PreviewRunResponse;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemType;
import com.bharath.stacked.modules.coding_problem.judge.CodingProblemJudgeService;
import com.bharath.stacked.modules.coding_problem.judge.CodingProblemJudgeService.CandidateTestcaseItem;
import com.bharath.stacked.modules.coding_problem.judge.CodingProblemJudgeService.InitializedTestcases;
import com.bharath.stacked.modules.coding_problem.judge.CodingProblemJudgeService.JudgeCandidateExecutionRequest;
import com.bharath.stacked.modules.coding_problem.judge.CodingProblemJudgeService.JudgeInitializationRequest;
import com.bharath.stacked.modules.coding_problem.judge.CodingProblemJudgeService.JudgePreviewRequest;
import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import com.bharath.stacked.modules.execution.dto.request.BatchCodeExecutionRequest;
import com.bharath.stacked.modules.execution.dto.request.SimpleCodeExecutionRequest;
import com.bharath.stacked.modules.execution.dto.response.BatchCodeExecutionResult;
import com.bharath.stacked.modules.execution.dto.response.SimpleCodeExecutionResult;
import com.bharath.stacked.modules.execution.enums.CodeExecutionStatus;
import com.bharath.stacked.modules.execution.service.CodeExecutionService;
import com.bharath.stacked.modules.language.Language;
import com.bharath.stacked.modules.language.LanguageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CodingProblemJudgeServiceImplTest {

    @Mock
    private CodeExecutionService codeExecutionService;

    @Mock
    private Language javaLanguage;

    @Mock
    private Language pythonLanguage;

    @Mock
    private Language sqliteLanguage;

    @Mock
    private Language postgresLanguage;

    @Mock
    private com.bharath.stacked.modules.language.registry.LanguageFileNameRegistry languageFileNameRegistry;

    @InjectMocks
    private CodingProblemJudgeServiceImpl judgeService;

    @BeforeEach
    void setUp() {
        lenient().when(javaLanguage.id()).thenReturn("java-21");
        lenient().when(javaLanguage.type()).thenReturn(LanguageType.COMPILED);
        lenient().when(javaLanguage.defaultFileName()).thenReturn("Solution.java");

        lenient().when(pythonLanguage.id()).thenReturn("python-3.12");
        lenient().when(pythonLanguage.type()).thenReturn(LanguageType.INTERPRETED);
        lenient().when(pythonLanguage.defaultFileName()).thenReturn("solution.py");

        lenient().when(sqliteLanguage.id()).thenReturn("sqlite-3");
        lenient().when(sqliteLanguage.type()).thenReturn(LanguageType.DATABASE);
        lenient().when(sqliteLanguage.defaultFileName()).thenReturn("solution.sql");

        lenient().when(postgresLanguage.id()).thenReturn("postgresql-16");
        lenient().when(postgresLanguage.type()).thenReturn(LanguageType.DATABASE);
        lenient().when(postgresLanguage.defaultFileName()).thenReturn("solution.sql");

        lenient().when(languageFileNameRegistry.resolveReferenceSolutionFileName(javaLanguage)).thenReturn("Solution.java");
        lenient().when(languageFileNameRegistry.resolveReferenceSolutionFileName(pythonLanguage)).thenReturn("solution.py");
        lenient().when(languageFileNameRegistry.resolveReferenceSolutionFileName(sqliteLanguage)).thenReturn("solution.sql");

        lenient().when(languageFileNameRegistry.resolveVerificationScriptFileName(javaLanguage)).thenReturn("Verifier.java");
        lenient().when(languageFileNameRegistry.resolveVerificationScriptFileName(pythonLanguage)).thenReturn("verifier.py");
        lenient().when(languageFileNameRegistry.resolveVerificationScriptFileName(sqliteLanguage)).thenReturn("verifier.sql");

        lenient().when(languageFileNameRegistry.resolveDatabaseInitFileName(sqliteLanguage)).thenReturn("init.sql");
        lenient().when(languageFileNameRegistry.resolveDatabaseSolutionFileName(sqliteLanguage)).thenReturn("solution.sql");
    }

    @Test
    @DisplayName("Case A: Generic problem uses batch execution and sets expected outputs directly from reference solution")
    void testGenericProblemBatchExecutionSuccessWithoutVerification() {
        List<TestcaseRequest> sampleTc = List.of(new TestcaseRequest("1 2", null));
        List<TestcaseRequest> hiddenTc = List.of(new TestcaseRequest("3 4", null), new TestcaseRequest("5 6", null));

        JudgeInitializationRequest request = new JudgeInitializationRequest(
                CodingProblemType.GENERIC,
                javaLanguage,
                "public class Solution { public static void main(String[] args) {} }",
                new CodeExecutionConstraints(2000L, 262144L),
                false,
                null,
                null,
                null,
                sampleTc,
                hiddenTc
        );

        SimpleCodeExecutionResult r1 = SimpleCodeExecutionResult.builder()
                .stdout("3\n")
                .executionStatus(CodeExecutionStatus.SUCCESS)
                .exitCode(0L)
                .build();
        SimpleCodeExecutionResult r2 = SimpleCodeExecutionResult.builder()
                .stdout("7\n")
                .executionStatus(CodeExecutionStatus.SUCCESS)
                .exitCode(0L)
                .build();
        SimpleCodeExecutionResult r3 = SimpleCodeExecutionResult.builder()
                .stdout("11\n")
                .executionStatus(CodeExecutionStatus.SUCCESS)
                .exitCode(0L)
                .build();

        BatchCodeExecutionResult batchResult = BatchCodeExecutionResult.builder()
                .results(List.of(r1, r2, r3))
                .totalExecutions(3)
                .successfulExecutions(3)
                .failedExecutions(0)
                .build();

        when(codeExecutionService.run(any(BatchCodeExecutionRequest.class))).thenReturn(batchResult);

        InitializedTestcases result = judgeService.initializeExpectedOutputs(request);

        assertThat(result).isNotNull();
        assertThat(result.sampleTestCases()).hasSize(1);
        assertThat(result.sampleTestCases().get(0).getStdin()).isEqualTo("1 2");
        assertThat(result.sampleTestCases().get(0).getOutput()).isEqualTo("3\n");
        assertThat(result.sampleTestCases().get(0).getId()).isNotBlank();

        assertThat(result.hiddenTestCases()).hasSize(2);
        assertThat(result.hiddenTestCases().get(0).getStdin()).isEqualTo("3 4");
        assertThat(result.hiddenTestCases().get(0).getOutput()).isEqualTo("7\n");
        assertThat(result.hiddenTestCases().get(1).getStdin()).isEqualTo("5 6");
        assertThat(result.hiddenTestCases().get(1).getOutput()).isEqualTo("11\n");

        ArgumentCaptor<BatchCodeExecutionRequest> captor = ArgumentCaptor.forClass(BatchCodeExecutionRequest.class);
        verify(codeExecutionService).run(captor.capture());
        BatchCodeExecutionRequest captured = captor.getValue();
        assertThat(captured.getLanguage()).isEqualTo(javaLanguage);
        assertThat(captured.getExecutions()).hasSize(3);
        assertThat(captured.getExecutions().get(0).getStdin()).isEqualTo("1 2");
        assertThat(captured.getExecutions().get(1).getStdin()).isEqualTo("3 4");
        assertThat(captured.getExecutions().get(2).getStdin()).isEqualTo("5 6");
    }

    @Test
    @DisplayName("Case A: Database problem runs testcases sequentially with separate init.sql and solution.sql files")
    void testDatabaseProblemSequentialExecutionSuccessWithoutVerification() {
        List<TestcaseRequest> sampleTc = List.of(new TestcaseRequest("", "CREATE TABLE t (v INT); INSERT INTO t VALUES (1);"));
        List<TestcaseRequest> hiddenTc = List.of(new TestcaseRequest("", "CREATE TABLE t (v INT); INSERT INTO t VALUES (2);"));

        JudgeInitializationRequest request = new JudgeInitializationRequest(
                CodingProblemType.DATABASE,
                sqliteLanguage,
                "SELECT SUM(v) FROM t;",
                new CodeExecutionConstraints(5000L, 262144L),
                false,
                null,
                null,
                null,
                sampleTc,
                hiddenTc
        );

        SimpleCodeExecutionResult initOk = SimpleCodeExecutionResult.builder()
                .stdout("")
                .executionStatus(CodeExecutionStatus.SUCCESS)
                .exitCode(0L)
                .build();
        SimpleCodeExecutionResult r1 = SimpleCodeExecutionResult.builder()
                .stdout("1\n")
                .executionStatus(CodeExecutionStatus.SUCCESS)
                .exitCode(0L)
                .build();
        SimpleCodeExecutionResult r2 = SimpleCodeExecutionResult.builder()
                .stdout("2\n")
                .executionStatus(CodeExecutionStatus.SUCCESS)
                .exitCode(0L)
                .build();

        BatchCodeExecutionResult batch1 = BatchCodeExecutionResult.builder()
                .results(List.of(initOk, r1))
                .totalExecutions(2)
                .build();
        BatchCodeExecutionResult batch2 = BatchCodeExecutionResult.builder()
                .results(List.of(initOk, r2))
                .totalExecutions(2)
                .build();

        when(codeExecutionService.run(any(BatchCodeExecutionRequest.class)))
                .thenReturn(batch1)
                .thenReturn(batch2);

        InitializedTestcases result = judgeService.initializeExpectedOutputs(request);

        assertThat(result.sampleTestCases()).hasSize(1);
        assertThat(result.sampleTestCases().get(0).getOutput()).isEqualTo("1\n");
        assertThat(result.hiddenTestCases()).hasSize(1);
        assertThat(result.hiddenTestCases().get(0).getOutput()).isEqualTo("2\n");

        ArgumentCaptor<BatchCodeExecutionRequest> captor = ArgumentCaptor.forClass(BatchCodeExecutionRequest.class);
        verify(codeExecutionService, times(2)).run(captor.capture());
        List<BatchCodeExecutionRequest> allRequests = captor.getAllValues();

        // Verify separate files are created
        assertThat(allRequests.get(0).getFiles()).containsEntry("init.sql", "CREATE TABLE t (v INT); INSERT INTO t VALUES (1);");
        assertThat(allRequests.get(0).getFiles()).containsEntry("solution.sql", "SELECT SUM(v) FROM t;");
        assertThat(allRequests.get(0).getExecutions().get(0).getExecutablePath()).isEqualTo("init.sql");
        assertThat(allRequests.get(0).getExecutions().get(1).getExecutablePath()).isEqualTo("solution.sql");

        assertThat(allRequests.get(1).getFiles()).containsEntry("init.sql", "CREATE TABLE t (v INT); INSERT INTO t VALUES (2);");
        assertThat(allRequests.get(1).getFiles()).containsEntry("solution.sql", "SELECT SUM(v) FROM t;");
    }

    @Test
    @DisplayName("Throws BadRequestException when database initialization script fails")
    void testDatabaseProblemInitFailureThrowsBadRequestException() {
        List<TestcaseRequest> sampleTc = List.of(new TestcaseRequest("", "INVALID SQL SYNTAX;"));

        JudgeInitializationRequest request = new JudgeInitializationRequest(
                CodingProblemType.DATABASE,
                sqliteLanguage,
                "SELECT 1;",
                new CodeExecutionConstraints(5000L, 262144L),
                false,
                null,
                null,
                null,
                sampleTc,
                List.of(new TestcaseRequest("", "CREATE TABLE x (y INT);"))
        );

        SimpleCodeExecutionResult initFail = SimpleCodeExecutionResult.builder()
                .stdout("")
                .stderr("near \"INVALID\": syntax error")
                .executionStatus(CodeExecutionStatus.RUNTIME_ERROR)
                .exitCode(1L)
                .build();

        BatchCodeExecutionResult failBatch = BatchCodeExecutionResult.builder()
                .results(List.of(initFail, SimpleCodeExecutionResult.builder().build()))
                .totalExecutions(2)
                .build();

        when(codeExecutionService.run(any(BatchCodeExecutionRequest.class))).thenReturn(failBatch);

        assertThatThrownBy(() -> judgeService.initializeExpectedOutputs(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Database initialization script failed on testcase 1")
                .hasMessageContaining("RUNTIME_ERROR")
                .hasMessageContaining("syntax error");
    }

    @Test
    @DisplayName("Case B: Verification script required runs reference solution first, then stages 3-file args for verification")
    void testVerificationScriptSuccessWith3Files() {
        List<TestcaseRequest> sampleTc = List.of(new TestcaseRequest("4\n1 2 3 4", null));
        List<TestcaseRequest> hiddenTc = List.of(new TestcaseRequest("2\n5 10", null));

        JudgeInitializationRequest request = new JudgeInitializationRequest(
                CodingProblemType.GENERIC,
                javaLanguage,
                "public class Solution {}",
                new CodeExecutionConstraints(2000L, 262144L),
                true,
                pythonLanguage,
                "import sys; print('VERIFIED_OK')",
                new CodeExecutionConstraints(3000L, 262144L),
                sampleTc,
                hiddenTc
        );

        // Reference solution batch execution
        BatchCodeExecutionResult refBatchResult = BatchCodeExecutionResult.builder()
                .results(List.of(
                        SimpleCodeExecutionResult.builder().stdout("raw_out_1\n").executionStatus(CodeExecutionStatus.SUCCESS).exitCode(0L).build(),
                        SimpleCodeExecutionResult.builder().stdout("raw_out_2\n").executionStatus(CodeExecutionStatus.SUCCESS).exitCode(0L).build()
                ))
                .totalExecutions(2)
                .build();

        // Verification script batch execution
        BatchCodeExecutionResult verBatchResult = BatchCodeExecutionResult.builder()
                .results(List.of(
                        SimpleCodeExecutionResult.builder().stdout("verified_final_1\n").executionStatus(CodeExecutionStatus.SUCCESS).exitCode(0L).build(),
                        SimpleCodeExecutionResult.builder().stdout("verified_final_2\n").executionStatus(CodeExecutionStatus.SUCCESS).exitCode(0L).build()
                ))
                .totalExecutions(2)
                .build();

        when(codeExecutionService.run(any(BatchCodeExecutionRequest.class)))
                .thenReturn(refBatchResult)
                .thenReturn(verBatchResult);

        InitializedTestcases result = judgeService.initializeExpectedOutputs(request);

        assertThat(result.sampleTestCases().get(0).getOutput()).isEqualTo("raw_out_1\n");
        assertThat(result.hiddenTestCases().get(0).getOutput()).isEqualTo("raw_out_2\n");

        ArgumentCaptor<BatchCodeExecutionRequest> captor = ArgumentCaptor.forClass(BatchCodeExecutionRequest.class);
        verify(codeExecutionService, times(2)).run(captor.capture());
        BatchCodeExecutionRequest verCall = captor.getAllValues().get(1);

        assertThat(verCall.getLanguage()).isEqualTo(pythonLanguage);
        assertThat(verCall.getFiles().get("input_0.txt")).isEqualTo("4\n1 2 3 4");
        assertThat(verCall.getFiles().get("candidate_0.txt")).isEqualTo("raw_out_1\n");
        assertThat(verCall.getFiles().get("expected_0.txt")).isEqualTo("raw_out_1\n");
        assertThat(verCall.getExecutions().get(0).getArgs()).containsExactly("input_0.txt", "candidate_0.txt", "expected_0.txt");
        assertThat(verCall.getExecutions().get(0).getStdin()).isEqualTo("");

        assertThat(verCall.getFiles().get("input_1.txt")).isEqualTo("2\n5 10");
        assertThat(verCall.getFiles().get("candidate_1.txt")).isEqualTo("raw_out_2\n");
        assertThat(verCall.getFiles().get("expected_1.txt")).isEqualTo("raw_out_2\n");
        assertThat(verCall.getExecutions().get(1).getArgs()).containsExactly("input_1.txt", "candidate_1.txt", "expected_1.txt");
        assertThat(verCall.getExecutions().get(1).getStdin()).isEqualTo("");
    }

    @Test
    @DisplayName("Throws BadRequestException when reference solution has compilation error")
    void testReferenceSolutionCompilationFailureThrowsBadRequestException() {
        List<TestcaseRequest> sampleTc = List.of(new TestcaseRequest("1", null));
        List<TestcaseRequest> hiddenTc = List.of(new TestcaseRequest("2", null));

        JudgeInitializationRequest request = new JudgeInitializationRequest(
                CodingProblemType.GENERIC,
                javaLanguage,
                "bad java code",
                new CodeExecutionConstraints(2000L, 262144L),
                false,
                null,
                null,
                null,
                sampleTc,
                hiddenTc
        );

        BatchCodeExecutionResult failBatch = BatchCodeExecutionResult.builder()
                .results(List.of(
                        SimpleCodeExecutionResult.builder()
                                .stdout("")
                                .stderr("Solution.java:1: error: ';' expected")
                                .executionStatus(CodeExecutionStatus.COMPILATION_ERROR)
                                .exitCode(1L)
                                .build(),
                        SimpleCodeExecutionResult.builder()
                                .stdout("")
                                .stderr("Solution.java:1: error: ';' expected")
                                .executionStatus(CodeExecutionStatus.COMPILATION_ERROR)
                                .exitCode(1L)
                                .build()
                ))
                .totalExecutions(2)
                .build();

        when(codeExecutionService.run(any(BatchCodeExecutionRequest.class))).thenReturn(failBatch);

        assertThatThrownBy(() -> judgeService.initializeExpectedOutputs(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Reference solution failed on testcase 1")
                .hasMessageContaining("COMPILATION_ERROR")
                .hasMessageContaining("error: ';' expected");
    }

    @Test
    @DisplayName("Throws BadRequestException when reference solution times out (TLE)")
    void testReferenceSolutionTleThrowsBadRequestException() {
        List<TestcaseRequest> sampleTc = List.of(new TestcaseRequest("1", null));
        List<TestcaseRequest> hiddenTc = List.of(new TestcaseRequest("2", null));

        JudgeInitializationRequest request = new JudgeInitializationRequest(
                CodingProblemType.GENERIC,
                javaLanguage,
                "while(true){}",
                new CodeExecutionConstraints(2000L, 262144L),
                false,
                null,
                null,
                null,
                sampleTc,
                hiddenTc
        );

        BatchCodeExecutionResult failBatch = BatchCodeExecutionResult.builder()
                .results(List.of(
                        SimpleCodeExecutionResult.builder()
                                .stdout("")
                                .stderr("Time limit exceeded")
                                .executionStatus(CodeExecutionStatus.TIME_LIMIT_EXCEEDED)
                                .exitCode(137L)
                                .build(),
                        SimpleCodeExecutionResult.builder()
                                .stdout("")
                                .executionStatus(CodeExecutionStatus.SUCCESS)
                                .exitCode(0L)
                                .build()
                ))
                .totalExecutions(2)
                .build();

        when(codeExecutionService.run(any(BatchCodeExecutionRequest.class))).thenReturn(failBatch);

        assertThatThrownBy(() -> judgeService.initializeExpectedOutputs(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Reference solution failed on testcase 1")
                .hasMessageContaining("TIME_LIMIT_EXCEEDED");
    }

    @Test
    @DisplayName("Throws BadRequestException when reference solution batch result count mismatches testcase count")
    void testReferenceSolutionBatchCountMismatchThrowsBadRequestException() {
        List<TestcaseRequest> sampleTc = List.of(new TestcaseRequest("1", null));
        List<TestcaseRequest> hiddenTc = List.of(new TestcaseRequest("2", null));

        JudgeInitializationRequest request = new JudgeInitializationRequest(
                CodingProblemType.GENERIC,
                javaLanguage,
                "public class Solution {}",
                new CodeExecutionConstraints(2000L, 262144L),
                false,
                null,
                null,
                null,
                sampleTc,
                hiddenTc
        );

        BatchCodeExecutionResult mismatchBatch = BatchCodeExecutionResult.builder()
                .results(List.of(
                        SimpleCodeExecutionResult.builder().stdout("1").executionStatus(CodeExecutionStatus.SUCCESS).exitCode(0L).build()
                ))
                .totalExecutions(1)
                .build();

        when(codeExecutionService.run(any(BatchCodeExecutionRequest.class))).thenReturn(mismatchBatch);

        assertThatThrownBy(() -> judgeService.initializeExpectedOutputs(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Reference solution batch execution result count mismatch: expected 2 but got 1");
    }

    @Test
    @DisplayName("Throws BadRequestException when verification script fails execution")
    void testVerificationScriptFailureThrowsBadRequestException() {
        List<TestcaseRequest> sampleTc = List.of(new TestcaseRequest("1", null));
        List<TestcaseRequest> hiddenTc = List.of(new TestcaseRequest("2", null));

        JudgeInitializationRequest request = new JudgeInitializationRequest(
                CodingProblemType.GENERIC,
                javaLanguage,
                "public class Solution {}",
                new CodeExecutionConstraints(2000L, 262144L),
                true,
                pythonLanguage,
                "bad verification script",
                new CodeExecutionConstraints(3000L, 262144L),
                sampleTc,
                hiddenTc
        );

        BatchCodeExecutionResult refBatchResult = BatchCodeExecutionResult.builder()
                .results(List.of(
                        SimpleCodeExecutionResult.builder().stdout("ref1").executionStatus(CodeExecutionStatus.SUCCESS).exitCode(0L).build(),
                        SimpleCodeExecutionResult.builder().stdout("ref2").executionStatus(CodeExecutionStatus.SUCCESS).exitCode(0L).build()
                ))
                .totalExecutions(2)
                .build();

        BatchCodeExecutionResult verBatchResult = BatchCodeExecutionResult.builder()
                .results(List.of(
                        SimpleCodeExecutionResult.builder().stdout("ok").executionStatus(CodeExecutionStatus.SUCCESS).exitCode(0L).build(),
                        SimpleCodeExecutionResult.builder().stdout("").stderr("AssertionError: invalid answer").executionStatus(CodeExecutionStatus.RUNTIME_ERROR).exitCode(1L).build()
                ))
                .totalExecutions(2)
                .build();

        when(codeExecutionService.run(any(BatchCodeExecutionRequest.class)))
                .thenReturn(refBatchResult)
                .thenReturn(verBatchResult);

        assertThatThrownBy(() -> judgeService.initializeExpectedOutputs(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Verification script failed on testcase 2")
                .hasMessageContaining("RUNTIME_ERROR")
                .hasMessageContaining("AssertionError: invalid answer");
    }

    @Test
    @DisplayName("Validation fails on null request or missing fields")
    void testValidationRejectsNullOrEmptyInputs() {
        assertThatThrownBy(() -> judgeService.initializeExpectedOutputs(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Judge initialization request cannot be null");

        JudgeInitializationRequest missingRefLang = new JudgeInitializationRequest(
                CodingProblemType.GENERIC,
                null,
                "code",
                new CodeExecutionConstraints(2000L),
                false,
                null,
                null,
                null,
                List.of(new TestcaseRequest("1", null)),
                List.of(new TestcaseRequest("2", null))
        );
        assertThatThrownBy(() -> judgeService.initializeExpectedOutputs(missingRefLang))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Reference solution language is required");

        JudgeInitializationRequest missingSample = new JudgeInitializationRequest(
                CodingProblemType.GENERIC,
                javaLanguage,
                "code",
                new CodeExecutionConstraints(2000L),
                false,
                null,
                null,
                null,
                List.of(),
                List.of(new TestcaseRequest("2", null))
        );
        assertThatThrownBy(() -> judgeService.initializeExpectedOutputs(missingSample))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("At least one sample test case is required");

        JudgeInitializationRequest missingVerScript = new JudgeInitializationRequest(
                CodingProblemType.GENERIC,
                javaLanguage,
                "code",
                new CodeExecutionConstraints(2000L),
                true,
                pythonLanguage,
                "",
                new CodeExecutionConstraints(3000L),
                List.of(new TestcaseRequest("1", null)),
                List.of(new TestcaseRequest("2", null))
        );
        assertThatThrownBy(() -> judgeService.initializeExpectedOutputs(missingVerScript))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Verification script is required");
    }

    @Test
    @DisplayName("Case B: Database problem executes init.sql, solution.sql, and verifier.sql in order in single container, output from verifier")
    void testDatabaseProblemWithVerificationScriptSuccess() {
        List<TestcaseRequest> sampleTc = List.of(new TestcaseRequest("", "CREATE TABLE t (v INT); INSERT INTO t VALUES (10);"));
        List<TestcaseRequest> hiddenTc = List.of(new TestcaseRequest("", "CREATE TABLE t (v INT); INSERT INTO t VALUES (20);"));

        JudgeInitializationRequest request = new JudgeInitializationRequest(
                CodingProblemType.DATABASE,
                sqliteLanguage,
                "UPDATE t SET v = v * 2;",
                new CodeExecutionConstraints(5000L, 262144L),
                true,
                sqliteLanguage,
                "SELECT v FROM t;",
                new CodeExecutionConstraints(5000L, 262144L),
                sampleTc,
                hiddenTc
        );

        SimpleCodeExecutionResult initOk = SimpleCodeExecutionResult.builder().stdout("").executionStatus(CodeExecutionStatus.SUCCESS).exitCode(0L).build();
        SimpleCodeExecutionResult solOk = SimpleCodeExecutionResult.builder().stdout("").executionStatus(CodeExecutionStatus.SUCCESS).exitCode(0L).build();
        SimpleCodeExecutionResult ver1 = SimpleCodeExecutionResult.builder().stdout("20\n").executionStatus(CodeExecutionStatus.SUCCESS).exitCode(0L).build();
        SimpleCodeExecutionResult ver2 = SimpleCodeExecutionResult.builder().stdout("40\n").executionStatus(CodeExecutionStatus.SUCCESS).exitCode(0L).build();

        BatchCodeExecutionResult batch1 = BatchCodeExecutionResult.builder().results(List.of(initOk, solOk, ver1)).totalExecutions(3).build();
        BatchCodeExecutionResult batch2 = BatchCodeExecutionResult.builder().results(List.of(initOk, solOk, ver2)).totalExecutions(3).build();

        when(codeExecutionService.run(any(BatchCodeExecutionRequest.class)))
                .thenReturn(batch1)
                .thenReturn(batch2);

        InitializedTestcases result = judgeService.initializeExpectedOutputs(request);

        assertThat(result.sampleTestCases()).hasSize(1);
        assertThat(result.sampleTestCases().get(0).getOutput()).isEqualTo("20\n");
        assertThat(result.hiddenTestCases()).hasSize(1);
        assertThat(result.hiddenTestCases().get(0).getOutput()).isEqualTo("40\n");

        ArgumentCaptor<BatchCodeExecutionRequest> captor = ArgumentCaptor.forClass(BatchCodeExecutionRequest.class);
        verify(codeExecutionService, times(2)).run(captor.capture());
        List<BatchCodeExecutionRequest> requests = captor.getAllValues();

        BatchCodeExecutionRequest req1 = requests.get(0);
        assertThat(req1.getFiles()).containsEntry("init.sql", "CREATE TABLE t (v INT); INSERT INTO t VALUES (10);");
        assertThat(req1.getFiles()).containsEntry("solution.sql", "UPDATE t SET v = v * 2;");
        assertThat(req1.getFiles()).containsEntry("verifier.sql", "SELECT v FROM t;");

        assertThat(req1.getExecutions()).hasSize(3);
        assertThat(req1.getExecutions().get(0).getExecutablePath()).isEqualTo("init.sql");
        assertThat(req1.getExecutions().get(1).getExecutablePath()).isEqualTo("solution.sql");
        assertThat(req1.getExecutions().get(2).getExecutablePath()).isEqualTo("verifier.sql");
    }

    @Test
    @DisplayName("Database problem without init.sql executes solution.sql and verifier.sql in order")
    void testDatabaseProblemWithoutInitScriptWithVerificationSuccess() {
        List<TestcaseRequest> sampleTc = List.of(new TestcaseRequest("", null));

        JudgeInitializationRequest request = new JudgeInitializationRequest(
                CodingProblemType.DATABASE,
                sqliteLanguage,
                "SELECT 1;",
                new CodeExecutionConstraints(5000L, 262144L),
                true,
                sqliteLanguage,
                "SELECT 2;",
                new CodeExecutionConstraints(5000L, 262144L),
                sampleTc,
                List.of(new TestcaseRequest("", ""))
        );

        SimpleCodeExecutionResult solOk = SimpleCodeExecutionResult.builder().stdout("1\n").executionStatus(CodeExecutionStatus.SUCCESS).exitCode(0L).build();
        SimpleCodeExecutionResult verOk = SimpleCodeExecutionResult.builder().stdout("2\n").executionStatus(CodeExecutionStatus.SUCCESS).exitCode(0L).build();
        BatchCodeExecutionResult batch = BatchCodeExecutionResult.builder().results(List.of(solOk, verOk)).totalExecutions(2).build();

        when(codeExecutionService.run(any(BatchCodeExecutionRequest.class))).thenReturn(batch);

        InitializedTestcases result = judgeService.initializeExpectedOutputs(request);

        assertThat(result.sampleTestCases().get(0).getOutput()).isEqualTo("2\n");
        assertThat(result.hiddenTestCases().get(0).getOutput()).isEqualTo("2\n");

        ArgumentCaptor<BatchCodeExecutionRequest> captor = ArgumentCaptor.forClass(BatchCodeExecutionRequest.class);
        verify(codeExecutionService, times(2)).run(captor.capture());
        BatchCodeExecutionRequest captured = captor.getAllValues().get(0);
        assertThat(captured.getFiles()).doesNotContainKey("init.sql");
        assertThat(captured.getExecutions()).hasSize(2);
        assertThat(captured.getExecutions().get(0).getExecutablePath()).isEqualTo("solution.sql");
        assertThat(captured.getExecutions().get(1).getExecutablePath()).isEqualTo("verifier.sql");
    }

    @Test
    @DisplayName("Preserves exact stdout whitespace, newlines, and tabs without any trimming")
    void testOutputWhitespacePreservedExactly() {
        String exactOutput = "  leading space\n\tindented tab\r\n trailing spaces   \n\n";
        List<TestcaseRequest> sampleTc = List.of(new TestcaseRequest("input", null));

        JudgeInitializationRequest request = new JudgeInitializationRequest(
                CodingProblemType.GENERIC,
                javaLanguage,
                "public class Solution {}",
                new CodeExecutionConstraints(2000L, 262144L),
                false,
                null,
                null,
                null,
                sampleTc,
                List.of(new TestcaseRequest("input2", null))
        );

        SimpleCodeExecutionResult r = SimpleCodeExecutionResult.builder()
                .stdout(exactOutput)
                .executionStatus(CodeExecutionStatus.SUCCESS)
                .exitCode(0L)
                .build();
        BatchCodeExecutionResult batchResult = BatchCodeExecutionResult.builder()
                .results(List.of(r, r))
                .totalExecutions(2)
                .build();

        when(codeExecutionService.run(any(BatchCodeExecutionRequest.class))).thenReturn(batchResult);

        InitializedTestcases result = judgeService.initializeExpectedOutputs(request);

        assertThat(result.sampleTestCases().get(0).getOutput()).isSameAs(exactOutput);
        assertThat(result.sampleTestCases().get(0).getOutput()).isEqualTo(exactOutput);
    }

    @Test
    @DisplayName("Handles null stdout consistently without inventing empty string")
    void testOutputNullPreservedAsNull() {
        List<TestcaseRequest> sampleTc = List.of(new TestcaseRequest("input", null));

        JudgeInitializationRequest request = new JudgeInitializationRequest(
                CodingProblemType.GENERIC,
                javaLanguage,
                "public class Solution {}",
                new CodeExecutionConstraints(2000L, 262144L),
                false,
                null,
                null,
                null,
                sampleTc,
                List.of(new TestcaseRequest("input2", null))
        );

        SimpleCodeExecutionResult r = SimpleCodeExecutionResult.builder()
                .stdout(null)
                .executionStatus(CodeExecutionStatus.SUCCESS)
                .exitCode(0L)
                .build();
        BatchCodeExecutionResult batchResult = BatchCodeExecutionResult.builder()
                .results(List.of(r, r))
                .totalExecutions(2)
                .build();

        when(codeExecutionService.run(any(BatchCodeExecutionRequest.class))).thenReturn(batchResult);

        InitializedTestcases result = judgeService.initializeExpectedOutputs(request);

        assertThat(result.sampleTestCases().get(0).getOutput()).isNull();
    }

    @Test
    @DisplayName("Throws BadRequestException with testcase number and details when reference solution encounters runtime error")
    void testReferenceSolutionRuntimeErrorThrowsBadRequestException() {
        JudgeInitializationRequest request = new JudgeInitializationRequest(
                CodingProblemType.GENERIC,
                javaLanguage,
                "public class Solution {}",
                new CodeExecutionConstraints(2000L, 262144L),
                false,
                null,
                null,
                null,
                List.of(new TestcaseRequest("1", null)),
                List.of(new TestcaseRequest("2", null))
        );

        SimpleCodeExecutionResult r1 = SimpleCodeExecutionResult.builder().stdout("1").executionStatus(CodeExecutionStatus.SUCCESS).exitCode(0L).build();
        SimpleCodeExecutionResult r2 = SimpleCodeExecutionResult.builder().stdout("").stderr("java.lang.NullPointerException: foo is null").executionStatus(CodeExecutionStatus.RUNTIME_ERROR).exitCode(1L).build();

        BatchCodeExecutionResult batchResult = BatchCodeExecutionResult.builder().results(List.of(r1, r2)).totalExecutions(2).build();
        when(codeExecutionService.run(any(BatchCodeExecutionRequest.class))).thenReturn(batchResult);

        assertThatThrownBy(() -> judgeService.initializeExpectedOutputs(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Reference solution failed on testcase 2")
                .hasMessageContaining("RUNTIME_ERROR")
                .hasMessageContaining("exitCode=1")
                .hasMessageContaining("java.lang.NullPointerException: foo is null");
    }

    @Test
    @DisplayName("Throws BadRequestException when reference solution exceeds memory limit (MLE)")
    void testReferenceSolutionMleThrowsBadRequestException() {
        JudgeInitializationRequest request = new JudgeInitializationRequest(
                CodingProblemType.GENERIC,
                javaLanguage,
                "public class Solution {}",
                new CodeExecutionConstraints(2000L, 262144L),
                false,
                null,
                null,
                null,
                List.of(new TestcaseRequest("1", null)),
                List.of(new TestcaseRequest("2", null))
        );

        SimpleCodeExecutionResult r1 = SimpleCodeExecutionResult.builder().stdout("").executionStatus(CodeExecutionStatus.MEMORY_LIMIT_EXCEEDED).exitCode(137L).build();
        BatchCodeExecutionResult batchResult = BatchCodeExecutionResult.builder().results(List.of(r1, r1)).totalExecutions(2).build();
        when(codeExecutionService.run(any(BatchCodeExecutionRequest.class))).thenReturn(batchResult);

        assertThatThrownBy(() -> judgeService.initializeExpectedOutputs(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Reference solution failed on testcase 1")
                .hasMessageContaining("MEMORY_LIMIT_EXCEEDED");
    }

    @Test
    @DisplayName("Throws BadRequestException when reference solution terminates abnormally with signal")
    void testReferenceSolutionAbnormalExitSignalThrowsBadRequestException() {
        JudgeInitializationRequest request = new JudgeInitializationRequest(
                CodingProblemType.GENERIC,
                javaLanguage,
                "public class Solution {}",
                new CodeExecutionConstraints(2000L, 262144L),
                false,
                null,
                null,
                null,
                List.of(new TestcaseRequest("1", null)),
                List.of(new TestcaseRequest("2", null))
        );

        SimpleCodeExecutionResult r = SimpleCodeExecutionResult.builder()
                .stdout("")
                .stderr("Segmentation fault")
                .executionStatus(CodeExecutionStatus.SUCCESS)
                .exitCode(139L)
                .exitSignal(11L)
                .build();

        BatchCodeExecutionResult batchResult = BatchCodeExecutionResult.builder().results(List.of(r, r)).totalExecutions(2).build();
        when(codeExecutionService.run(any(BatchCodeExecutionRequest.class))).thenReturn(batchResult);

        assertThatThrownBy(() -> judgeService.initializeExpectedOutputs(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Reference solution failed on testcase 1")
                .hasMessageContaining("signal=11")
                .hasMessageContaining("Segmentation fault");
    }

    @Test
    @DisplayName("Throws BadRequestException when execution result is missing execution status")
    void testReferenceSolutionMissingStatusThrowsBadRequestException() {
        JudgeInitializationRequest request = new JudgeInitializationRequest(
                CodingProblemType.GENERIC,
                javaLanguage,
                "public class Solution {}",
                new CodeExecutionConstraints(2000L, 262144L),
                false,
                null,
                null,
                null,
                List.of(new TestcaseRequest("1", null)),
                List.of(new TestcaseRequest("2", null))
        );

        SimpleCodeExecutionResult r = SimpleCodeExecutionResult.builder()
                .stdout("")
                .stderr("mystery error")
                .executionStatus(null)
                .exitCode(1L)
                .build();

        BatchCodeExecutionResult batchResult = BatchCodeExecutionResult.builder().results(List.of(r, r)).totalExecutions(2).build();
        when(codeExecutionService.run(any(BatchCodeExecutionRequest.class))).thenReturn(batchResult);

        assertThatThrownBy(() -> judgeService.initializeExpectedOutputs(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Reference solution execution missing status on testcase 1")
                .hasMessageContaining("mystery error");
    }

    @Test
    @DisplayName("Throws BadRequestException when execution result element inside batch is null")
    void testReferenceSolutionNullResultThrowsBadRequestException() {
        JudgeInitializationRequest request = new JudgeInitializationRequest(
                CodingProblemType.GENERIC,
                javaLanguage,
                "public class Solution {}",
                new CodeExecutionConstraints(2000L, 262144L),
                false,
                null,
                null,
                null,
                List.of(new TestcaseRequest("1", null)),
                List.of(new TestcaseRequest("2", null))
        );

        List<SimpleCodeExecutionResult> resultsWithNull = new ArrayList<>();
        resultsWithNull.add(null);
        resultsWithNull.add(null);

        BatchCodeExecutionResult batchResult = BatchCodeExecutionResult.builder().results(resultsWithNull).totalExecutions(2).build();
        when(codeExecutionService.run(any(BatchCodeExecutionRequest.class))).thenReturn(batchResult);

        assertThatThrownBy(() -> judgeService.initializeExpectedOutputs(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Reference solution execution returned null result on testcase 1");
    }

    @Test
    @DisplayName("Throws BadRequestException when batch execution service returns null")
    void testReferenceSolutionBatchExecutionNullResultThrowsBadRequestException() {
        JudgeInitializationRequest request = new JudgeInitializationRequest(
                CodingProblemType.GENERIC,
                javaLanguage,
                "public class Solution {}",
                new CodeExecutionConstraints(2000L, 262144L),
                false,
                null,
                null,
                null,
                List.of(new TestcaseRequest("1", null)),
                List.of(new TestcaseRequest("2", null))
        );

        when(codeExecutionService.run(any(BatchCodeExecutionRequest.class))).thenReturn(null);

        assertThatThrownBy(() -> judgeService.initializeExpectedOutputs(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Reference solution batch execution returned null result");
    }

    @Test
    @DisplayName("Throws BadRequestException when solution.sql fails in database execution")
    void testDatabaseProblemSolutionFailureThrowsBadRequestException() {
        JudgeInitializationRequest request = new JudgeInitializationRequest(
                CodingProblemType.DATABASE,
                sqliteLanguage,
                "SELECT * FROM nonexistent_table;",
                new CodeExecutionConstraints(5000L, 262144L),
                false,
                null,
                null,
                null,
                List.of(new TestcaseRequest("", "CREATE TABLE t (id INT);")),
                List.of(new TestcaseRequest("", "CREATE TABLE t (id INT);"))
        );

        SimpleCodeExecutionResult initOk = SimpleCodeExecutionResult.builder().stdout("").executionStatus(CodeExecutionStatus.SUCCESS).exitCode(0L).build();
        SimpleCodeExecutionResult solFail = SimpleCodeExecutionResult.builder()
                .stdout("")
                .stderr("no such table: nonexistent_table")
                .executionStatus(CodeExecutionStatus.RUNTIME_ERROR)
                .exitCode(1L)
                .build();

        BatchCodeExecutionResult batchResult = BatchCodeExecutionResult.builder().results(List.of(initOk, solFail)).totalExecutions(2).build();
        when(codeExecutionService.run(any(BatchCodeExecutionRequest.class))).thenReturn(batchResult);

        assertThatThrownBy(() -> judgeService.initializeExpectedOutputs(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Reference solution failed on testcase 1")
                .hasMessageContaining("no such table: nonexistent_table");
    }

    @Test
    @DisplayName("Throws BadRequestException when verification script fails in database execution")
    void testDatabaseProblemVerifierFailureThrowsBadRequestException() {
        JudgeInitializationRequest request = new JudgeInitializationRequest(
                CodingProblemType.DATABASE,
                sqliteLanguage,
                "UPDATE t SET v = 1;",
                new CodeExecutionConstraints(5000L, 262144L),
                true,
                sqliteLanguage,
                "SELECT * FROM broken_view;",
                new CodeExecutionConstraints(5000L, 262144L),
                List.of(new TestcaseRequest("", "CREATE TABLE t (v INT);")),
                List.of(new TestcaseRequest("", "CREATE TABLE t (v INT);"))
        );

        SimpleCodeExecutionResult initOk = SimpleCodeExecutionResult.builder().stdout("").executionStatus(CodeExecutionStatus.SUCCESS).exitCode(0L).build();
        SimpleCodeExecutionResult solOk = SimpleCodeExecutionResult.builder().stdout("").executionStatus(CodeExecutionStatus.SUCCESS).exitCode(0L).build();
        SimpleCodeExecutionResult verFail = SimpleCodeExecutionResult.builder()
                .stdout("")
                .stderr("no such view: broken_view")
                .executionStatus(CodeExecutionStatus.RUNTIME_ERROR)
                .exitCode(1L)
                .build();

        BatchCodeExecutionResult batchResult = BatchCodeExecutionResult.builder().results(List.of(initOk, solOk, verFail)).totalExecutions(3).build();
        when(codeExecutionService.run(any(BatchCodeExecutionRequest.class))).thenReturn(batchResult);

        assertThatThrownBy(() -> judgeService.initializeExpectedOutputs(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Verification script failed on testcase 1")
                .hasMessageContaining("no such view: broken_view");
    }

    @Test
    @DisplayName("Throws BadRequestException when database execution result count mismatches expected item count")
    void testDatabaseProblemCountMismatchThrowsBadRequestException() {
        JudgeInitializationRequest request = new JudgeInitializationRequest(
                CodingProblemType.DATABASE,
                sqliteLanguage,
                "SELECT 1;",
                new CodeExecutionConstraints(5000L, 262144L),
                false,
                null,
                null,
                null,
                List.of(new TestcaseRequest("", "CREATE TABLE t (id INT);")),
                List.of(new TestcaseRequest("", "CREATE TABLE t (id INT);"))
        );

        BatchCodeExecutionResult batchResult = BatchCodeExecutionResult.builder()
                .results(List.of(SimpleCodeExecutionResult.builder().stdout("").executionStatus(CodeExecutionStatus.SUCCESS).exitCode(0L).build()))
                .totalExecutions(1)
                .build();
        when(codeExecutionService.run(any(BatchCodeExecutionRequest.class))).thenReturn(batchResult);

        assertThatThrownBy(() -> judgeService.initializeExpectedOutputs(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Database execution result count mismatch on testcase 1: expected 2 but got 1");
    }

    @Test
    @DisplayName("Throws BadRequestException when database problem specifies non-database verification language")
    void testDatabaseProblemMismatchedVerificationLanguageThrowsBadRequestException() {
        JudgeInitializationRequest request = new JudgeInitializationRequest(
                CodingProblemType.DATABASE,
                sqliteLanguage,
                "SELECT 1;",
                new CodeExecutionConstraints(5000L, 262144L),
                true,
                pythonLanguage,
                "print(1)",
                new CodeExecutionConstraints(3000L, 262144L),
                List.of(new TestcaseRequest("", "CREATE TABLE t (id INT);")),
                List.of(new TestcaseRequest("", "CREATE TABLE t (id INT);"))
        );

        assertThatThrownBy(() -> judgeService.initializeExpectedOutputs(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Verification script for a DATABASE problem must be a database language");
    }

    @Test
    @DisplayName("Throws BadRequestException when database problem verification language does not match reference language")
    void testDatabaseProblemDifferentDatabaseVerificationLanguageThrowsBadRequestException() {
        JudgeInitializationRequest request = new JudgeInitializationRequest(
                CodingProblemType.DATABASE,
                sqliteLanguage,
                "SELECT 1;",
                new CodeExecutionConstraints(5000L, 262144L),
                true,
                postgresLanguage,
                "SELECT 1;",
                new CodeExecutionConstraints(5000L, 262144L),
                List.of(new TestcaseRequest("", "CREATE TABLE t (id INT);")),
                List.of(new TestcaseRequest("", "CREATE TABLE t (id INT);"))
        );

        assertThatThrownBy(() -> judgeService.initializeExpectedOutputs(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must match reference solution language 'sqlite-3'");
    }

    @Test
    @DisplayName("Throws BadRequestException when generic problem uses database reference language")
    void testGenericProblemDatabaseReferenceLanguageThrowsBadRequestException() {
        JudgeInitializationRequest request = new JudgeInitializationRequest(
                CodingProblemType.GENERIC,
                sqliteLanguage,
                "SELECT 1;",
                new CodeExecutionConstraints(5000L, 262144L),
                false,
                null,
                null,
                null,
                List.of(new TestcaseRequest("1", null)),
                List.of(new TestcaseRequest("2", null))
        );

        assertThatThrownBy(() -> judgeService.initializeExpectedOutputs(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Database language 'sqlite-3' cannot be used as reference solution for a GENERIC problem");
    }

    @Test
    @DisplayName("Throws BadRequestException when generic problem uses database verification language")
    void testGenericProblemDatabaseVerificationLanguageThrowsBadRequestException() {
        JudgeInitializationRequest request = new JudgeInitializationRequest(
                CodingProblemType.GENERIC,
                javaLanguage,
                "public class Solution {}",
                new CodeExecutionConstraints(2000L, 262144L),
                true,
                sqliteLanguage,
                "SELECT 1;",
                new CodeExecutionConstraints(5000L, 262144L),
                List.of(new TestcaseRequest("1", null)),
                List.of(new TestcaseRequest("2", null))
        );

        assertThatThrownBy(() -> judgeService.initializeExpectedOutputs(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Database language 'sqlite-3' cannot be used as verification script for a GENERIC problem");
    }

    @Test
    @DisplayName("Throws BadRequestException when sample or hidden testcases contain null elements")
    void testValidationRejectsNullTestcaseElements() {
        List<TestcaseRequest> sampleWithNull = new ArrayList<>();
        sampleWithNull.add(null);

        JudgeInitializationRequest req1 = new JudgeInitializationRequest(
                CodingProblemType.GENERIC,
                javaLanguage,
                "code",
                new CodeExecutionConstraints(2000L, 262144L),
                false,
                null,
                null,
                null,
                sampleWithNull,
                List.of(new TestcaseRequest("1", null))
        );

        assertThatThrownBy(() -> judgeService.initializeExpectedOutputs(req1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Sample testcase at index 0 cannot be null");

        List<TestcaseRequest> hiddenWithNull = new ArrayList<>();
        hiddenWithNull.add(null);

        JudgeInitializationRequest req2 = new JudgeInitializationRequest(
                CodingProblemType.GENERIC,
                javaLanguage,
                "code",
                new CodeExecutionConstraints(2000L, 262144L),
                false,
                null,
                null,
                null,
                List.of(new TestcaseRequest("1", null)),
                hiddenWithNull
        );

        assertThatThrownBy(() -> judgeService.initializeExpectedOutputs(req2))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Hidden testcase at index 0 cannot be null");
    }

    // =========================================================================
    // Preview Execution Tests
    // =========================================================================

    @Test
    @DisplayName("previewExecution: Generic problem runs batch and returns SUCCESS without storing")
    void testPreviewGenericExecutionSuccess() {
        when(languageFileNameRegistry.resolveReferenceSolutionFileName(javaLanguage)).thenReturn("Solution.java");

        SimpleCodeExecutionResult r1 = SimpleCodeExecutionResult.builder()
                .executionStatus(CodeExecutionStatus.SUCCESS)
                .stdout("3")
                .stderr("")
                .exitCode(0L)
                .executionTimeMs(15L)
                .memoryUsageKb(20480L)
                .build();
        SimpleCodeExecutionResult r2 = SimpleCodeExecutionResult.builder()
                .executionStatus(CodeExecutionStatus.SUCCESS)
                .stdout("7")
                .stderr("")
                .exitCode(0L)
                .executionTimeMs(18L)
                .memoryUsageKb(21500L)
                .build();

        when(codeExecutionService.run(any(BatchCodeExecutionRequest.class)))
                .thenReturn(BatchCodeExecutionResult.builder()
                        .results(List.of(r1, r2))
                        .totalExecutionTimeMs(33L)
                        .totalExecutions(2)
                        .successfulExecutions(2)
                        .failedExecutions(0)
                        .build());

        JudgePreviewRequest request = new JudgePreviewRequest(
                CodingProblemType.GENERIC,
                javaLanguage,
                "class Solution {}",
                new CodeExecutionConstraints(2000L, 262144L),
                false,
                null,
                null,
                null,
                List.of(
                        new PreviewTestcaseRequest("case-1", "1 2", null),
                        new PreviewTestcaseRequest("case-2", "3 4", null)
                )
        );

        PreviewRunResponse response = judgeService.previewExecution(request);

        assertThat(response).isNotNull();
        assertThat(response.getOverallStatus()).isEqualTo("SUCCESS");
        assertThat(response.getTotalTestcases()).isEqualTo(2);
        assertThat(response.getPassedTestcases()).isEqualTo(2);
        assertThat(response.getFailedTestcases()).isEqualTo(0);
        assertThat(response.getResults()).hasSize(2);
        assertThat(response.getResults().get(0).getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getResults().get(0).getStdout()).isEqualTo("3");
        assertThat(response.getResults().get(1).getStdout()).isEqualTo("7");
    }

    @Test
    @DisplayName("previewExecution: Compilation error returns COMPILATION_ERROR status without throwing exception")
    void testPreviewGenericExecutionCompilationError() {
        when(languageFileNameRegistry.resolveReferenceSolutionFileName(javaLanguage)).thenReturn("Solution.java");

        SimpleCodeExecutionResult compileErr = SimpleCodeExecutionResult.builder()
                .executionStatus(CodeExecutionStatus.COMPILATION_ERROR)
                .stdout("")
                .stderr("error: ';' expected at line 4")
                .exitCode(1L)
                .build();

        when(codeExecutionService.run(any(BatchCodeExecutionRequest.class)))
                .thenReturn(BatchCodeExecutionResult.builder()
                        .results(List.of(compileErr))
                        .totalExecutionTimeMs(0L)
                        .totalExecutions(1)
                        .successfulExecutions(0)
                        .failedExecutions(1)
                        .build());

        JudgePreviewRequest request = new JudgePreviewRequest(
                CodingProblemType.GENERIC,
                javaLanguage,
                "class Solution broken",
                new CodeExecutionConstraints(2000L, 262144L),
                false,
                null,
                null,
                null,
                List.of(new PreviewTestcaseRequest("case-1", "1 2", null))
        );

        PreviewRunResponse response = judgeService.previewExecution(request);

        assertThat(response).isNotNull();
        assertThat(response.getOverallStatus()).isEqualTo("COMPILATION_ERROR");
        assertThat(response.getPassedTestcases()).isEqualTo(0);
        assertThat(response.getFailedTestcases()).isEqualTo(1);
        assertThat(response.getResults().get(0).getStatus()).isEqualTo("COMPILATION_ERROR");
        assertThat(response.getResults().get(0).getStderr()).contains("';' expected");
    }

    @Test
    @DisplayName("previewExecution: Database execution runs init and solution")
    void testPreviewDatabaseExecutionSuccess() {
        when(languageFileNameRegistry.resolveDatabaseInitFileName(sqliteLanguage)).thenReturn("init.sql");
        when(languageFileNameRegistry.resolveDatabaseSolutionFileName(sqliteLanguage)).thenReturn("solution.sql");

        SimpleCodeExecutionResult initRes = SimpleCodeExecutionResult.builder()
                .executionStatus(CodeExecutionStatus.SUCCESS)
                .stdout("")
                .exitCode(0L)
                .executionTimeMs(10L)
                .build();
        SimpleCodeExecutionResult solRes = SimpleCodeExecutionResult.builder()
                .executionStatus(CodeExecutionStatus.SUCCESS)
                .stdout("1|Alice\n2|Bob")
                .exitCode(0L)
                .executionTimeMs(12L)
                .memoryUsageKb(15000L)
                .build();

        when(codeExecutionService.run(any(BatchCodeExecutionRequest.class)))
                .thenReturn(BatchCodeExecutionResult.builder()
                        .results(List.of(initRes, solRes))
                        .totalExecutionTimeMs(22L)
                        .totalExecutions(2)
                        .successfulExecutions(2)
                        .failedExecutions(0)
                        .build());

        JudgePreviewRequest request = new JudgePreviewRequest(
                CodingProblemType.DATABASE,
                sqliteLanguage,
                "SELECT * FROM Users;",
                new CodeExecutionConstraints(2000L, 262144L),
                false,
                null,
                null,
                null,
                List.of(new PreviewTestcaseRequest("case-1", null, "CREATE TABLE Users (id INT, name TEXT);"))
        );

        PreviewRunResponse response = judgeService.previewExecution(request);

        assertThat(response).isNotNull();
        assertThat(response.getOverallStatus()).isEqualTo("SUCCESS");
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getResults().get(0).getStdout()).isEqualTo("1|Alice\n2|Bob");
    }

    @Test
    @DisplayName("executeGenerator executes code via CodeExecutionService and returns mapped response")
    void testExecuteGenerator_Success() {
        when(pythonLanguage.type()).thenReturn(LanguageType.INTERPRETED);
        when(languageFileNameRegistry.resolveDefaultFileName(pythonLanguage)).thenReturn("generator.py");

        SimpleCodeExecutionResult execResult = SimpleCodeExecutionResult.builder()
                .executionStatus(CodeExecutionStatus.SUCCESS)
                .stdout("1 2 3\n---STACKED_TESTCASE_DELIMITER---\n4 5 6\n")
                .stderr("")
                .exitCode(0L)
                .executionTimeMs(85L)
                .memoryUsageKb(12345L)
                .build();

        when(codeExecutionService.run(any(SimpleCodeExecutionRequest.class))).thenReturn(execResult);

        CodeExecutionConstraints constraints = new CodeExecutionConstraints(3000L, 262144L);
        com.bharath.stacked.modules.coding_problem.dto.response.TestcaseGeneratorRunResponse response =
                judgeService.executeGenerator(pythonLanguage, "print('code')", "seed_123", constraints);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(response.stdout()).isEqualTo("1 2 3\n---STACKED_TESTCASE_DELIMITER---\n4 5 6\n");
        assertThat(response.executionTimeMs()).isEqualTo(85L);
        assertThat(response.memoryUsageKb()).isEqualTo(12345L);
        assertThat(response.error()).isNull();

        ArgumentCaptor<SimpleCodeExecutionRequest> captor = ArgumentCaptor.forClass(SimpleCodeExecutionRequest.class);
        verify(codeExecutionService).run(captor.capture());
        SimpleCodeExecutionRequest captured = captor.getValue();
        assertThat(captured.getLanguage()).isEqualTo(pythonLanguage);
        assertThat(captured.getFileName()).isEqualTo("generator.py");
        assertThat(captured.getCode()).isEqualTo("print('code')");
        assertThat(captured.getStdin()).isEqualTo("seed_123");
        assertThat(captured.getConstraints()).isEqualTo(constraints);
    }

    @Test
    @DisplayName("executeGenerator maps error diagnostics when script execution fails")
    void testExecuteGenerator_Failure() {
        when(pythonLanguage.type()).thenReturn(LanguageType.INTERPRETED);
        when(languageFileNameRegistry.resolveDefaultFileName(pythonLanguage)).thenReturn("generator.py");

        SimpleCodeExecutionResult execResult = SimpleCodeExecutionResult.builder()
                .executionStatus(CodeExecutionStatus.RUNTIME_ERROR)
                .stdout("")
                .stderr("ZeroDivisionError: division by zero")
                .exitCode(1L)
                .executionTimeMs(50L)
                .memoryUsageKb(10000L)
                .build();

        when(codeExecutionService.run(any(SimpleCodeExecutionRequest.class))).thenReturn(execResult);

        com.bharath.stacked.modules.coding_problem.dto.response.TestcaseGeneratorRunResponse response =
                judgeService.executeGenerator(pythonLanguage, "print(1/0)", null, new CodeExecutionConstraints(2000L, 262144L));

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
        assertThat(response.error()).contains("ZeroDivisionError");
    }

    @Test
    @DisplayName("executeGenerator throws BadRequestException if database language is used")
    void testExecuteGenerator_DatabaseLanguage_ThrowsBadRequest() {
        when(sqliteLanguage.type()).thenReturn(LanguageType.DATABASE);

        assertThatThrownBy(() -> judgeService.executeGenerator(
                sqliteLanguage,
                "SELECT 1;",
                "",
                new CodeExecutionConstraints(2000L, 262144L)
        )).isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Database languages cannot be used for testcase generation.");
    }

    @Test
    @DisplayName("executeCandidate without verification script compares actual and expected output")
    void testExecuteCandidate_DirectComparison_SuccessAndWrongAnswer() {
        List<CandidateTestcaseItem> items = List.of(
                new CandidateTestcaseItem("case-1", 1, "1 2", null, "3", false, true),
                new CandidateTestcaseItem("case-2", 2, "3 4", null, "7", false, true)
        );

        JudgeCandidateExecutionRequest request = new JudgeCandidateExecutionRequest(
                CodingProblemType.GENERIC,
                pythonLanguage,
                "import sys\nnums=sys.stdin.read().split()\nprint(int(nums[0])+int(nums[1]))",
                new CodeExecutionConstraints(2000L, 262144L),
                pythonLanguage,
                "print(3)",
                new CodeExecutionConstraints(2000L, 262144L),
                false,
                null,
                null,
                null,
                items
        );

        SimpleCodeExecutionResult r1 = SimpleCodeExecutionResult.builder()
                .executionStatus(CodeExecutionStatus.SUCCESS)
                .stdout("3\n")
                .stderr("")
                .exitCode(0L)
                .executionTimeMs(50L)
                .memoryUsageKb(10240L)
                .build();

        SimpleCodeExecutionResult r2 = SimpleCodeExecutionResult.builder()
                .executionStatus(CodeExecutionStatus.SUCCESS)
                .stdout("999\n") // Wrong answer
                .stderr("")
                .exitCode(0L)
                .executionTimeMs(60L)
                .memoryUsageKb(10240L)
                .build();

        when(codeExecutionService.run(any(BatchCodeExecutionRequest.class)))
                .thenReturn(BatchCodeExecutionResult.builder().results(List.of(r1, r2)).totalExecutions(2).build());

        CodingProblemExecutionResponse response = judgeService.executeCandidate(request);

        assertThat(response).isNotNull();
        assertThat(response.getOverallStatus()).isEqualTo("WRONG_ANSWER");
        assertThat(response.getTotalTestcases()).isEqualTo(2);
        assertThat(response.getPassedTestcases()).isEqualTo(1);
        assertThat(response.getFailedTestcases()).isEqualTo(1);

        assertThat(response.getResults().get(0).getStatus()).isEqualTo("PASSED");
        assertThat(response.getResults().get(0).getActualOutput()).isEqualTo("3\n");
        assertThat(response.getResults().get(0).getExpectedOutput()).isEqualTo("3");

        assertThat(response.getResults().get(1).getStatus()).isEqualTo("WRONG_ANSWER");
        assertThat(response.getResults().get(1).getActualOutput()).isEqualTo("999\n");
        assertThat(response.getResults().get(1).getExpectedOutput()).isEqualTo("7");
    }

    @Test
    @DisplayName("executeCandidate with verification script gives reference solution output as expected output when candidate is wrong")
    void testExecuteCandidate_WithVerificationScript_WrongAnswerGivesReferenceOutput() {
        List<CandidateTestcaseItem> items = List.of(
                new CandidateTestcaseItem("case-1", 1, "4 9\n2 7 11 15", null, null, false, true)
        );

        JudgeCandidateExecutionRequest request = new JudgeCandidateExecutionRequest(
                CodingProblemType.GENERIC,
                pythonLanguage,
                "def solve(): pass",
                new CodeExecutionConstraints(2000L, 262144L),
                pythonLanguage,
                "print('0 1')",
                new CodeExecutionConstraints(2000L, 262144L),
                true,
                pythonLanguage,
                "assert False, 'custom verifier failed'",
                new CodeExecutionConstraints(2000L, 262144L),
                items
        );

        // 1. Reference solution execution returns "0 1"
        SimpleCodeExecutionResult refResult = SimpleCodeExecutionResult.builder()
                .executionStatus(CodeExecutionStatus.SUCCESS)
                .stdout("0 1\n")
                .stderr("")
                .exitCode(0L)
                .executionTimeMs(40L)
                .build();

        // 2. Candidate solution execution returns "99 99"
        SimpleCodeExecutionResult candResult = SimpleCodeExecutionResult.builder()
                .executionStatus(CodeExecutionStatus.SUCCESS)
                .stdout("99 99\n")
                .stderr("")
                .exitCode(0L)
                .executionTimeMs(50L)
                .build();

        // 3. Verifier execution fails (exit code 1)
        SimpleCodeExecutionResult verResult = SimpleCodeExecutionResult.builder()
                .executionStatus(CodeExecutionStatus.SUCCESS)
                .stdout("Verifier assertion log")
                .stderr("AssertionError: custom verifier failed")
                .exitCode(1L)
                .executionTimeMs(30L)
                .build();

        when(codeExecutionService.run(any(BatchCodeExecutionRequest.class)))
                .thenReturn(BatchCodeExecutionResult.builder().results(List.of(refResult)).totalExecutions(1).build())  // Reference batch
                .thenReturn(BatchCodeExecutionResult.builder().results(List.of(candResult)).totalExecutions(1).build()) // Candidate batch
                .thenReturn(BatchCodeExecutionResult.builder().results(List.of(verResult)).totalExecutions(1).build()); // Verifier batch

        CodingProblemExecutionResponse response = judgeService.executeCandidate(request);

        assertThat(response).isNotNull();
        assertThat(response.getOverallStatus()).isEqualTo("WRONG_ANSWER");
        assertThat(response.getResults()).hasSize(1);

        var result = response.getResults().get(0);
        assertThat(result.getStatus()).isEqualTo("WRONG_ANSWER");
        // User rule: actualOutput is candidate stdout
        assertThat(result.getActualOutput()).isEqualTo("99 99\n");
        // User rule: expectedOutput is reference solution output, NOT verifier output!
        assertThat(result.getExpectedOutput()).isEqualTo("0 1\n");
        assertThat(result.getExpectedOutput()).isNotEqualTo("Verifier assertion log");
    }

    @Test
    @DisplayName("executeCandidate with compilation error marks overall and all testcases as COMPILATION_ERROR")
    void testExecuteCandidate_CompilationError() {
        List<CandidateTestcaseItem> items = List.of(
                new CandidateTestcaseItem("case-1", 1, "1 2", null, "3", false, true)
        );

        JudgeCandidateExecutionRequest request = new JudgeCandidateExecutionRequest(
                CodingProblemType.GENERIC,
                javaLanguage,
                "class Solution { syntax error }",
                new CodeExecutionConstraints(2000L, 262144L),
                javaLanguage,
                "class Solution {}",
                new CodeExecutionConstraints(2000L, 262144L),
                false,
                null,
                null,
                null,
                items
        );

        SimpleCodeExecutionResult candResult = SimpleCodeExecutionResult.builder()
                .executionStatus(CodeExecutionStatus.COMPILATION_ERROR)
                .stdout("")
                .stderr("javac: syntax error")
                .exitCode(1L)
                .executionTimeMs(100L)
                .build();

        when(codeExecutionService.run(any(BatchCodeExecutionRequest.class)))
                .thenReturn(BatchCodeExecutionResult.builder().results(List.of(candResult)).totalExecutions(1).build());

        CodingProblemExecutionResponse response = judgeService.executeCandidate(request);

        assertThat(response).isNotNull();
        assertThat(response.getOverallStatus()).isEqualTo("COMPILATION_ERROR");
        assertThat(response.getResults().get(0).getStatus()).isEqualTo("COMPILATION_ERROR");
        assertThat(response.getResults().get(0).getStderr()).isEqualTo("javac: syntax error");
    }
}


