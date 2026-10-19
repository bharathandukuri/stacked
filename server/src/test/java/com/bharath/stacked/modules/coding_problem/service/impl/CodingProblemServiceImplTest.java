package com.bharath.stacked.modules.coding_problem.service.impl;

import com.bharath.stacked.exception.BadRequestException;
import com.bharath.stacked.exception.ConflictException;
import com.bharath.stacked.exception.ResourceNotFoundException;
import com.bharath.stacked.modules.coding_problem.dto.request.CreateCodingProblemRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.ExecutionConstraintRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.TestcaseRequest;
import com.bharath.stacked.modules.coding_problem.dto.response.SupportedLanguageResponse;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemDifficulty;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemStatus;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemType;
import com.bharath.stacked.modules.coding_problem.judge.CodingProblemJudgeService;
import com.bharath.stacked.modules.coding_problem.judge.CodingProblemJudgeService.InitializedTestcases;
import com.bharath.stacked.modules.coding_problem.judge.CodingProblemJudgeService.JudgeInitializationRequest;
import com.bharath.stacked.modules.coding_problem.model.CodingProblem;
import com.bharath.stacked.modules.coding_problem.model.TestCase;
import com.bharath.stacked.modules.coding_problem.repository.CodingProblemRepository;
import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import com.bharath.stacked.modules.execution.resolver.ExecutionConstraintResolver;
import com.bharath.stacked.modules.language.Language;
import com.bharath.stacked.modules.language.LanguageType;
import com.bharath.stacked.modules.language.registry.LanguageRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CodingProblemServiceImplTest {

    @Mock
    private CodingProblemRepository codingProblemRepository;

    @Mock
    private LanguageRegistry languageRegistry;

    @Mock
    private ExecutionConstraintResolver executionConstraintResolver;

    @Mock
    private CodingProblemJudgeService codingProblemJudgeService;

    @Mock
    private Language javaLanguage;

    @Mock
    private Language pythonLanguage;

    @Mock
    private Language postgresLanguage;

    @Mock
    private Language sqliteLanguage;

    @InjectMocks
    private CodingProblemServiceImpl codingProblemService;

    @BeforeEach
    void setUp() {
        lenient().when(javaLanguage.id()).thenReturn("java-21");
        lenient().when(javaLanguage.name()).thenReturn("Java 21");
        lenient().when(javaLanguage.type()).thenReturn(LanguageType.COMPILED);
        lenient().when(javaLanguage.fileExtension()).thenReturn(".java");

        lenient().when(pythonLanguage.id()).thenReturn("python-3.12");
        lenient().when(pythonLanguage.name()).thenReturn("Python 3.12");
        lenient().when(pythonLanguage.type()).thenReturn(LanguageType.INTERPRETED);
        lenient().when(pythonLanguage.fileExtension()).thenReturn(".py");

        lenient().when(postgresLanguage.id()).thenReturn("postgresql-16");
        lenient().when(postgresLanguage.name()).thenReturn("PostgreSQL 16");
        lenient().when(postgresLanguage.type()).thenReturn(LanguageType.DATABASE);
        lenient().when(postgresLanguage.fileExtension()).thenReturn(".sql");

        lenient().when(sqliteLanguage.id()).thenReturn("sqlite-3");
        lenient().when(sqliteLanguage.name()).thenReturn("SQLite 3");
        lenient().when(sqliteLanguage.type()).thenReturn(LanguageType.DATABASE);
        lenient().when(sqliteLanguage.fileExtension()).thenReturn(".sql");
    }

    private CreateCodingProblemRequest buildValidRequest() {
        CreateCodingProblemRequest request = new CreateCodingProblemRequest();
        request.setTitle("Two Sum");
        request.setSlug("two-sum");
        request.setProblemType(CodingProblemType.GENERIC);
        request.setDifficulty(CodingProblemDifficulty.EASY);
        request.setSupportedLanguageIds(List.of("java-21", "python-3.12"));
        request.setStatement("Given an array of integers, find two numbers such that they add up to a specific target.");
        request.setTopics(List.of("Array", "Hash Table"));
        request.setHints(List.of("Try using a hash map"));
        request.setStarterCode(Map.of("java-21", "class Solution {}", "python-3.12", "def twoSum(): pass"));
        request.setReferenceSolutionLanguage("java-21");
        request.setReferenceSolution("public class Solution { public static void main(String[] args) {} }");
        request.setRequireVerificationScript(false);

        TestcaseRequest sampleTc = new TestcaseRequest("4 9\n2 7 11 15", null);
        request.setSampleTestcases(List.of(sampleTc));

        TestcaseRequest hiddenTc = new TestcaseRequest("3 6\n3 2 4", null);
        request.setHiddenTestcases(List.of(hiddenTc));

        Map<String, ExecutionConstraintRequest> constraints = Map.of(
                "java-21", new ExecutionConstraintRequest(2000L, 262144L),
                "python-3.12", new ExecutionConstraintRequest(3000L, 262144L)
        );
        request.setExecutionConstraints(constraints);

        return request;
    }

    @Test
    @DisplayName("Should create generic coding problem successfully with per-language constraints and judge expected outputs")
    void shouldCreateGenericProblemSuccessfully() {
        CreateCodingProblemRequest request = buildValidRequest();

        when(codingProblemRepository.findBySlug("two-sum")).thenReturn(Optional.empty());
        when(languageRegistry.isSupported("java-21")).thenReturn(true);
        when(languageRegistry.isSupported("python-3.12")).thenReturn(true);
        when(languageRegistry.get("java-21")).thenReturn(javaLanguage);
        when(languageRegistry.get("python-3.12")).thenReturn(pythonLanguage);

        when(executionConstraintResolver.resolveTimeLimitMs(eq(javaLanguage), any(), any())).thenReturn(2000L);
        when(executionConstraintResolver.resolveMemoryLimitKb(eq(javaLanguage), any(), any())).thenReturn(262144L);
        when(executionConstraintResolver.resolveTimeLimitMs(eq(pythonLanguage), any(), any())).thenReturn(3000L);
        when(executionConstraintResolver.resolveMemoryLimitKb(eq(pythonLanguage), any(), any())).thenReturn(262144L);

        TestCase sampleTc = new TestCase("s1", "4 9\n2 7 11 15", null, "0 1");
        TestCase hiddenTc = new TestCase("h1", "3 6\n3 2 4", null, "1 2");
        when(codingProblemJudgeService.initializeExpectedOutputs(any(JudgeInitializationRequest.class)))
                .thenReturn(new InitializedTestcases(List.of(sampleTc), List.of(hiddenTc)));

        when(codingProblemRepository.save(any(CodingProblem.class))).thenAnswer(invocation -> {
            CodingProblem cp = invocation.getArgument(0);
            cp.setId("generated-mongo-id");
            return cp;
        });

        CodingProblem created = codingProblemService.create(request);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo("generated-mongo-id");
        assertThat(created.getSlug()).isEqualTo("two-sum");
        assertThat(created.getTitle()).isEqualTo("Two Sum");
        assertThat(created.getStatus()).isEqualTo(CodingProblemStatus.UNPUBLISHED);
        assertThat(created.getSampleTestCases()).hasSize(1);
        assertThat(created.getSampleTestCases().get(0).getOutput()).isEqualTo("0 1");
        assertThat(created.getHiddenTestCases()).hasSize(1);
        assertThat(created.getHiddenTestCases().get(0).getOutput()).isEqualTo("1 2");

        // Verify per-language constraints stored in model
        assertThat(created.getExecutionConstraints()).isNotNull();
        assertThat(created.getExecutionConstraints()).containsKeys("java-21", "python-3.12");
        assertThat(created.getExecutionConstraints().get("java-21").timeLimitMs()).isEqualTo(2000L);
        assertThat(created.getExecutionConstraints().get("java-21").memoryLimitKb()).isEqualTo(262144L);
        assertThat(created.getExecutionConstraints().get("python-3.12").timeLimitMs()).isEqualTo(3000L);
        assertThat(created.getExecutionConstraints().get("python-3.12").memoryLimitKb()).isEqualTo(262144L);

        ArgumentCaptor<JudgeInitializationRequest> captor = ArgumentCaptor.forClass(JudgeInitializationRequest.class);
        verify(codingProblemJudgeService).initializeExpectedOutputs(captor.capture());
        JudgeInitializationRequest judgeReq = captor.getValue();
        assertThat(judgeReq.problemType()).isEqualTo(CodingProblemType.GENERIC);
        assertThat(judgeReq.referenceLanguage()).isEqualTo(javaLanguage);
        assertThat(judgeReq.referenceConstraints().timeLimitMs()).isEqualTo(2000L);
        assertThat(judgeReq.referenceConstraints().memoryLimitKb()).isEqualTo(262144L);

        verify(codingProblemRepository).save(any(CodingProblem.class));
    }

    @Test
    @DisplayName("Should apply sensible defaults when execution constraints are omitted")
    void shouldApplySensibleDefaultsWhenExecutionConstraintsOmitted() {
        CreateCodingProblemRequest request = buildValidRequest();
        request.setExecutionConstraints(null); // Omitted -> should resolve defaults

        when(codingProblemRepository.findBySlug("two-sum")).thenReturn(Optional.empty());
        when(languageRegistry.isSupported("java-21")).thenReturn(true);
        when(languageRegistry.isSupported("python-3.12")).thenReturn(true);
        when(languageRegistry.get("java-21")).thenReturn(javaLanguage);
        when(languageRegistry.get("python-3.12")).thenReturn(pythonLanguage);

        when(executionConstraintResolver.resolveTimeLimitMs(eq(javaLanguage), isNull(), isNull())).thenReturn(2000L);
        when(executionConstraintResolver.resolveMemoryLimitKb(eq(javaLanguage), isNull(), isNull())).thenReturn(262144L);
        when(executionConstraintResolver.resolveTimeLimitMs(eq(pythonLanguage), isNull(), isNull())).thenReturn(3000L);
        when(executionConstraintResolver.resolveMemoryLimitKb(eq(pythonLanguage), isNull(), isNull())).thenReturn(262144L);

        when(codingProblemJudgeService.initializeExpectedOutputs(any(JudgeInitializationRequest.class)))
                .thenReturn(new InitializedTestcases(List.of(new TestCase()), List.of(new TestCase())));
        when(codingProblemRepository.save(any(CodingProblem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CodingProblem created = codingProblemService.create(request);

        assertThat(created.getExecutionConstraints()).containsKeys("java-21", "python-3.12");
        assertThat(created.getExecutionConstraints().get("java-21").timeLimitMs()).isEqualTo(2000L);
        assertThat(created.getExecutionConstraints().get("python-3.12").timeLimitMs()).isEqualTo(3000L);
    }

    @Test
    @DisplayName("Should create database coding problem successfully with database language constraints")
    void shouldCreateDatabaseProblemSuccessfully() {
        CreateCodingProblemRequest request = new CreateCodingProblemRequest();
        request.setTitle("Customers With Orders");
        request.setSlug("customers-with-orders");
        request.setProblemType(CodingProblemType.DATABASE);
        request.setDifficulty(CodingProblemDifficulty.MEDIUM);
        request.setSupportedLanguageIds(List.of("postgresql-16"));
        request.setStatement("Find all customers who have placed at least one order.");
        request.setTopics(List.of("SQL", "Join"));
        request.setReferenceSolutionLanguage("postgresql-16");
        request.setReferenceSolution("SELECT name FROM customers;");
        request.setSampleTestcases(List.of(new TestcaseRequest("", "CREATE TABLE customers (id INT, name TEXT);")));
        request.setHiddenTestcases(List.of(new TestcaseRequest("", "CREATE TABLE customers (id INT, name TEXT);")));

        when(codingProblemRepository.findBySlug("customers-with-orders")).thenReturn(Optional.empty());
        when(languageRegistry.isSupported("postgresql-16")).thenReturn(true);
        when(languageRegistry.get("postgresql-16")).thenReturn(postgresLanguage);

        when(executionConstraintResolver.resolveTimeLimitMs(eq(postgresLanguage), any(), any())).thenReturn(5000L);
        when(executionConstraintResolver.resolveMemoryLimitKb(eq(postgresLanguage), any(), any())).thenReturn(524288L);

        when(codingProblemJudgeService.initializeExpectedOutputs(any(JudgeInitializationRequest.class)))
                .thenReturn(new InitializedTestcases(
                        List.of(new TestCase("s1", "", "CREATE TABLE ...", "Alice")),
                        List.of(new TestCase("h1", "", "CREATE TABLE ...", "Bob"))
                ));
        when(codingProblemRepository.save(any(CodingProblem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CodingProblem created = codingProblemService.create(request);

        assertThat(created).isNotNull();
        assertThat(created.getExecutionConstraints().get("postgresql-16").timeLimitMs()).isEqualTo(5000L);
        assertThat(created.getExecutionConstraints().get("postgresql-16").memoryLimitKb()).isEqualTo(524288L);
    }

    @Test
    @DisplayName("Should create problem with verification script and resolve its constraints")
    void shouldCreateProblemWithVerificationScriptSuccessfully() {
        CreateCodingProblemRequest request = buildValidRequest();
        request.setRequireVerificationScript(true);
        request.setVerificationScriptLanguage("python-3.12");
        request.setVerificationScript("import sys; print('ok')");

        when(codingProblemRepository.findBySlug("two-sum")).thenReturn(Optional.empty());
        when(languageRegistry.isSupported("java-21")).thenReturn(true);
        when(languageRegistry.isSupported("python-3.12")).thenReturn(true);
        when(languageRegistry.get("java-21")).thenReturn(javaLanguage);
        when(languageRegistry.get("python-3.12")).thenReturn(pythonLanguage);

        when(executionConstraintResolver.resolveTimeLimitMs(any(), any(), any())).thenReturn(2000L);
        when(executionConstraintResolver.resolveMemoryLimitKb(any(), any(), any())).thenReturn(262144L);

        when(codingProblemJudgeService.initializeExpectedOutputs(any(JudgeInitializationRequest.class)))
                .thenReturn(new InitializedTestcases(List.of(new TestCase()), List.of(new TestCase())));
        when(codingProblemRepository.save(any(CodingProblem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CodingProblem created = codingProblemService.create(request);

        assertThat(created.isRequireVerificationScript()).isTrue();
        assertThat(created.getVerificationScriptLanguage()).isEqualTo("python-3.12");
        assertThat(created.getVerificationScript()).isEqualTo("import sys; print('ok')");

        ArgumentCaptor<JudgeInitializationRequest> captor = ArgumentCaptor.forClass(JudgeInitializationRequest.class);
        verify(codingProblemJudgeService).initializeExpectedOutputs(captor.capture());
        JudgeInitializationRequest captured = captor.getValue();
        assertThat(captured.requireVerificationScript()).isTrue();
        assertThat(captured.verificationScriptLanguage()).isEqualTo(pythonLanguage);
    }

    @Test
    @DisplayName("Should throw ConflictException when slug already exists")
    void shouldThrowConflictWhenSlugExists() {
        CreateCodingProblemRequest request = buildValidRequest();
        when(codingProblemRepository.findBySlug("two-sum")).thenReturn(Optional.of(new CodingProblem()));

        assertThatThrownBy(() -> codingProblemService.create(request))
                .isInstanceOf(ConflictException.class);
        verify(codingProblemRepository, never()).save(any());
        verifyNoInteractions(codingProblemJudgeService);
    }

    @Test
    @DisplayName("Should throw BadRequestException when supported language is not registered")
    void shouldThrowBadRequestWhenLanguageUnsupported() {
        CreateCodingProblemRequest request = buildValidRequest();
        when(codingProblemRepository.findBySlug("two-sum")).thenReturn(Optional.empty());
        when(languageRegistry.isSupported("java-21")).thenReturn(false);

        assertThatThrownBy(() -> codingProblemService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("is not supported");
    }

    @Test
    @DisplayName("Should throw BadRequestException when language type mismatches problem type")
    void shouldThrowBadRequestWhenLanguageTypeMismatchesProblemType() {
        CreateCodingProblemRequest request = buildValidRequest();
        request.setSupportedLanguageIds(List.of("postgresql-16"));
        request.setReferenceSolutionLanguage("postgresql-16");

        when(codingProblemRepository.findBySlug("two-sum")).thenReturn(Optional.empty());
        when(languageRegistry.isSupported("postgresql-16")).thenReturn(true);
        when(languageRegistry.get("postgresql-16")).thenReturn(postgresLanguage);

        assertThatThrownBy(() -> codingProblemService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Database language");
    }

    @Test
    @DisplayName("Should throw BadRequestException when reference solution language is not in supportedLanguageIds")
    void shouldThrowBadRequestWhenRefSolutionLanguageNotInSupported() {
        CreateCodingProblemRequest request = buildValidRequest();
        request.setReferenceSolutionLanguage("python-3.12");
        request.setSupportedLanguageIds(List.of("java-21"));

        when(codingProblemRepository.findBySlug("two-sum")).thenReturn(Optional.empty());
        when(languageRegistry.isSupported("java-21")).thenReturn(true);
        when(languageRegistry.get("java-21")).thenReturn(javaLanguage);

        assertThatThrownBy(() -> codingProblemService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must be included in supportedLanguageIds");
    }

    @Test
    @DisplayName("Should throw BadRequestException when starter code has unsupported language")
    void shouldThrowBadRequestWhenStarterCodeHasUnsupportedLanguage() {
        CreateCodingProblemRequest request = buildValidRequest();
        request.setStarterCode(Map.of("rust-1.75", "fn main() {}"));

        when(codingProblemRepository.findBySlug("two-sum")).thenReturn(Optional.empty());
        when(languageRegistry.isSupported("java-21")).thenReturn(true);
        when(languageRegistry.isSupported("python-3.12")).thenReturn(true);
        when(languageRegistry.get("java-21")).thenReturn(javaLanguage);
        when(languageRegistry.get("python-3.12")).thenReturn(pythonLanguage);

        assertThatThrownBy(() -> codingProblemService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Starter code provided for unsupported language");
    }

    @Test
    @DisplayName("Should throw BadRequestException when verification script is required but script is missing")
    void shouldThrowBadRequestWhenVerificationScriptMissing() {
        CreateCodingProblemRequest request = buildValidRequest();
        request.setRequireVerificationScript(true);
        request.setVerificationScriptLanguage("python-3.12");
        request.setVerificationScript("");

        when(codingProblemRepository.findBySlug("two-sum")).thenReturn(Optional.empty());
        when(languageRegistry.isSupported("java-21")).thenReturn(true);
        when(languageRegistry.isSupported("python-3.12")).thenReturn(true);
        when(languageRegistry.get("java-21")).thenReturn(javaLanguage);
        when(languageRegistry.get("python-3.12")).thenReturn(pythonLanguage);

        assertThatThrownBy(() -> codingProblemService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Verification script is required");
    }

    @Test
    @DisplayName("Should throw BadRequestException when execution constraints specify unsupported language")
    void shouldThrowBadRequestWhenExecutionConstraintsSpecifyUnsupportedLanguage() {
        CreateCodingProblemRequest request = buildValidRequest();
        request.setExecutionConstraints(Map.of("unsupported-lang", new ExecutionConstraintRequest(1000L, 1000L)));

        when(codingProblemRepository.findBySlug("two-sum")).thenReturn(Optional.empty());
        when(languageRegistry.isSupported("java-21")).thenReturn(true);
        when(languageRegistry.isSupported("python-3.12")).thenReturn(true);
        when(languageRegistry.isSupported("unsupported-lang")).thenReturn(false);
        when(languageRegistry.get("java-21")).thenReturn(javaLanguage);
        when(languageRegistry.get("python-3.12")).thenReturn(pythonLanguage);

        assertThatThrownBy(() -> codingProblemService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Execution constraints provided for unsupported language");
    }

    @Test
    @DisplayName("Should throw BadRequestException when execution constraints specify language not in problem supported languages")
    void shouldThrowBadRequestWhenExecutionConstraintsSpecifyLanguageNotInProblem() {
        CreateCodingProblemRequest request = buildValidRequest();
        request.setSupportedLanguageIds(List.of("java-21"));
        request.setStarterCode(Map.of("java-21", "class Solution {}"));
        // python-3.12 is supported by platform but NOT in problem's supportedLanguageIds
        request.setExecutionConstraints(Map.of("python-3.12", new ExecutionConstraintRequest(1000L, 1000L)));

        when(codingProblemRepository.findBySlug("two-sum")).thenReturn(Optional.empty());
        when(languageRegistry.isSupported("java-21")).thenReturn(true);
        when(languageRegistry.isSupported("python-3.12")).thenReturn(true);
        when(languageRegistry.get("java-21")).thenReturn(javaLanguage);

        assertThatThrownBy(() -> codingProblemService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not in supportedLanguageIds");
    }

    @Test
    @DisplayName("Should propagate BadRequestException and not save problem when judge service fails")
    void shouldPropagateBadRequestExceptionWhenJudgeFails() {
        CreateCodingProblemRequest request = buildValidRequest();

        when(codingProblemRepository.findBySlug("two-sum")).thenReturn(Optional.empty());
        when(languageRegistry.isSupported("java-21")).thenReturn(true);
        when(languageRegistry.isSupported("python-3.12")).thenReturn(true);
        when(languageRegistry.get("java-21")).thenReturn(javaLanguage);
        when(languageRegistry.get("python-3.12")).thenReturn(pythonLanguage);

        when(executionConstraintResolver.resolveTimeLimitMs(any(), any(), any())).thenReturn(2000L);
        when(executionConstraintResolver.resolveMemoryLimitKb(any(), any(), any())).thenReturn(262144L);

        when(codingProblemJudgeService.initializeExpectedOutputs(any(JudgeInitializationRequest.class)))
                .thenThrow(new BadRequestException("Reference solution failed on testcase 1: Runtime error"));

        assertThatThrownBy(() -> codingProblemService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Reference solution failed on testcase 1");

        verify(codingProblemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BadRequestException when database problem has more than one supported language")
    void shouldThrowBadRequestWhenDatabaseProblemHasMultipleSupportedLanguages() {
        CreateCodingProblemRequest request = new CreateCodingProblemRequest();
        request.setTitle("DB Multiple Languages");
        request.setSlug("db-multiple-languages");
        request.setProblemType(CodingProblemType.DATABASE);
        request.setDifficulty(CodingProblemDifficulty.EASY);
        request.setSupportedLanguageIds(List.of("sqlite-3", "postgresql-16"));
        request.setStatement("Query data.");
        request.setTopics(List.of("SQL"));
        request.setReferenceSolutionLanguage("sqlite-3");
        request.setReferenceSolution("SELECT 1;");
        request.setSampleTestcases(List.of(new TestcaseRequest("", "CREATE TABLE t (x INT);")));
        request.setHiddenTestcases(List.of(new TestcaseRequest("", "CREATE TABLE t (x INT);")));

        when(codingProblemRepository.findBySlug("db-multiple-languages")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> codingProblemService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("A DATABASE problem must support exactly one database language");
    }

    @Test
    @DisplayName("Should throw BadRequestException when database problem verification script language does not match reference language")
    void shouldThrowBadRequestWhenDatabaseProblemVerificationScriptLanguageMismatchesReferenceLanguage() {
        CreateCodingProblemRequest request = new CreateCodingProblemRequest();
        request.setTitle("DB Verification Mismatch");
        request.setSlug("db-verification-mismatch");
        request.setProblemType(CodingProblemType.DATABASE);
        request.setDifficulty(CodingProblemDifficulty.EASY);
        request.setSupportedLanguageIds(List.of("sqlite-3"));
        request.setStatement("Query data.");
        request.setTopics(List.of("SQL"));
        request.setReferenceSolutionLanguage("sqlite-3");
        request.setReferenceSolution("SELECT 1;");
        request.setRequireVerificationScript(true);
        request.setVerificationScriptLanguage("postgresql-16");
        request.setVerificationScript("SELECT 1;");
        request.setSampleTestcases(List.of(new TestcaseRequest("", "CREATE TABLE t (x INT);")));
        request.setHiddenTestcases(List.of(new TestcaseRequest("", "CREATE TABLE t (x INT);")));

        when(codingProblemRepository.findBySlug("db-verification-mismatch")).thenReturn(Optional.empty());
        when(languageRegistry.isSupported("sqlite-3")).thenReturn(true);
        when(languageRegistry.get("sqlite-3")).thenReturn(sqliteLanguage);
        when(languageRegistry.isSupported("postgresql-16")).thenReturn(true);
        when(languageRegistry.get("postgresql-16")).thenReturn(postgresLanguage);

        assertThatThrownBy(() -> codingProblemService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must match reference solution language 'sqlite-3'");
    }

    @Test
    @DisplayName("Should throw BadRequestException when database problem verification script is not a database language")
    void shouldThrowBadRequestWhenDatabaseProblemVerificationScriptNotDatabaseLanguage() {
        CreateCodingProblemRequest request = new CreateCodingProblemRequest();
        request.setTitle("DB Verification NonDB");
        request.setSlug("db-verification-nondb");
        request.setProblemType(CodingProblemType.DATABASE);
        request.setDifficulty(CodingProblemDifficulty.EASY);
        request.setSupportedLanguageIds(List.of("sqlite-3"));
        request.setStatement("Query data.");
        request.setTopics(List.of("SQL"));
        request.setReferenceSolutionLanguage("sqlite-3");
        request.setReferenceSolution("SELECT 1;");
        request.setRequireVerificationScript(true);
        request.setVerificationScriptLanguage("python-3.12");
        request.setVerificationScript("print(1)");
        request.setSampleTestcases(List.of(new TestcaseRequest("", "CREATE TABLE t (x INT);")));
        request.setHiddenTestcases(List.of(new TestcaseRequest("", "CREATE TABLE t (x INT);")));

        when(codingProblemRepository.findBySlug("db-verification-nondb")).thenReturn(Optional.empty());
        when(languageRegistry.isSupported("sqlite-3")).thenReturn(true);
        when(languageRegistry.get("sqlite-3")).thenReturn(sqliteLanguage);
        when(languageRegistry.isSupported("python-3.12")).thenReturn(true);
        when(languageRegistry.get("python-3.12")).thenReturn(pythonLanguage);

        assertThatThrownBy(() -> codingProblemService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Verification script for a DATABASE problem must be a database language");
    }

    @Test
    @DisplayName("isSlugAvailable should return false for null, blank, or existing slugs and true for unique slugs")
    void shouldCheckSlugAvailabilityCorrectly() {
        assertThat(codingProblemService.isSlugAvailable(null)).isFalse();
        assertThat(codingProblemService.isSlugAvailable("")).isFalse();
        assertThat(codingProblemService.isSlugAvailable("   ")).isFalse();

        when(codingProblemRepository.existsBySlug("existing-slug")).thenReturn(true);
        when(codingProblemRepository.existsBySlug("new-slug")).thenReturn(false);

        assertThat(codingProblemService.isSlugAvailable("existing-slug")).isFalse();
        assertThat(codingProblemService.isSlugAvailable("new-slug")).isTrue();
    }

    @Test
    @DisplayName("publish should set status to PUBLISHED and save when problem found by id")
    void shouldPublishProblemSuccessfullyById() {
        CodingProblem existing = CodingProblem.builder()
                .id("prob-123")
                .slug("two-sum")
                .status(CodingProblemStatus.UNPUBLISHED)
                .build();

        when(codingProblemRepository.findById("prob-123")).thenReturn(Optional.of(existing));
        when(codingProblemRepository.save(any(CodingProblem.class))).thenAnswer(inv -> inv.getArgument(0));

        CodingProblem published = codingProblemService.publish("prob-123");

        assertThat(published.getStatus()).isEqualTo(CodingProblemStatus.PUBLISHED);
        verify(codingProblemRepository).save(existing);
    }

    @Test
    @DisplayName("publish should set status to PUBLISHED when problem found by slug")
    void shouldPublishProblemSuccessfullyBySlug() {
        CodingProblem existing = CodingProblem.builder()
                .id("prob-123")
                .slug("two-sum")
                .status(CodingProblemStatus.UNPUBLISHED)
                .build();

        when(codingProblemRepository.findById("two-sum")).thenReturn(Optional.empty());
        when(codingProblemRepository.findBySlug("two-sum")).thenReturn(Optional.of(existing));
        when(codingProblemRepository.save(any(CodingProblem.class))).thenAnswer(inv -> inv.getArgument(0));

        CodingProblem published = codingProblemService.publish("two-sum");

        assertThat(published.getStatus()).isEqualTo(CodingProblemStatus.PUBLISHED);
        verify(codingProblemRepository).save(existing);
    }

    @Test
    @DisplayName("unpublish should set status to UNPUBLISHED and save")
    void shouldUnpublishProblemSuccessfully() {
        CodingProblem existing = CodingProblem.builder()
                .id("prob-123")
                .slug("two-sum")
                .status(CodingProblemStatus.PUBLISHED)
                .build();

        when(codingProblemRepository.findById("prob-123")).thenReturn(Optional.of(existing));
        when(codingProblemRepository.save(any(CodingProblem.class))).thenAnswer(inv -> inv.getArgument(0));

        CodingProblem unpublished = codingProblemService.unpublish("prob-123");

        assertThat(unpublished.getStatus()).isEqualTo(CodingProblemStatus.UNPUBLISHED);
        verify(codingProblemRepository).save(existing);
    }

    @Test
    @DisplayName("updateStatus should throw ResourceNotFoundException when problem does not exist by id or slug")
    void shouldThrowResourceNotFoundExceptionWhenProblemNotFound() {
        when(codingProblemRepository.findById("unknown")).thenReturn(Optional.empty());
        when(codingProblemRepository.findBySlug("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> codingProblemService.updateStatus("unknown", CodingProblemStatus.PUBLISHED))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("CodingProblem not found with id or slug: 'unknown'");
    }

    @Test
    @DisplayName("updateStatus should throw BadRequestException when identifier is blank or status is null")
    void shouldThrowBadRequestWhenIdentifierOrStatusInvalid() {
        assertThatThrownBy(() -> codingProblemService.updateStatus("", CodingProblemStatus.PUBLISHED))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Coding problem identifier cannot be empty");

        assertThatThrownBy(() -> codingProblemService.updateStatus("prob-123", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Status cannot be null");
    }

    @Test
    @DisplayName("getSupportedLanguages should return all supported languages sorted by name")
    void shouldReturnAllSupportedLanguagesSortedByName() {
        when(languageRegistry.getAll()).thenReturn(List.of(sqliteLanguage, javaLanguage, pythonLanguage));
        when(languageRegistry.resolveReferenceSolutionFileName(javaLanguage)).thenReturn("Solution.java");
        when(languageRegistry.resolveReferenceSolutionFileName(pythonLanguage)).thenReturn("solution.py");
        when(languageRegistry.resolveReferenceSolutionFileName(sqliteLanguage)).thenReturn("solution.sql");

        when(executionConstraintResolver.resolveTimeLimitMs(eq(javaLanguage), any(), any())).thenReturn(2000L);
        when(executionConstraintResolver.resolveMemoryLimitKb(eq(javaLanguage), any(), any())).thenReturn(262144L);
        when(executionConstraintResolver.resolveTimeLimitMs(eq(pythonLanguage), any(), any())).thenReturn(3000L);
        when(executionConstraintResolver.resolveMemoryLimitKb(eq(pythonLanguage), any(), any())).thenReturn(131072L);
        when(executionConstraintResolver.resolveTimeLimitMs(eq(sqliteLanguage), any(), any())).thenReturn(1000L);
        when(executionConstraintResolver.resolveMemoryLimitKb(eq(sqliteLanguage), any(), any())).thenReturn(65536L);

        List<SupportedLanguageResponse> responses = codingProblemService.getSupportedLanguages();

        assertThat(responses).hasSize(3);
        assertThat(responses.get(0).id()).isEqualTo("java-21");
        assertThat(responses.get(0).name()).isEqualTo("Java 21");
        assertThat(responses.get(0).type()).isEqualTo(LanguageType.COMPILED);
        assertThat(responses.get(0).fileExtension()).isEqualTo(".java");
        assertThat(responses.get(0).defaultFileName()).isEqualTo("Solution.java");
        assertThat(responses.get(0).defaultTimeLimitMs()).isEqualTo(2000L);
        assertThat(responses.get(0).defaultMemoryLimitKb()).isEqualTo(262144L);

        assertThat(responses.get(1).id()).isEqualTo("python-3.12");
        assertThat(responses.get(2).id()).isEqualTo("sqlite-3");
    }

    @Test
    @DisplayName("getSupportedLanguages should filter for GENERIC problems")
    void shouldFilterSupportedLanguagesByGeneric() {
        when(languageRegistry.getAll()).thenReturn(List.of(sqliteLanguage, javaLanguage, pythonLanguage));
        when(languageRegistry.resolveReferenceSolutionFileName(javaLanguage)).thenReturn("Solution.java");
        when(languageRegistry.resolveReferenceSolutionFileName(pythonLanguage)).thenReturn("solution.py");

        when(executionConstraintResolver.resolveTimeLimitMs(eq(javaLanguage), any(), any())).thenReturn(2000L);
        when(executionConstraintResolver.resolveMemoryLimitKb(eq(javaLanguage), any(), any())).thenReturn(262144L);
        when(executionConstraintResolver.resolveTimeLimitMs(eq(pythonLanguage), any(), any())).thenReturn(3000L);
        when(executionConstraintResolver.resolveMemoryLimitKb(eq(pythonLanguage), any(), any())).thenReturn(131072L);

        List<SupportedLanguageResponse> responses = codingProblemService.getSupportedLanguages(CodingProblemType.GENERIC);

        assertThat(responses).hasSize(2);
        assertThat(responses.stream().map(SupportedLanguageResponse::id)).containsExactly("java-21", "python-3.12");
    }

    @Test
    @DisplayName("getSupportedLanguages should filter for DATABASE problems")
    void shouldFilterSupportedLanguagesByDatabase() {
        when(languageRegistry.getAll()).thenReturn(List.of(sqliteLanguage, javaLanguage, pythonLanguage));
        when(languageRegistry.resolveReferenceSolutionFileName(sqliteLanguage)).thenReturn("solution.sql");

        when(executionConstraintResolver.resolveTimeLimitMs(eq(sqliteLanguage), any(), any())).thenReturn(1000L);
        when(executionConstraintResolver.resolveMemoryLimitKb(eq(sqliteLanguage), any(), any())).thenReturn(65536L);

        List<SupportedLanguageResponse> responses = codingProblemService.getSupportedLanguages(CodingProblemType.DATABASE);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo("sqlite-3");
        assertThat(responses.get(0).type()).isEqualTo(LanguageType.DATABASE);
    }
}
