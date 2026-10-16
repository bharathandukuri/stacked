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

@DisplayName("C 17 Live Integration Tests")
class CExecutionIT extends BaseExecutionIT {

    @Test
    @DisplayName("Executes C 17 single program with math library linking")
    void executeC17Success() {
        Language c = languageRegistry.get("c-17");
        assumeLanguageImageAvailable(c);

        String code = """
                #include <stdio.h>
                #include <math.h>

                int main() {
                    double val;
                    if (scanf("%lf", &val) == 1) {
                        printf("%.2f\\n", sqrt(val));
                    }
                    return 0;
                }
                """;

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(c)
                .code(code)
                .fileName("solution.c")
                .stdin("16.0\n")
                .constraints(new CodeExecutionConstraints(3000L, 65536L))
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout().trim()).isEqualTo("4.00");
    }

    @Test
    @DisplayName("C 17 batch execution compiles once and executes multiple inputs")
    void executeC17BatchSuccess() {
        Language c = languageRegistry.get("c-17");
        assumeLanguageImageAvailable(c);

        String code = """
                #include <stdio.h>
                int main() {
                    int x;
                    if (scanf("%d", &x) == 1) {
                        printf("%d\\n", x * x);
                    }
                    return 0;
                }
                """;

        BatchCodeExecutionRequest request = BatchCodeExecutionRequest.builder()
                .language(c)
                .files(Map.of("solution.c", code))
                .executions(List.of(
                        BatchExecutionItem.builder().stdin("3\n").build(),
                        BatchExecutionItem.builder().stdin("7\n").build()
                ))
                .build();

        BatchCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.totalExecutions()).isEqualTo(2);
        assertThat(result.successfulExecutions()).isEqualTo(2);
        assertThat(result.results().get(0).stdout().trim()).isEqualTo("9");
        assertThat(result.results().get(1).stdout().trim()).isEqualTo("49");
    }

    @Test
    @DisplayName("C 17 compilation error reports COMPILATION_ERROR status with compiler stderr")
    void executeC17CompilationError() {
        Language c = languageRegistry.get("c-17");
        assumeLanguageImageAvailable(c);

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(c)
                .code("int main() { syntax error }")
                .fileName("solution.c")
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.COMPILATION_ERROR);
        assertThat(result.stderr()).contains("error");
    }
}

