package com.bharath.stacked.modules.coding.service.impl;

import com.bharath.stacked.exception.BadRequestException;
import com.bharath.stacked.exception.ConflictException;
import com.bharath.stacked.modules.coding.dto.request.CreateCodingProblemRequest;
import com.bharath.stacked.modules.coding.dto.request.ExecutionLimitsRequest;
import com.bharath.stacked.modules.coding.dto.request.TestcaseRequest;
import com.bharath.stacked.modules.coding.enums.CodingProblemDifficulty;
import com.bharath.stacked.modules.coding.enums.CodingProblemType;
import com.bharath.stacked.modules.coding.model.CodingProblem;
import com.bharath.stacked.modules.coding.repository.CodingProblemRepository;
import com.bharath.stacked.modules.execution.dto.request.SimpleCodeExecutionRequest;
import com.bharath.stacked.modules.execution.dto.response.SimpleCodeExecutionResult;
import com.bharath.stacked.modules.execution.enums.CodeExecutionStatus;
import com.bharath.stacked.modules.execution.service.CodeExecutionService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CodingProblemServiceImplTest {

    @Mock
    private CodingProblemRepository codingProblemRepository;

    @Mock
    private LanguageRegistry languageRegistry;

    @Mock
    private CodeExecutionService codeExecutionService;

    @Mock
    private Language javaLanguage;

    @Mock
    private Language pythonLanguage;

    @Mock
    private Language postgresLanguage;

    @InjectMocks
    private CodingProblemServiceImpl codingProblemService;

    @BeforeEach
    void setUp() {
        lenient().when(javaLanguage.id()).thenReturn("java-21");
        lenient().when(javaLanguage.type()).thenReturn(LanguageType.COMPILED);

        lenient().when(pythonLanguage.id()).thenReturn("python-3.12");
        lenient().when(pythonLanguage.type()).thenReturn(LanguageType.INTERPRETED);

        lenient().when(postgresLanguage.id()).thenReturn("postgres-16");
        lenient().when(postgresLanguage.type()).thenReturn(LanguageType.DATABASE);
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
        request.setReferenceSolution("public class Solution { public static void main(String[] args) { System.out.println(\"0 1\"); } }");
        request.setRequireVerificationScript(false);

        TestcaseRequest sampleTc = new TestcaseRequest("4 9\n2 7 11 15", null);
        request.setSampleTestcases(List.of(sampleTc));

        TestcaseRequest hiddenTc = new TestcaseRequest("3 6\n3 2 4", null);
        request.setHiddenTestcases(List.of(hiddenTc));

        ExecutionLimitsRequest limits = ExecutionLimitsRequest.builder()
                .timeLimitMs(2000L)
                .memoryLimitKb(262144L)
                .outputLimitBytes(10485760L)
                .queryTimeoutMs(10000L)
                .build();
        request.setExecutionLimits(limits);

        return request;
    }

    @Test
    @DisplayName("Should create generic coding problem successfully and generate expected outputs")
    void shouldCreateGenericProblemSuccessfully() {
        CreateCodingProblemRequest request = buildValidRequest();

        when(codingProblemRepository.findBySlug("two-sum")).thenReturn(Optional.empty());
        when(languageRegistry.isSupported("java-21")).thenReturn(true);
        when(languageRegistry.isSupported("python-3.12")).thenReturn(true);
        when(languageRegistry.get("java-21")).thenReturn(javaLanguage);
        when(languageRegistry.get("python-3.12")).thenReturn(pythonLanguage);

        SimpleCodeExecutionResult execResult = SimpleCodeExecutionResult.builder()
                .stdout("0 1\n")
                .stderr("")
                .exitCode(0L)
                .executionStatus(CodeExecutionStatus.SUCCESS)
                .logs(List.of("Execution OK"))
                .build();
        when(codeExecutionService.run(any(SimpleCodeExecutionRequest.class))).thenReturn(execResult);

        when(codingProblemRepository.save(any(CodingProblem.class))).thenAnswer(invocation -> {
            CodingProblem cp = invocation.getArgument(0);
            cp.setId("generated-mongo-id");
            return cp;
        });

        CodingProblem created = codingProblemService.create(request);

        assertNotNull(created);
        assertEquals("two-sum", created.getSlug());
        assertEquals("Two Sum", created.getTitle());
        assertEquals("generated-mongo-id", created.getId());
        assertEquals(1, created.getSampleTestCases().size());
        assertEquals("0 1", created.getSampleTestCases().getFirst().getOutput());
        assertEquals(1, created.getHiddenTestCases().size());
        assertEquals("0 1", created.getHiddenTestCases().getFirst().getOutput());
        assertEquals(262144L, created.getExecutionLimits().getMemoryLimitKb());
        assertEquals(2000L, created.getExecutionLimits().getTimeLimitMs());

        verify(codeExecutionService, times(2)).run(any(SimpleCodeExecutionRequest.class));
        verify(codingProblemRepository).save(any(CodingProblem.class));
    }

    @Test
    @DisplayName("Should create database coding problem successfully with schema init prepended")
    void shouldCreateDatabaseProblemSuccessfully() {
        CreateCodingProblemRequest request = new CreateCodingProblemRequest();
        request.setTitle("Customers With Orders");
        request.setSlug("customers-with-orders");
        request.setProblemType(CodingProblemType.DATABASE);
        request.setDifficulty(CodingProblemDifficulty.MEDIUM);
        request.setSupportedLanguageIds(List.of("postgres-16"));
        request.setStatement("Find all customers who have placed at least one order.");
        request.setTopics(List.of("SQL", "Join"));
        request.setReferenceSolutionLanguage("postgres-16");
        request.setReferenceSolution("SELECT name FROM customers;");
        request.setSampleTestcases(List.of(new TestcaseRequest("", "CREATE TABLE customers (id INT, name TEXT); INSERT INTO customers VALUES (1, 'Alice');")));
        request.setHiddenTestcases(List.of(new TestcaseRequest("", "CREATE TABLE customers (id INT, name TEXT); INSERT INTO customers VALUES (2, 'Bob');")));

        when(codingProblemRepository.findBySlug("customers-with-orders")).thenReturn(Optional.empty());
        when(languageRegistry.isSupported("postgres-16")).thenReturn(true);
        when(languageRegistry.get("postgres-16")).thenReturn(postgresLanguage);

        SimpleCodeExecutionResult execResult = SimpleCodeExecutionResult.builder()
                .stdout("Alice\n")
                .stderr("")
                .exitCode(0L)
                .executionStatus(CodeExecutionStatus.SUCCESS)
                .logs(List.of("DB OK"))
                .build();
        when(codeExecutionService.run(any(SimpleCodeExecutionRequest.class))).thenReturn(execResult);

        when(codingProblemRepository.save(any(CodingProblem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CodingProblem created = codingProblemService.create(request);

        assertNotNull(created);
        assertEquals("customers-with-orders", created.getSlug());
        assertEquals("Alice", created.getSampleTestCases().getFirst().getOutput());

        ArgumentCaptor<SimpleCodeExecutionRequest> captor = ArgumentCaptor.forClass(SimpleCodeExecutionRequest.class);
        verify(codeExecutionService, times(2)).run(captor.capture());
        assertTrue(captor.getAllValues().getFirst().getCode().contains("CREATE TABLE customers"));
        assertTrue(captor.getAllValues().getFirst().getCode().contains("SELECT name FROM customers;"));
    }

    @Test
    @DisplayName("Should throw ConflictException when slug already exists")
    void shouldThrowConflictWhenSlugExists() {
        CreateCodingProblemRequest request = buildValidRequest();
        when(codingProblemRepository.findBySlug("two-sum")).thenReturn(Optional.of(new CodingProblem()));

        assertThrows(ConflictException.class, () -> codingProblemService.create(request));
        verify(codingProblemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BadRequestException when supported language is not registered")
    void shouldThrowBadRequestWhenLanguageUnsupported() {
        CreateCodingProblemRequest request = buildValidRequest();
        when(codingProblemRepository.findBySlug("two-sum")).thenReturn(Optional.empty());
        when(languageRegistry.isSupported("java-21")).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> codingProblemService.create(request));
        assertTrue(ex.getMessage().contains("is not supported"));
    }

    @Test
    @DisplayName("Should throw BadRequestException when language type mismatches problem type")
    void shouldThrowBadRequestWhenLanguageTypeMismatchesProblemType() {
        CreateCodingProblemRequest request = buildValidRequest();
        request.setSupportedLanguageIds(List.of("postgres-16"));
        request.setReferenceSolutionLanguage("postgres-16");

        when(codingProblemRepository.findBySlug("two-sum")).thenReturn(Optional.empty());
        when(languageRegistry.isSupported("postgres-16")).thenReturn(true);
        when(languageRegistry.get("postgres-16")).thenReturn(postgresLanguage);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> codingProblemService.create(request));
        assertTrue(ex.getMessage().contains("Database language"));
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

        BadRequestException ex = assertThrows(BadRequestException.class, () -> codingProblemService.create(request));
        assertTrue(ex.getMessage().contains("must be included in supportedLanguageIds"));
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

        BadRequestException ex = assertThrows(BadRequestException.class, () -> codingProblemService.create(request));
        assertTrue(ex.getMessage().contains("Starter code provided for unsupported language"));
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

        BadRequestException ex = assertThrows(BadRequestException.class, () -> codingProblemService.create(request));
        assertTrue(ex.getMessage().contains("Verification script is required"));
    }

    @Test
    @DisplayName("Should throw BadRequestException when reference solution execution fails on a testcase")
    void shouldThrowBadRequestWhenReferenceSolutionFails() {
        CreateCodingProblemRequest request = buildValidRequest();

        when(codingProblemRepository.findBySlug("two-sum")).thenReturn(Optional.empty());
        when(languageRegistry.isSupported("java-21")).thenReturn(true);
        when(languageRegistry.isSupported("python-3.12")).thenReturn(true);
        when(languageRegistry.get("java-21")).thenReturn(javaLanguage);
        when(languageRegistry.get("python-3.12")).thenReturn(pythonLanguage);

        SimpleCodeExecutionResult failResult = SimpleCodeExecutionResult.builder()
                .stdout("")
                .stderr("NullPointerException")
                .exitCode(1L)
                .executionStatus(CodeExecutionStatus.RUNTIME_ERROR)
                .logs(List.of("Error"))
                .build();
        when(codeExecutionService.run(any(SimpleCodeExecutionRequest.class))).thenReturn(failResult);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> codingProblemService.create(request));
        assertTrue(ex.getMessage().contains("Reference solution failed execution"));
        verify(codingProblemRepository, never()).save(any());
    }
}

