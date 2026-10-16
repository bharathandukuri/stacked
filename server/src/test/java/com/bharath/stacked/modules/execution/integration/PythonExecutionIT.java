package com.bharath.stacked.modules.execution.integration;

import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import com.bharath.stacked.modules.execution.dto.request.BatchCodeExecutionRequest;
import com.bharath.stacked.modules.execution.dto.request.BatchExecutionItem;
import com.bharath.stacked.modules.execution.dto.request.SimpleCodeExecutionRequest;
import com.bharath.stacked.modules.execution.dto.response.BatchCodeExecutionResult;
import com.bharath.stacked.modules.execution.dto.response.SimpleCodeExecutionResult;
import com.bharath.stacked.modules.execution.enums.CodeExecutionStatus;
import com.bharath.stacked.modules.language.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Python 3.12 Live Integration Tests")
class PythonExecutionIT extends BaseExecutionIT {

    @Test
    @DisplayName("Executes Python 3.12 script with stdin processing")
    void executePython312Success() {
        Language python = languageRegistry.get("python-3.12");
        assumeLanguageImageAvailable(python);

        String code = """
                import sys
                lines = sys.stdin.read().split()
                print(sum(map(int, lines)))
                """;

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(python)
                .code(code)
                .fileName("solution.py")
                .stdin("10 20 30 40\n")
                .constraints(new CodeExecutionConstraints(3000L))
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout().trim()).isEqualTo("100");
    }

    @Test
    @DisplayName("Python 3.12 batch execution executes multiple inputs in a single container")
    void executePython312BatchSuccess() {
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

    @Test
    @DisplayName("Python 3.12 runtime exception maps to RUNTIME_ERROR with traceback")
    void executePython312RuntimeError() {
        Language python = languageRegistry.get("python-3.12");
        assumeLanguageImageAvailable(python);

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(python)
                .code("raise ValueError('something went wrong')")
                .fileName("solution.py")
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
        assertThat(result.stderr()).contains("ValueError: something went wrong");
    }

    @Test
    @DisplayName("Python 3.12 timeout terminates infinite loop")
    void executePython312TimeLimitExceeded() {
        Language python = languageRegistry.get("python-3.12");
        assumeLanguageImageAvailable(python);

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(python)
                .code("while True: pass")
                .fileName("solution.py")
                .constraints(new CodeExecutionConstraints(1000L))
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
    }
}

