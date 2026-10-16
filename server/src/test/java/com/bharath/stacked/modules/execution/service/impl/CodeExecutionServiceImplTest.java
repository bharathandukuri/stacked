package com.bharath.stacked.modules.execution.service.impl;

import com.bharath.stacked.modules.execution.dto.request.BatchCodeExecutionRequest;
import com.bharath.stacked.modules.execution.dto.request.BatchExecutionItem;
import com.bharath.stacked.modules.execution.dto.request.SimpleCodeExecutionRequest;
import com.bharath.stacked.modules.execution.dto.response.BatchCodeExecutionResult;
import com.bharath.stacked.modules.execution.dto.response.SimpleCodeExecutionResult;
import java.util.Map;
import com.bharath.stacked.modules.execution.enums.CodeExecutionStatus;
import com.bharath.stacked.modules.execution.enums.IsolateExecutionStatus;
import com.bharath.stacked.modules.execution.exception.DockerContainerCreationException;
import com.bharath.stacked.modules.execution.exception.DockerContainerDeletionException;
import com.bharath.stacked.modules.execution.exception.DockerContainerNotFoundException;
import com.bharath.stacked.modules.execution.exception.DockerContainerStopException;
import com.bharath.stacked.modules.execution.exception.DockerExecutionException;
import com.bharath.stacked.modules.execution.exception.IsolateCleanupException;
import com.bharath.stacked.modules.execution.exception.IsolateExecutionException;
import com.bharath.stacked.modules.execution.exception.IsolateInitializationException;
import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import com.bharath.stacked.modules.execution.dto.DockerContainerConstraints;
import com.bharath.stacked.modules.execution.dto.DockerContainerDetails;
import com.bharath.stacked.modules.execution.dto.response.DockerExecutionResult;
import com.bharath.stacked.modules.execution.dto.DockerImageDetails;
import com.bharath.stacked.modules.execution.dto.IsolateExecutionConstraints;
import com.bharath.stacked.modules.execution.dto.response.IsolateExecutionResult;
import com.bharath.stacked.modules.execution.dto.IsolateSandBoxDetails;
import com.bharath.stacked.modules.execution.mapper.CodeExecutionStatusMapper;
import com.bharath.stacked.modules.execution.service.DockerExecutionService;
import com.bharath.stacked.modules.execution.service.IsolateExecutionService;
import com.bharath.stacked.modules.language.Language;
import com.bharath.stacked.modules.language.factory.impl.LanguageFactoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CodeExecutionServiceImpl Unit Tests")
class CodeExecutionServiceImplTest {

    @Mock
    private DockerExecutionService dockerExecutionService;

    @Mock
    private IsolateExecutionService isolateExecutionService;

    @Spy
    private CodeExecutionStatusMapper codeExecutionStatusMapper = new CodeExecutionStatusMapper();

    @Spy
    private com.bharath.stacked.modules.execution.config.ExecutionProperties executionProperties =
            new com.bharath.stacked.modules.execution.config.ExecutionProperties();

    @Spy
    private com.bharath.stacked.modules.execution.validator.ExecutionPathValidator executionPathValidator =
            new com.bharath.stacked.modules.execution.validator.ExecutionPathValidator(executionProperties);

    @Spy
    private com.bharath.stacked.modules.execution.resolver.ExecutionConstraintResolver executionConstraintResolver =
            new com.bharath.stacked.modules.execution.resolver.ExecutionConstraintResolver(executionProperties);

    @InjectMocks
    private CodeExecutionServiceImpl codeExecutionService;

    private final LanguageFactoryImpl languageFactory = new LanguageFactoryImpl();
    private DockerContainerDetails testContainer;
    private IsolateSandBoxDetails testSandbox;

    @BeforeEach
    void setUp() {
        testContainer = new DockerContainerDetails("cnt-12345", "test-container");
        testSandbox = new IsolateSandBoxDetails(42, testContainer);
        lenient().when(dockerExecutionService.isContainerRunning(anyString())).thenReturn(true);
        lenient().when(dockerExecutionService.execContainer(anyString(), anyList()))
                .thenReturn(new DockerExecutionResult(0L, "", ""));
    }

    @Test
    @DisplayName("Returns SYSTEM_ERROR when request is null")
    void nullRequestReturnsSystemError() {
        SimpleCodeExecutionResult result = codeExecutionService.run((SimpleCodeExecutionRequest) null);

        assertThat(result).isNotNull();
        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SYSTEM_ERROR);
        assertThat(result.logs()).contains("Execution request and language must not be null.");
        verifyNoInteractions(dockerExecutionService, isolateExecutionService);
    }

    @Test
    @DisplayName("Returns SYSTEM_ERROR when language in request is null")
    void nullLanguageReturnsSystemError() {
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .code("some code")
                .language(null)
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result).isNotNull();
        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SYSTEM_ERROR);
        assertThat(result.logs()).contains("Execution request and language must not be null.");
        verifyNoInteractions(dockerExecutionService, isolateExecutionService);
    }

    @Test
    @DisplayName("Successfully compiles and runs compiled language (Java)")
    void executeCompiledLanguageSuccess() {
        Language java = languageFactory.create("java-21");
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(java)
                .code("public class Solution { public static void main(String[] args) { System.out.println(\"Hello\"); } }")
                .fileName("Solution.java")
                .stdin("")
                .constraints(new CodeExecutionConstraints(3000L, 524288L))
                .build();

        when(dockerExecutionService.createContainer(any(DockerImageDetails.class))).thenReturn(testContainer);
        doNothing().when(dockerExecutionService).startContainer(testContainer.id());
        when(isolateExecutionService.initialize(testContainer)).thenReturn(testSandbox);

        // Compilation succeeds
        when(dockerExecutionService.execContainer(eq(testContainer.id()), anyList()))
                .thenReturn(new DockerExecutionResult(0L, "", ""));

        // Isolate execution succeeds
        IsolateExecutionResult isolateResult = new IsolateExecutionResult(
                IsolateExecutionStatus.SUCCESS,
                "Hello\n",
                "",
                0.05,
                0.08,
                35000L,
                0L,
                0L,
                false,
                10L,
                2L);
        when(isolateExecutionService.executeWithConstraints(eq(testSandbox), anyList(), eq(""),
                any(IsolateExecutionConstraints.class)))
                .thenReturn(isolateResult);

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result).isNotNull();
        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout()).isEqualTo("Hello\n");
        assertThat(result.stderr()).isEmpty();
        assertThat(result.exitCode()).isEqualTo(0L);
        assertThat(result.logs()).isNotEmpty();

        // Verify lifecycle order and cleanup
        verify(dockerExecutionService).createContainer(java.dockerImageDetails());
        verify(dockerExecutionService).startContainer(testContainer.id());
        verify(isolateExecutionService).initialize(testContainer);
        verify(dockerExecutionService).writeFile(eq(testContainer.id()), eq("/var/lib/isolate/42/box/Solution.java"),
                anyString());
        verify(dockerExecutionService).execContainer(eq(testContainer.id()), anyList());
        List<String> expectedJavaRunCmd = List.of("/bin/bash", "-c", "exec \"$@\"", "--", "java", "-cp", ".",
                "Solution");
        ArgumentCaptor<IsolateExecutionConstraints> constraintsCaptor =
                ArgumentCaptor.forClass(IsolateExecutionConstraints.class);
        verify(isolateExecutionService).executeWithConstraints(eq(testSandbox), eq(expectedJavaRunCmd), eq(""),
                constraintsCaptor.capture());
        IsolateExecutionConstraints captured = constraintsCaptor.getValue();
        assertThat(captured.cpuTimeSeconds()).isEqualTo(3.0);
        assertThat(captured.wallTimeSeconds()).isEqualTo(6.0);
        assertThat(captured.memoryKb()).isNull();

        verify(isolateExecutionService).cleanup(testSandbox);
        verify(dockerExecutionService).stopContainer(testContainer.id());
        verify(dockerExecutionService).deleteContainer(testContainer.id());
    }

    @Test
    @DisplayName("Successfully compiles and runs compiled language (C++) with memory constraints")
    void executeCompiledLanguageCppWithMemoryConstraintsSuccess() {
        Language cpp = languageFactory.create("cpp-23");
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code("#include <iostream>\nint main() { std::cout << 42; return 0; }")
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(3000L, 131072L))
                .build();

        when(dockerExecutionService.createContainer(any(DockerImageDetails.class))).thenReturn(testContainer);
        doNothing().when(dockerExecutionService).startContainer(testContainer.id());
        when(isolateExecutionService.initialize(testContainer)).thenReturn(testSandbox);
        when(dockerExecutionService.execContainer(eq(testContainer.id()), anyList()))
                .thenReturn(new DockerExecutionResult(0L, "", ""));

        IsolateExecutionResult isolateResult = new IsolateExecutionResult(
                IsolateExecutionStatus.SUCCESS,
                "42",
                "",
                0.01,
                0.02,
                15000L,
                0L,
                0L,
                false,
                10L,
                2L);
        when(isolateExecutionService.executeWithConstraints(eq(testSandbox), anyList(), eq(""),
                any(IsolateExecutionConstraints.class)))
                .thenReturn(isolateResult);

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result).isNotNull();
        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout()).isEqualTo("42");

        ArgumentCaptor<IsolateExecutionConstraints> constraintsCaptor =
                ArgumentCaptor.forClass(IsolateExecutionConstraints.class);
        verify(isolateExecutionService).executeWithConstraints(eq(testSandbox), anyList(), eq(""),
                constraintsCaptor.capture());
        IsolateExecutionConstraints captured = constraintsCaptor.getValue();
        assertThat(captured.cpuTimeSeconds()).isEqualTo(3.0);
        assertThat(captured.wallTimeSeconds()).isEqualTo(6.0);
        assertThat(captured.memoryKb()).isEqualTo(131072L);

        verify(isolateExecutionService).cleanup(testSandbox);
        verify(dockerExecutionService).stopContainer(testContainer.id());
        verify(dockerExecutionService).deleteContainer(testContainer.id());
    }

    @Test
    @DisplayName("Compilation error stops execution early and returns COMPILATION_ERROR")
    void executeCompiledLanguageCompilationErrorStopsBeforeRun() {
        Language java = languageFactory.create("java-21");
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(java)
                .code("invalid syntax code")
                .fileName("Solution.java")
                .build();

        when(dockerExecutionService.createContainer(any(DockerImageDetails.class))).thenReturn(testContainer);
        doNothing().when(dockerExecutionService).startContainer(testContainer.id());
        when(isolateExecutionService.initialize(testContainer)).thenReturn(testSandbox);

        // Compilation fails
        when(dockerExecutionService.execContainer(eq(testContainer.id()), anyList()))
                .thenReturn(new DockerExecutionResult(1L, "", "Solution.java:1: error: ';' expected\n"));

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result).isNotNull();
        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.COMPILATION_ERROR);
        assertThat(result.exitCode()).isEqualTo(1L);
        assertThat(result.stderr()).contains("error: ';' expected");
        assertThat(result.logs()).contains("Compilation failed with exit code: 1");

        // Verify isolate run was NOT called
        verify(isolateExecutionService, never()).executeWithConstraints(any(), anyList(), anyString(), any());

        // Cleanup was still executed
        verify(isolateExecutionService).cleanup(testSandbox);
        verify(dockerExecutionService).stopContainer(testContainer.id());
        verify(dockerExecutionService).deleteContainer(testContainer.id());
    }

    @Test
    @DisplayName("Interpreted language runs without compilation phase (Python)")
    void executeInterpretedLanguageSuccess() {
        Language python = languageFactory.create("python-3.12");
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(python)
                .code("print('Python Hello')")
                .fileName("solution.py")
                .stdin("input line")
                .constraints(new CodeExecutionConstraints(2500L, 131072L))
                .build();

        when(dockerExecutionService.createContainer(any(DockerImageDetails.class))).thenReturn(testContainer);
        doNothing().when(dockerExecutionService).startContainer(testContainer.id());
        when(isolateExecutionService.initialize(testContainer)).thenReturn(testSandbox);

        IsolateExecutionResult isolateResult = new IsolateExecutionResult(
                IsolateExecutionStatus.SUCCESS,
                "Python Hello\n",
                "",
                0.02,
                0.03,
                12000L,
                0L,
                0L,
                false,
                5L,
                1L);
        List<String> expectedPythonRunCmd = List.of("/bin/bash", "-c", "exec \"$@\"", "--", "python3", "solution.py");
        when(isolateExecutionService.executeWithConstraints(eq(testSandbox), eq(expectedPythonRunCmd), eq("input line"),
                any(IsolateExecutionConstraints.class)))
                .thenReturn(isolateResult);

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout()).isEqualTo("Python Hello\n");

        // Compilation command was NOT called for interpreted language
        verify(dockerExecutionService, never()).execContainer(anyString(), anyList());
        ArgumentCaptor<IsolateExecutionConstraints> pythonConstraintsCaptor =
                ArgumentCaptor.forClass(IsolateExecutionConstraints.class);
        verify(isolateExecutionService).executeWithConstraints(eq(testSandbox), eq(expectedPythonRunCmd),
                eq("input line"), pythonConstraintsCaptor.capture());
        IsolateExecutionConstraints captured = pythonConstraintsCaptor.getValue();
        assertThat(captured.cpuTimeSeconds()).isEqualTo(2.5);
        assertThat(captured.wallTimeSeconds()).isEqualTo(5.0);
        assertThat(captured.memoryKb()).isEqualTo(131072L);

        verify(isolateExecutionService).cleanup(testSandbox);
        verify(dockerExecutionService).stopContainer(testContainer.id());
        verify(dockerExecutionService).deleteContainer(testContainer.id());
    }

    @Test
    @DisplayName("Resolves default file names when request fileName is null or blank")
    void resolvesDefaultFileNames() {
        Language java = languageFactory.create("java-21");
        Language python = languageFactory.create("python-3.12");
        Language cpp = languageFactory.create("cpp-23");

        // Java defaults to Solution.java
        when(dockerExecutionService.createContainer(any(DockerImageDetails.class))).thenReturn(testContainer);
        when(isolateExecutionService.initialize(testContainer)).thenReturn(testSandbox);
        when(dockerExecutionService.execContainer(any(), any())).thenReturn(new DockerExecutionResult(0L, "", ""));
        when(isolateExecutionService.executeWithConstraints(any(), any(), any(), any()))
                .thenReturn(new IsolateExecutionResult(IsolateExecutionStatus.SUCCESS, "", "", 0.0, 0.0, 1000L, 0L, 0L,
                        false, 0L, 0L));

        codeExecutionService
                .run(SimpleCodeExecutionRequest.builder().language(java).code("code").fileName(null).build());
        verify(dockerExecutionService).writeFile(eq(testContainer.id()), eq("/var/lib/isolate/42/box/Main.java"),
                eq("code"));

        // Python defaults to main.py
        codeExecutionService
                .run(SimpleCodeExecutionRequest.builder().language(python).code("code").fileName("  ").build());
        verify(dockerExecutionService).writeFile(eq(testContainer.id()), eq("/var/lib/isolate/42/box/main.py"),
                eq("code"));

        // C++ defaults to main.cpp
        codeExecutionService
                .run(SimpleCodeExecutionRequest.builder().language(cpp).code("code").fileName(null).build());
        verify(dockerExecutionService).writeFile(eq(testContainer.id()), eq("/var/lib/isolate/42/box/main.cpp"),
                eq("code"));
    }

    @Test
    @DisplayName("Maps TIME_LIMIT_EXCEEDED status from isolate result")
    void executeTimeLimitExceeded() {
        Language python = languageFactory.create("python-3.12");
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(python)
                .code("while True: pass")
                .fileName("solution.py")
                .build();

        when(dockerExecutionService.createContainer(any(DockerImageDetails.class))).thenReturn(testContainer);
        when(isolateExecutionService.initialize(testContainer)).thenReturn(testSandbox);

        IsolateExecutionResult isolateResult = new IsolateExecutionResult(
                IsolateExecutionStatus.TIME_LIMIT_EXCEEDED,
                "",
                "Time limit exceeded",
                2.01,
                5.02,
                15000L,
                null,
                9L,
                true,
                10L,
                2L);
        when(isolateExecutionService.executeWithConstraints(any(), any(), any(), any())).thenReturn(isolateResult);

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
        assertThat(result.stderr()).isEqualTo("Time limit exceeded");
    }

    @Test
    @DisplayName("Maps MEMORY_LIMIT_EXCEEDED status from isolate result")
    void executeMemoryLimitExceeded() {
        Language python = languageFactory.create("python-3.12");
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(python)
                .code("a = [1] * 100000000")
                .fileName("solution.py")
                .build();

        when(dockerExecutionService.createContainer(any(DockerImageDetails.class))).thenReturn(testContainer);
        when(isolateExecutionService.initialize(testContainer)).thenReturn(testSandbox);

        IsolateExecutionResult isolateResult = new IsolateExecutionResult(
                IsolateExecutionStatus.MEMORY_LIMIT_EXCEEDED,
                "",
                "Out of memory",
                0.5,
                0.7,
                262145L,
                null,
                9L,
                true,
                10L,
                2L);
        when(isolateExecutionService.executeWithConstraints(any(), any(), any(), any())).thenReturn(isolateResult);

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.MEMORY_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("Maps RUNTIME_ERROR status from isolate result")
    void executeRuntimeError() {
        Language python = languageFactory.create("python-3.12");
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(python)
                .code("raise ValueError('bad data')")
                .fileName("solution.py")
                .build();

        when(dockerExecutionService.createContainer(any(DockerImageDetails.class))).thenReturn(testContainer);
        when(isolateExecutionService.initialize(testContainer)).thenReturn(testSandbox);

        IsolateExecutionResult isolateResult = new IsolateExecutionResult(
                IsolateExecutionStatus.RUNTIME_ERROR,
                "",
                "ValueError: bad data",
                0.05,
                0.06,
                12000L,
                1L,
                0L,
                false,
                5L,
                1L);
        when(isolateExecutionService.executeWithConstraints(any(), any(), any(), any())).thenReturn(isolateResult);

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
        assertThat(result.exitCode()).isEqualTo(1L);
        assertThat(result.stderr()).contains("ValueError: bad data");
    }

    @Test
    @DisplayName("Catches DockerException and returns SYSTEM_ERROR with cleanup")
    void dockerExceptionReturnsSystemErrorAndCleansUp() {
        Language python = languageFactory.create("python-3.12");
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(python)
                .code("print(1)")
                .build();

        when(dockerExecutionService.createContainer(any(DockerImageDetails.class))).thenReturn(testContainer);
        doThrow(new DockerExecutionException("Docker start daemon died")).when(dockerExecutionService)
                .startContainer(testContainer.id());

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SYSTEM_ERROR);
        assertThat(result.logs()).contains("Docker start daemon died");

        // Verify container cleanup was still attempted in finally
        verify(dockerExecutionService).stopContainer(testContainer.id());
        verify(dockerExecutionService).deleteContainer(testContainer.id());
    }

    @Test
    @DisplayName("Catches IsolateException during execution and returns SYSTEM_ERROR with cleanup")
    void isolateExceptionReturnsSystemErrorAndCleansUp() {
        Language python = languageFactory.create("python-3.12");
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(python)
                .code("print(1)")
                .build();

        when(dockerExecutionService.createContainer(any(DockerImageDetails.class))).thenReturn(testContainer);
        when(isolateExecutionService.initialize(testContainer)).thenReturn(testSandbox);
        when(isolateExecutionService.executeWithConstraints(any(), any(), any(), any()))
                .thenThrow(new IsolateExecutionException("Isolate execution crashed"));

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SYSTEM_ERROR);
        assertThat(result.logs()).contains("Isolate execution crashed");

        // Sandbox and container cleanup executed
        verify(isolateExecutionService).cleanup(testSandbox);
        verify(dockerExecutionService).stopContainer(testContainer.id());
        verify(dockerExecutionService).deleteContainer(testContainer.id());
    }

    @Test
    @DisplayName("Executes database language (MySQL) with DatabaseContainerConstraints, no isolate, and cleans up")
    void executeDatabaseSuccess() {
        Language mysql = languageFactory.create("mysql-8.0");
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(mysql)
                .code("SELECT 1;")
                .fileName("solution.sql")
                .constraints(new CodeExecutionConstraints(4000L))
                .build();

        when(dockerExecutionService.createContainer(eq(mysql.dockerImageDetails()), any(DockerContainerConstraints.class)))
                .thenReturn(testContainer);
        doNothing().when(dockerExecutionService).startContainer(testContainer.id());
        when(dockerExecutionService.execContainer(eq(testContainer.id()), anyList(), eq(4000L)))
                .thenReturn(new DockerExecutionResult(0L, "1\n1\n", ""));

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result).isNotNull();
        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout()).isEqualTo("1\n1\n");
        assertThat(result.exitCode()).isEqualTo(0L);

        // Verify container created with DatabaseContainerConstraints
        ArgumentCaptor<DockerContainerConstraints> constraintsCaptor =
                ArgumentCaptor.forClass(DockerContainerConstraints.class);
        verify(dockerExecutionService).createContainer(eq(mysql.dockerImageDetails()), constraintsCaptor.capture());
        assertThat(constraintsCaptor.getValue().cpuLimit()).isEqualTo(1L);
        assertThat(constraintsCaptor.getValue().memoryLimitKb()).isEqualTo(262144L);

        // Verify file written to /tmp/solution.sql
        verify(dockerExecutionService).writeFile(eq(testContainer.id()), eq("/tmp/solution.sql"), eq("SELECT 1;"));

        // Verify execContainer called with timeout
        verify(dockerExecutionService).execContainer(eq(testContainer.id()), anyList(), eq(4000L));

        // Verify Isolate was NEVER used for database execution
        verifyNoInteractions(isolateExecutionService);

        // Verify cleanup
        verify(dockerExecutionService).stopContainer(testContainer.id());
        verify(dockerExecutionService).deleteContainer(testContainer.id());
    }

    @Test
    @DisplayName("Database execution returns TIME_LIMIT_EXCEEDED on exit code 124")
    void executeDatabaseTimeLimitExceeded() {
        Language postgres = languageFactory.create("postgresql-16");
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(postgres)
                .code("SELECT pg_sleep(10);")
                .constraints(new CodeExecutionConstraints(2000L))
                .build();

        when(dockerExecutionService.createContainer(eq(postgres.dockerImageDetails()), any(DockerContainerConstraints.class)))
                .thenReturn(testContainer);
        doNothing().when(dockerExecutionService).startContainer(testContainer.id());
        when(dockerExecutionService.execContainer(eq(testContainer.id()), anyList(), eq(2000L)))
                .thenReturn(new DockerExecutionResult(124L, "", "Execution timed out"));

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result).isNotNull();
        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
        assertThat(result.exitCode()).isEqualTo(124L);

        verifyNoInteractions(isolateExecutionService);
        verify(dockerExecutionService).stopContainer(testContainer.id());
        verify(dockerExecutionService).deleteContainer(testContainer.id());
    }

    @Test
    @DisplayName("Database execution returns RUNTIME_ERROR on non-zero exit code")
    void executeDatabaseRuntimeError() {
        Language mysql = languageFactory.create("mysql-8.0");
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(mysql)
                .code("SELECT * FROM non_existing_table;")
                .build();

        when(dockerExecutionService.createContainer(eq(mysql.dockerImageDetails()), any(DockerContainerConstraints.class)))
                .thenReturn(testContainer);
        doNothing().when(dockerExecutionService).startContainer(testContainer.id());
        when(dockerExecutionService.execContainer(eq(testContainer.id()), anyList(), eq(5000L)))
                .thenReturn(new DockerExecutionResult(1L, "", "Table 'non_existing_table' doesn't exist"));

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result).isNotNull();
        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
        assertThat(result.exitCode()).isEqualTo(1L);
        assertThat(result.stderr()).contains("Table 'non_existing_table' doesn't exist");

        verifyNoInteractions(isolateExecutionService);
        verify(dockerExecutionService).stopContainer(testContainer.id());
        verify(dockerExecutionService).deleteContainer(testContainer.id());
    }

    @Test
    @DisplayName("Database execution handles null code, negative constraints, and defaults time limit to 5000ms")
    void executeDatabaseNullCodeAndNegativeConstraints() {
        Language mysql = languageFactory.create("mysql-8.0");
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(mysql)
                .code(null)
                .fileName(null)
                .constraints(new CodeExecutionConstraints(-500L))
                .build();

        when(dockerExecutionService.createContainer(eq(mysql.dockerImageDetails()), any(DockerContainerConstraints.class)))
                .thenReturn(testContainer);
        doNothing().when(dockerExecutionService).startContainer(testContainer.id());
        when(dockerExecutionService.execContainer(eq(testContainer.id()), anyList(), eq(5000L)))
                .thenReturn(new DockerExecutionResult(0L, "Query OK", ""));

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout()).isEqualTo("Query OK");

        verify(dockerExecutionService).writeFile(eq(testContainer.id()), eq("/tmp/main.sql"), eq(""));
        verify(dockerExecutionService).execContainer(eq(testContainer.id()), anyList(), eq(5000L));
        verifyNoInteractions(isolateExecutionService);
        verify(dockerExecutionService).stopContainer(testContainer.id());
        verify(dockerExecutionService).deleteContainer(testContainer.id());
    }

    @Test
    @DisplayName("Database execution returns SYSTEM_ERROR when container creation fails and does not attempt cleanup")
    void executeDatabaseContainerCreationThrowsException() {
        Language postgres = languageFactory.create("postgresql-16");
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(postgres)
                .code("SELECT 1;")
                .build();

        when(dockerExecutionService.createContainer(eq(postgres.dockerImageDetails()), any(DockerContainerConstraints.class)))
                .thenThrow(new DockerContainerCreationException("Failed to allocate container port or cgroup"));

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SYSTEM_ERROR);
        assertThat(result.logs()).contains("Failed to allocate container port or cgroup");

        verifyNoInteractions(isolateExecutionService);
        verify(dockerExecutionService, never()).stopContainer(anyString());
        verify(dockerExecutionService, never()).deleteContainer(anyString());
    }

    @Test
    @DisplayName("Database execution returns SYSTEM_ERROR when startContainer fails and cleans up container")
    void executeDatabaseStartContainerThrowsExceptionWithCleanup() {
        Language mysql = languageFactory.create("mysql-8.0");
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(mysql)
                .code("SELECT 1;")
                .build();

        when(dockerExecutionService.createContainer(eq(mysql.dockerImageDetails()), any(DockerContainerConstraints.class)))
                .thenReturn(testContainer);
        doThrow(new DockerContainerNotFoundException("Container cnt-12345 not found"))
                .when(dockerExecutionService).startContainer(testContainer.id());

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SYSTEM_ERROR);
        assertThat(result.logs()).contains("Container cnt-12345 not found");

        verify(dockerExecutionService).stopContainer(testContainer.id());
        verify(dockerExecutionService).deleteContainer(testContainer.id());
    }

    @Test
    @DisplayName("Database execution returns SYSTEM_ERROR when writing SQL script fails and cleans up container")
    void executeDatabaseWriteFileThrowsExceptionWithCleanup() {
        Language mysql = languageFactory.create("mysql-8.0");
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(mysql)
                .code("SELECT 1;")
                .build();

        when(dockerExecutionService.createContainer(eq(mysql.dockerImageDetails()), any(DockerContainerConstraints.class)))
                .thenReturn(testContainer);
        doNothing().when(dockerExecutionService).startContainer(testContainer.id());
        doThrow(new DockerExecutionException("Disk full in docker container"))
                .when(dockerExecutionService).writeFile(eq(testContainer.id()), eq("/tmp/main.sql"), anyString());

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SYSTEM_ERROR);
        assertThat(result.logs()).contains("Disk full in docker container");

        verify(dockerExecutionService).stopContainer(testContainer.id());
        verify(dockerExecutionService).deleteContainer(testContainer.id());
    }

    @Test
    @DisplayName("Database execution returns SUCCESS even if stopContainer and deleteContainer throw in finally")
    void executeDatabaseCleanupThrowsExceptionsDoesNotMaskSuccess() {
        Language mysql = languageFactory.create("mysql-8.0");
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(mysql)
                .code("SELECT 42;")
                .build();

        when(dockerExecutionService.createContainer(eq(mysql.dockerImageDetails()), any(DockerContainerConstraints.class)))
                .thenReturn(testContainer);
        doNothing().when(dockerExecutionService).startContainer(testContainer.id());
        when(dockerExecutionService.execContainer(eq(testContainer.id()), anyList(), eq(5000L)))
                .thenReturn(new DockerExecutionResult(0L, "42\n", ""));

        doThrow(new DockerContainerStopException("Failed to stop", new RuntimeException()))
                .when(dockerExecutionService).stopContainer(testContainer.id());
        doThrow(new DockerContainerDeletionException("Failed to delete", new RuntimeException()))
                .when(dockerExecutionService).deleteContainer(testContainer.id());

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout()).isEqualTo("42\n");
        verify(dockerExecutionService).stopContainer(testContainer.id());
        verify(dockerExecutionService).deleteContainer(testContainer.id());
    }

    @Test
    @DisplayName("Compiled execution returns SYSTEM_ERROR when Isolate sandbox initialization fails and cleans up container")
    void executeCompiledLanguageSandboxInitFails() {
        Language cpp = languageFactory.create("cpp-23");
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code("int main() { return 0; }")
                .build();

        when(dockerExecutionService.createContainer(any(DockerImageDetails.class))).thenReturn(testContainer);
        doNothing().when(dockerExecutionService).startContainer(testContainer.id());
        when(isolateExecutionService.initialize(testContainer))
                .thenThrow(new IsolateInitializationException("Failed to initialize cgroup in isolate"));

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SYSTEM_ERROR);
        assertThat(result.logs()).contains("Failed to initialize cgroup in isolate");

        verify(isolateExecutionService, never()).cleanup(any());
        verify(dockerExecutionService).stopContainer(testContainer.id());
        verify(dockerExecutionService).deleteContainer(testContainer.id());
    }

    @Test
    @DisplayName("Compiled execution returns SYSTEM_ERROR when compiler execution throws DockerExecutionException")
    void executeCompiledLanguageCompilationThrowsUnexpectedException() {
        Language cpp = languageFactory.create("cpp-23");
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code("int main() { return 0; }")
                .build();

        when(dockerExecutionService.createContainer(any(DockerImageDetails.class))).thenReturn(testContainer);
        doNothing().when(dockerExecutionService).startContainer(testContainer.id());
        when(isolateExecutionService.initialize(testContainer)).thenReturn(testSandbox);
        when(dockerExecutionService.execContainer(eq(testContainer.id()), anyList()))
                .thenThrow(new DockerExecutionException("Docker exec socket broken during compile"));

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SYSTEM_ERROR);
        assertThat(result.logs()).contains("Docker exec socket broken during compile");

        verify(isolateExecutionService, never()).executeWithConstraints(any(), anyList(), anyString(), any());
        verify(isolateExecutionService).cleanup(testSandbox);
        verify(dockerExecutionService).stopContainer(testContainer.id());
        verify(dockerExecutionService).deleteContainer(testContainer.id());
    }

    @Test
    @DisplayName("Compiled execution handles null code and null stdin gracefully")
    void executeCompiledLanguageEmptyCodeAndNullStdin() {
        Language c = languageFactory.create("c-17");
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(c)
                .code(null)
                .fileName("main.c")
                .stdin(null)
                .build();

        when(dockerExecutionService.createContainer(any(DockerImageDetails.class))).thenReturn(testContainer);
        doNothing().when(dockerExecutionService).startContainer(testContainer.id());
        when(isolateExecutionService.initialize(testContainer)).thenReturn(testSandbox);
        when(dockerExecutionService.execContainer(eq(testContainer.id()), anyList()))
                .thenReturn(new DockerExecutionResult(0L, "", ""));

        IsolateExecutionResult isolateResult = new IsolateExecutionResult(
                IsolateExecutionStatus.SUCCESS,
                "",
                "",
                0.01,
                0.01,
                1000L,
                0L,
                0L,
                false,
                1L,
                1L);
        when(isolateExecutionService.executeWithConstraints(eq(testSandbox), anyList(), eq(""), any()))
                .thenReturn(isolateResult);

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        verify(dockerExecutionService).writeFile(eq(testContainer.id()), eq("/var/lib/isolate/42/box/main.c"), eq(""));
        verify(isolateExecutionService).executeWithConstraints(eq(testSandbox), anyList(), eq(""), any());
        verify(isolateExecutionService).cleanup(testSandbox);
        verify(dockerExecutionService).stopContainer(testContainer.id());
        verify(dockerExecutionService).deleteContainer(testContainer.id());
    }

    @Test
    @DisplayName("Compiled execution maps killed by signal (e.g. SIGSEGV 11) to RUNTIME_ERROR with exitSignal captured")
    void executeCompiledLanguageSignalKillMapsCorrectly() {
        Language cpp = languageFactory.create("cpp-23");
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code("int main() { int* p = nullptr; *p = 1; }")
                .build();

        when(dockerExecutionService.createContainer(any(DockerImageDetails.class))).thenReturn(testContainer);
        doNothing().when(dockerExecutionService).startContainer(testContainer.id());
        when(isolateExecutionService.initialize(testContainer)).thenReturn(testSandbox);
        when(dockerExecutionService.execContainer(eq(testContainer.id()), anyList()))
                .thenReturn(new DockerExecutionResult(0L, "", ""));

        IsolateExecutionResult isolateResult = new IsolateExecutionResult(
                IsolateExecutionStatus.RUNTIME_ERROR,
                "",
                "Segmentation fault",
                0.01,
                0.01,
                4000L,
                null,
                11L,
                false,
                10L,
                2L);
        when(isolateExecutionService.executeWithConstraints(eq(testSandbox), anyList(), anyString(), any()))
                .thenReturn(isolateResult);

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
        assertThat(result.exitSignal()).isEqualTo(11L);
        assertThat(result.stderr()).contains("Segmentation fault");
    }

    @Test
    @DisplayName("Compiled execution preserves COMPILATION_ERROR even if sandbox cleanup and container stop fail")
    void executeCompiledLanguageCleanupExceptionsDoNotMaskCompilationError() {
        Language java = languageFactory.create("java-21");
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(java)
                .code("invalid syntax")
                .build();

        when(dockerExecutionService.createContainer(any(DockerImageDetails.class))).thenReturn(testContainer);
        doNothing().when(dockerExecutionService).startContainer(testContainer.id());
        when(isolateExecutionService.initialize(testContainer)).thenReturn(testSandbox);
        when(dockerExecutionService.execContainer(eq(testContainer.id()), anyList()))
                .thenReturn(new DockerExecutionResult(1L, "", "error: class, interface, enum, or record expected"));

        doThrow(new IsolateCleanupException("Cleanup failed", new RuntimeException()))
                .when(isolateExecutionService).cleanup(testSandbox);
        doThrow(new DockerContainerStopException("Stop failed", new RuntimeException()))
                .when(dockerExecutionService).stopContainer(testContainer.id());

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.COMPILATION_ERROR);
        assertThat(result.stderr()).contains("error: class, interface, enum, or record expected");
        verify(isolateExecutionService).cleanup(testSandbox);
        verify(dockerExecutionService).stopContainer(testContainer.id());
        verify(dockerExecutionService).deleteContainer(testContainer.id());
    }

    @Test
    @DisplayName("Interpreted execution returns SYSTEM_ERROR when writing script to sandbox fails and cleans up")
    void executeInterpretedLanguageWriteFileThrowsException() {
        Language python = languageFactory.create("python-3.12");
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(python)
                .code("print('hello')")
                .build();

        when(dockerExecutionService.createContainer(any(DockerImageDetails.class))).thenReturn(testContainer);
        doNothing().when(dockerExecutionService).startContainer(testContainer.id());
        when(isolateExecutionService.initialize(testContainer)).thenReturn(testSandbox);
        doThrow(new DockerExecutionException("No space left on device"))
                .when(dockerExecutionService).writeFile(eq(testContainer.id()), eq("/var/lib/isolate/42/box/main.py"), anyString());

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SYSTEM_ERROR);
        assertThat(result.logs()).contains("No space left on device");

        verify(isolateExecutionService, never()).executeWithConstraints(any(), anyList(), anyString(), any());
        verify(isolateExecutionService).cleanup(testSandbox);
        verify(dockerExecutionService).stopContainer(testContainer.id());
        verify(dockerExecutionService).deleteContainer(testContainer.id());
    }

    @Test
    @DisplayName("Interpreted execution applies default constraints when non-positive constraints provided")
    void executeInterpretedLanguageZeroAndNegativeConstraintsFallBackToDefaults() {
        Language python = languageFactory.create("python-3.12");
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(python)
                .code("print(1)")
                .constraints(new CodeExecutionConstraints(-1000L, -50000L))
                .build();

        when(dockerExecutionService.createContainer(any(DockerImageDetails.class))).thenReturn(testContainer);
        doNothing().when(dockerExecutionService).startContainer(testContainer.id());
        when(isolateExecutionService.initialize(testContainer)).thenReturn(testSandbox);

        IsolateExecutionResult isolateResult = new IsolateExecutionResult(
                IsolateExecutionStatus.SUCCESS, "1\n", "", 0.01, 0.01, 5000L, 0L, 0L, false, 1L, 1L);
        when(isolateExecutionService.executeWithConstraints(eq(testSandbox), anyList(), anyString(), any()))
                .thenReturn(isolateResult);

        codeExecutionService.run(request);

        ArgumentCaptor<IsolateExecutionConstraints> constraintsCaptor =
                ArgumentCaptor.forClass(IsolateExecutionConstraints.class);
        verify(isolateExecutionService).executeWithConstraints(eq(testSandbox), anyList(), anyString(),
                constraintsCaptor.capture());
        IsolateExecutionConstraints captured = constraintsCaptor.getValue();
        assertThat(captured.cpuTimeSeconds()).isEqualTo(2.0);
        assertThat(captured.wallTimeSeconds()).isEqualTo(5.0);
        assertThat(captured.memoryKb()).isEqualTo(262144L);
    }

    @Test
    @DisplayName("Interpreted Node.js execution sets memoryKb to null to prevent V8 CodeRange address space crash")
    void executeInterpretedLanguageNodeJsExemptFromIsolateMemoryLimit() {
        Language node = languageFactory.create("javascript-node-20");
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(node)
                .code("console.log('node');")
                .constraints(new CodeExecutionConstraints(3500L, 131072L))
                .build();

        when(dockerExecutionService.createContainer(any(DockerImageDetails.class))).thenReturn(testContainer);
        doNothing().when(dockerExecutionService).startContainer(testContainer.id());
        when(isolateExecutionService.initialize(testContainer)).thenReturn(testSandbox);

        IsolateExecutionResult isolateResult = new IsolateExecutionResult(
                IsolateExecutionStatus.SUCCESS, "node\n", "", 0.02, 0.03, 25000L, 0L, 0L, false, 5L, 1L);
        when(isolateExecutionService.executeWithConstraints(eq(testSandbox), anyList(), anyString(), any()))
                .thenReturn(isolateResult);

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout()).isEqualTo("node\n");

        verify(dockerExecutionService, never()).execContainer(anyString(), anyList());

        ArgumentCaptor<IsolateExecutionConstraints> constraintsCaptor =
                ArgumentCaptor.forClass(IsolateExecutionConstraints.class);
        verify(isolateExecutionService).executeWithConstraints(eq(testSandbox), anyList(), anyString(),
                constraintsCaptor.capture());
        IsolateExecutionConstraints captured = constraintsCaptor.getValue();
        assertThat(captured.cpuTimeSeconds()).isEqualTo(3.5);
        assertThat(captured.wallTimeSeconds()).isEqualTo(7.0);
        assertThat(captured.memoryKb()).isNull();
    }

    @Test
    @DisplayName("Interpreted execution preserves SUCCESS even if Isolate cleanup throws exception")
    void executeInterpretedLanguageIsolateCleanupFailsDoesNotCrashSuccessResult() {
        Language python = languageFactory.create("python-3.12");
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(python)
                .code("print('ok')")
                .build();

        when(dockerExecutionService.createContainer(any(DockerImageDetails.class))).thenReturn(testContainer);
        doNothing().when(dockerExecutionService).startContainer(testContainer.id());
        when(isolateExecutionService.initialize(testContainer)).thenReturn(testSandbox);

        IsolateExecutionResult isolateResult = new IsolateExecutionResult(
                IsolateExecutionStatus.SUCCESS, "ok\n", "", 0.01, 0.01, 8000L, 0L, 0L, false, 1L, 1L);
        when(isolateExecutionService.executeWithConstraints(eq(testSandbox), anyList(), anyString(), any()))
                .thenReturn(isolateResult);

        doThrow(new IsolateCleanupException("Failed to remove sandbox directory", new RuntimeException()))
                .when(isolateExecutionService).cleanup(testSandbox);

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout()).isEqualTo("ok\n");

        verify(isolateExecutionService).cleanup(testSandbox);
        verify(dockerExecutionService).stopContainer(testContainer.id());
        verify(dockerExecutionService).deleteContainer(testContainer.id());
    }

    @Test
    @DisplayName("Executes SQLite 3 database problem without wait ready loop and captures execution time")
    void executeDatabaseSqliteSuccess() {
        Language sqlite = languageFactory.create("sqlite-3");
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(sqlite)
                .code("CREATE TABLE users (id INT); INSERT INTO users VALUES (1); SELECT * FROM users;")
                .build();

        when(dockerExecutionService.createContainer(eq(sqlite.dockerImageDetails()), any(DockerContainerConstraints.class)))
                .thenReturn(testContainer);
        doNothing().when(dockerExecutionService).startContainer(testContainer.id());
        when(dockerExecutionService.execContainer(eq(testContainer.id()), anyList(), eq(5000L)))
                .thenReturn(new DockerExecutionResult(0L, "id\n1\n", ""));

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout()).isEqualTo("id\n1\n");
        assertThat(result.executionTimeMs()).isNotNull();

        verify(dockerExecutionService).writeFile(eq(testContainer.id()), eq("/tmp/main.sql"), anyString());
        verify(dockerExecutionService).stopContainer(testContainer.id());
        verify(dockerExecutionService).deleteContainer(testContainer.id());
    }

    @Test
    @DisplayName("Executes MongoDB 8.0 with mongosh ping readiness check")
    void executeDatabaseMongoDbSuccess() {
        Language mongo = languageFactory.create("mongodb-8.0");
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(mongo)
                .code("db.users.insertOne({ name: 'Alice' }); printjson(db.users.findOne());")
                .build();

        when(dockerExecutionService.createContainer(eq(mongo.dockerImageDetails()), any(DockerContainerConstraints.class)))
                .thenReturn(testContainer);
        doNothing().when(dockerExecutionService).startContainer(testContainer.id());
        when(dockerExecutionService.execContainer(eq(testContainer.id()), anyList(), eq(5000L)))
                .thenReturn(new DockerExecutionResult(0L, "{ name: 'Alice' }\n", ""));

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout()).contains("Alice");
        assertThat(result.executionTimeMs()).isNotNull();

        verify(dockerExecutionService).writeFile(eq(testContainer.id()), eq("/tmp/main.js"), anyString());
        verify(dockerExecutionService).stopContainer(testContainer.id());
        verify(dockerExecutionService).deleteContainer(testContainer.id());
    }

    @Test
    @DisplayName("Database execution filters MySQL command line password warning from stderr")
    void executeDatabaseSanitizesMysqlPasswordWarning() {
        Language mysql = languageFactory.create("mysql-8.0");
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(mysql)
                .code("SELECT 1;")
                .build();

        when(dockerExecutionService.createContainer(eq(mysql.dockerImageDetails()), any(DockerContainerConstraints.class)))
                .thenReturn(testContainer);
        doNothing().when(dockerExecutionService).startContainer(testContainer.id());
        when(dockerExecutionService.execContainer(eq(testContainer.id()), anyList(), eq(5000L)))
                .thenReturn(new DockerExecutionResult(0L, "1\n", "mysql: [Warning] Using a password on the command line interface can be insecure."));

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stderr()).isNull();
    }

    @Test
    @DisplayName("Database execution returns SYSTEM_ERROR when database container crashes before becoming ready")
    void executeDatabaseContainerStopsDuringReadinessCheck() {
        Language postgres = languageFactory.create("postgresql-16");
        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(postgres)
                .code("SELECT 1;")
                .build();

        when(dockerExecutionService.createContainer(eq(postgres.dockerImageDetails()), any(DockerContainerConstraints.class)))
                .thenReturn(testContainer);
        doNothing().when(dockerExecutionService).startContainer(testContainer.id());
        when(dockerExecutionService.isContainerRunning(testContainer.id())).thenReturn(false);
        when(dockerExecutionService.getContainerLogs(testContainer.id(), 50)).thenReturn("FATAL: could not create shared memory segment");

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SYSTEM_ERROR);
        assertThat(result.logs()).anyMatch(log -> log.contains("Database container stopped unexpectedly"));
    }

    // =========================================================================
    // Batch Code Execution Unit Tests
    // =========================================================================

    @Test
    @DisplayName("Batch execution returns empty result when request or language is null")
    void batchExecutionNullRequestReturnsEmpty() {
        BatchCodeExecutionResult res1 = codeExecutionService.run((BatchCodeExecutionRequest) null);
        assertThat(res1).isNotNull();
        assertThat(res1.totalExecutions()).isEqualTo(0);
        assertThat(res1.results()).isEmpty();

        BatchCodeExecutionRequest reqNullLang = new BatchCodeExecutionRequest(null, Map.of(), null, List.of());
        BatchCodeExecutionResult res2 = codeExecutionService.run(reqNullLang);
        assertThat(res2.totalExecutions()).isEqualTo(0);
        assertThat(res2.results()).isEmpty();
    }

    @Test
    @DisplayName("Batch execution returns empty result when executions list is empty")
    void batchExecutionEmptyExecutionsReturnsEmpty() {
        Language python = languageFactory.create("python-3.12");
        BatchCodeExecutionRequest request = new BatchCodeExecutionRequest(python, Map.of("main.py", "print(1)"), null, List.of());

        BatchCodeExecutionResult result = codeExecutionService.run(request);
        assertThat(result.totalExecutions()).isEqualTo(0);
        assertThat(result.results()).isEmpty();
    }

    @Test
    @DisplayName("Batch execution creates ONE container, ONE sandbox, and runs all items with proper constraints")
    void batchExecutionRunsAllItemsWithSingleContainer() {
        Language java = languageFactory.create("java-21");

        BatchExecutionItem item1 = new BatchExecutionItem(null, "10\n", new CodeExecutionConstraints(1000L, 128000L));
        BatchExecutionItem item2 = new BatchExecutionItem(null, "20\n", new CodeExecutionConstraints(3000L, 256000L));

        BatchCodeExecutionRequest request = new BatchCodeExecutionRequest(
                java,
                Map.of("Main.java", "public class Main { public static void main(String[] args) {} }"),
                new CodeExecutionConstraints(2000L, 262144L),
                List.of(item1, item2)
        );

        when(dockerExecutionService.createContainer(eq(java.dockerImageDetails()))).thenReturn(testContainer);
        doNothing().when(dockerExecutionService).startContainer(testContainer.id());
        when(isolateExecutionService.initialize(testContainer)).thenReturn(testSandbox);

        // Compilation succeeds
        when(dockerExecutionService.execContainer(eq(testContainer.id()), anyList()))
                .thenReturn(new DockerExecutionResult(0L, "", ""));

        // Item 1 execution result
        IsolateExecutionResult res1 = new IsolateExecutionResult(
                IsolateExecutionStatus.SUCCESS, "ANS_1\n", "", 0.05, 0.08, 15000L, 0L, 0L, false, 10L, 2L
        );
        // Item 2 execution result
        IsolateExecutionResult res2 = new IsolateExecutionResult(
                IsolateExecutionStatus.SUCCESS, "ANS_2\n", "", 0.12, 0.15, 18000L, 0L, 0L, false, 12L, 3L
        );

        when(isolateExecutionService.executeWithConstraints(eq(testSandbox), anyList(), eq("10\n"), any(IsolateExecutionConstraints.class)))
                .thenReturn(res1);
        when(isolateExecutionService.executeWithConstraints(eq(testSandbox), anyList(), eq("20\n"), any(IsolateExecutionConstraints.class)))
                .thenReturn(res2);

        BatchCodeExecutionResult batchResult = codeExecutionService.run(request);

        assertThat(batchResult).isNotNull();
        assertThat(batchResult.totalExecutions()).isEqualTo(2);
        assertThat(batchResult.successfulExecutions()).isEqualTo(2);
        assertThat(batchResult.failedExecutions()).isEqualTo(0);
        assertThat(batchResult.results()).hasSize(2);
        assertThat(batchResult.results().get(0).stdout()).isEqualTo("ANS_1\n");
        assertThat(batchResult.results().get(1).stdout()).isEqualTo("ANS_2\n");

        // Container and sandbox created ONCE only!
        verify(dockerExecutionService, times(1)).createContainer(eq(java.dockerImageDetails()));
        verify(dockerExecutionService, times(1)).startContainer(testContainer.id());
        verify(isolateExecutionService, times(1)).initialize(testContainer);
        verify(isolateExecutionService, times(2)).executeWithConstraints(eq(testSandbox), anyList(), anyString(), any());
        verify(isolateExecutionService, times(1)).cleanup(testSandbox);
        verify(dockerExecutionService, times(1)).stopContainer(testContainer.id());
        verify(dockerExecutionService, times(1)).deleteContainer(testContainer.id());
    }

    @Test
    @DisplayName("Batch execution marks all items as COMPILATION_ERROR when compilation fails")
    void batchExecutionCompilationFailureMarksAllItems() {
        Language cpp = languageFactory.create("cpp-23");

        BatchExecutionItem item1 = new BatchExecutionItem(null, "1\n", null);
        BatchExecutionItem item2 = new BatchExecutionItem(null, "2\n", null);

        BatchCodeExecutionRequest request = new BatchCodeExecutionRequest(
                cpp,
                Map.of("main.cpp", "int main() { syntax error }"),
                new CodeExecutionConstraints(2000L, 262144L),
                List.of(item1, item2)
        );

        when(dockerExecutionService.createContainer(eq(cpp.dockerImageDetails()))).thenReturn(testContainer);
        doNothing().when(dockerExecutionService).startContainer(testContainer.id());
        when(isolateExecutionService.initialize(testContainer)).thenReturn(testSandbox);

        // Compilation fails
        when(dockerExecutionService.execContainer(eq(testContainer.id()), anyList()))
                .thenReturn(new DockerExecutionResult(1L, "", "error: expected ';' before '}'"));

        BatchCodeExecutionResult batchResult = codeExecutionService.run(request);

        assertThat(batchResult).isNotNull();
        assertThat(batchResult.totalExecutions()).isEqualTo(2);
        assertThat(batchResult.successfulExecutions()).isEqualTo(0);
        assertThat(batchResult.failedExecutions()).isEqualTo(2);
        assertThat(batchResult.results()).hasSize(2);
        assertThat(batchResult.results().get(0).executionStatus()).isEqualTo(CodeExecutionStatus.COMPILATION_ERROR);
        assertThat(batchResult.results().get(1).executionStatus()).isEqualTo(CodeExecutionStatus.COMPILATION_ERROR);

        // Isolate execution should never be invoked
        verify(isolateExecutionService, never()).executeWithConstraints(any(), any(), any(), any());
        verify(isolateExecutionService, times(1)).cleanup(testSandbox);
        verify(dockerExecutionService, times(1)).deleteContainer(testContainer.id());
    }

    @Test
    @DisplayName("Batch execution continues remaining items when one item encounters an unexpected error")
    void batchExecutionContinuesWhenItemFails() {
        Language python = languageFactory.create("python-3.12");

        BatchExecutionItem item1 = new BatchExecutionItem(null, "in1\n", null);
        BatchExecutionItem item2 = new BatchExecutionItem(null, "in2\n", null);

        BatchCodeExecutionRequest request = new BatchCodeExecutionRequest(
                python,
                Map.of("main.py", "import sys; print(sys.stdin.read())"),
                null,
                List.of(item1, item2)
        );

        when(dockerExecutionService.createContainer(eq(python.dockerImageDetails()))).thenReturn(testContainer);
        doNothing().when(dockerExecutionService).startContainer(testContainer.id());
        when(isolateExecutionService.initialize(testContainer)).thenReturn(testSandbox);

        // Item 1 throws unexpected exception
        when(isolateExecutionService.executeWithConstraints(eq(testSandbox), anyList(), eq("in1\n"), any()))
                .thenThrow(new RuntimeException("Simulated isolate crash"));

        // Item 2 succeeds
        IsolateExecutionResult res2 = new IsolateExecutionResult(
                IsolateExecutionStatus.SUCCESS, "out2\n", "", 0.05, 0.08, 10000L, 0L, 0L, false, 5L, 1L
        );
        when(isolateExecutionService.executeWithConstraints(eq(testSandbox), anyList(), eq("in2\n"), any()))
                .thenReturn(res2);

        BatchCodeExecutionResult batchResult = codeExecutionService.run(request);

        assertThat(batchResult.totalExecutions()).isEqualTo(2);
        assertThat(batchResult.successfulExecutions()).isEqualTo(1);
        assertThat(batchResult.failedExecutions()).isEqualTo(1);
        assertThat(batchResult.results().get(0).executionStatus()).isEqualTo(CodeExecutionStatus.SYSTEM_ERROR);
        assertThat(batchResult.results().get(1).executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
    }
}
