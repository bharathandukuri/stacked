package com.bharath.stacked.modules.execution.integration;

import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import com.bharath.stacked.modules.execution.dto.request.BatchCodeExecutionRequest;
import com.bharath.stacked.modules.execution.dto.request.BatchExecutionItem;
import com.bharath.stacked.modules.execution.dto.request.SimpleCodeExecutionRequest;
import com.bharath.stacked.modules.execution.dto.response.BatchCodeExecutionResult;
import com.bharath.stacked.modules.execution.dto.response.SimpleCodeExecutionResult;
import com.bharath.stacked.modules.execution.enums.CodeExecutionStatus;
import com.bharath.stacked.modules.language.Language;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@DisplayName("Python 3.12 Comprehensive Execution Limit & Edge Case Tests")
public class PythonExecutionLimitIT extends BaseExecutionIT {

    @Test
    @DisplayName("Normal Execution: Reading stdin and producing correct stdout")
    void testPythonNormalExecution() {
        Language python = languageRegistry.get("python-3.12");
        assumeLanguageImageAvailable(python);

        String code = """
                import sys
                lines = sys.stdin.read().split()
                print(sum(map(int, lines)))
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(python)
                .code(code)
                .fileName("solution.py")
                .stdin("10 20 30 40\n")
                .constraints(new CodeExecutionConstraints(3000L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout().trim()).isEqualTo("100");
    }

    @Test
    @DisplayName("Empty Input: Handles empty stdin without error")
    void testPythonEmptyInput() {
        Language python = languageRegistry.get("python-3.12");
        assumeLanguageImageAvailable(python);

        String code = """
                import sys
                data = sys.stdin.read()
                print("EMPTY" if len(data) == 0 else "NON_EMPTY")
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(python)
                .code(code)
                .fileName("solution.py")
                .stdin("")
                .constraints(new CodeExecutionConstraints(3000L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout().trim()).isEqualTo("EMPTY");
    }

    @Test
    @DisplayName("Large Input: Processes 20,000 numbers streamed into stdin")
    void testPythonLargeInput() {
        Language python = languageRegistry.get("python-3.12");
        assumeLanguageImageAvailable(python);

        String code = """
                import sys
                nums = [int(x) for x in sys.stdin.read().split()]
                print(f"COUNT={len(nums)} SUM={sum(nums)}")
                """;

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 20000; i++) {
            sb.append("5 ");
        }
        sb.append("\n");

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(python)
                .code(code)
                .fileName("solution.py")
                .stdin(sb.toString())
                .constraints(new CodeExecutionConstraints(4000L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout().trim()).isEqualTo("COUNT=20000 SUM=100000");
    }

    @Test
    @DisplayName("Time Limit Exceeded: Infinite loop is terminated cleanly")
    void testPythonTimeLimitExceeded() {
        Language python = languageRegistry.get("python-3.12");
        assumeLanguageImageAvailable(python);

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(python)
                .code("while True: pass")
                .fileName("solution.py")
                .constraints(new CodeExecutionConstraints(1000L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("Memory Limit Exceeded: Large list allocation mapped to MEMORY_LIMIT_EXCEEDED")
    void testPythonMemoryLimitExceeded() {
        Language python = languageRegistry.get("python-3.12");
        assumeLanguageImageAvailable(python);

        String code = """
                chunks = []
                for _ in range(300):
                    chunks.append("X" * (1024 * 1024)) # 1MB string
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(python)
                .code(code)
                .fileName("solution.py")
                .constraints(new CodeExecutionConstraints(3000L, 32768L)) // 32MB limit
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.MEMORY_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("Stack Exhaustion: RecursionError mapped to RUNTIME_ERROR (not MLE)")
    void testPythonStackExhaustion() {
        Language python = languageRegistry.get("python-3.12");
        assumeLanguageImageAvailable(python);

        String code = """
                def recurse(n):
                    return recurse(n + 1)
                recurse(0)
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(python)
                .code(code)
                .fileName("solution.py")
                .constraints(new CodeExecutionConstraints(3000L, 131072L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
        assertThat(result.stderr()).contains("RecursionError");
    }

    @Test
    @DisplayName("Runtime Error: ZeroDivisionError mapped to RUNTIME_ERROR")
    void testPythonZeroDivisionError() {
        Language python = languageRegistry.get("python-3.12");
        assumeLanguageImageAvailable(python);

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(python)
                .code("x = 10 / 0")
                .fileName("solution.py")
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
        assertThat(result.stderr()).contains("ZeroDivisionError");
    }

    @Test
    @DisplayName("Runtime Error: Missing import raises ModuleNotFoundError mapped to RUNTIME_ERROR")
    void testPythonMissingImport() {
        Language python = languageRegistry.get("python-3.12");
        assumeLanguageImageAvailable(python);

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(python)
                .code("import non_existent_stacked_module_123")
                .fileName("solution.py")
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
        assertThat(result.stderr()).contains("ModuleNotFoundError");
    }

    @Test
    @DisplayName("Runtime Error: Syntax error mapped to RUNTIME_ERROR")
    void testPythonSyntaxError() {
        Language python = languageRegistry.get("python-3.12");
        assumeLanguageImageAvailable(python);

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(python)
                .code("def broken_syntax(:")
                .fileName("solution.py")
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
        assertThat(result.stderr()).contains("SyntaxError");
    }

    @Test
    @DisplayName("Multi-File Python: solution.py imports helper.py")
    void testPythonMultiFile() {
        Language python = languageRegistry.get("python-3.12");
        assumeLanguageImageAvailable(python);

        String helper = """
                def transform(s):
                    return s.upper() + "_PROCESSED"
                """;
        String solution = """
                import helper
                print(helper.transform("stacked"))
                """;

        BatchCodeExecutionRequest request = BatchCodeExecutionRequest.builder()
                .language(python)
                .files(Map.of("solution.py", solution, "helper.py", helper))
                .executions(List.of(BatchExecutionItem.builder().executablePath("solution.py").stdin("").build()))
                .build();

        BatchCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.successfulExecutions()).isEqualTo(1);
        assertThat(result.results().get(0).stdout().trim()).isEqualTo("STACKED_PROCESSED");
    }

    @Test
    @DisplayName("Batch Execution: Executes multiple inputs in single container")
    void testPythonBatchSequential() {
        Language python = languageRegistry.get("python-3.12");
        assumeLanguageImageAvailable(python);

        String code = """
                import sys
                data = sys.stdin.read().strip()
                print(data[::-1])
                """;

        BatchCodeExecutionRequest request = BatchCodeExecutionRequest.builder()
                .language(python)
                .files(Map.of("solution.py", code))
                .executions(List.of(
                        BatchExecutionItem.builder().stdin("hello\n").build(),
                        BatchExecutionItem.builder().stdin("world\n").build()
                ))
                .build();

        BatchCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.totalExecutions()).isEqualTo(2);
        assertThat(result.successfulExecutions()).isEqualTo(2);
        assertThat(result.results().get(0).stdout().trim()).isEqualTo("olleh");
        assertThat(result.results().get(1).stdout().trim()).isEqualTo("dlrow");
    }
}

