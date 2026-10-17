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
@DisplayName("C 17 Comprehensive Execution Limit & Edge Case Tests")
public class CExecutionLimitIT extends BaseExecutionIT {

    @Test
    @DisplayName("Normal Execution: Math library functions with stdin input")
    void testCNormalExecution() {
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

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(c)
                .code(code)
                .fileName("solution.c")
                .stdin("16.0\n")
                .constraints(new CodeExecutionConstraints(3000L, 65536L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout().trim()).isEqualTo("4.00");
    }

    @Test
    @DisplayName("Empty Input: Handles empty stdin stream cleanly")
    void testCEmptyInput() {
        Language c = languageRegistry.get("c-17");
        assumeLanguageImageAvailable(c);

        String code = """
                #include <stdio.h>
                int main() {
                    int val;
                    if (scanf("%d", &val) != 1) {
                        printf("NO_INT_FOUND\\n");
                    }
                    return 0;
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(c)
                .code(code)
                .fileName("solution.c")
                .stdin("")
                .constraints(new CodeExecutionConstraints(1000L, 65536L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout().trim()).isEqualTo("NO_INT_FOUND");
    }

    @Test
    @DisplayName("Large Input: Streams 20,000 integers via scanf")
    void testCLargeInput() {
        Language c = languageRegistry.get("c-17");
        assumeLanguageImageAvailable(c);

        String code = """
                #include <stdio.h>
                int main() {
                    int x;
                    long long sum = 0;
                    int count = 0;
                    while (scanf("%d", &x) == 1) {
                        sum += x;
                        count++;
                    }
                    printf("COUNT=%d SUM=%lld\\n", count, sum);
                    return 0;
                }
                """;

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 20000; i++) {
            sb.append("3 ");
        }
        sb.append("\n");

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(c)
                .code(code)
                .fileName("solution.c")
                .stdin(sb.toString())
                .constraints(new CodeExecutionConstraints(2000L, 65536L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout().trim()).isEqualTo("COUNT=20000 SUM=60000");
    }

    @Test
    @DisplayName("Time Limit Exceeded: Infinite loop is terminated cleanly")
    void testCTimeLimitExceeded() {
        Language c = languageRegistry.get("c-17");
        assumeLanguageImageAvailable(c);

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(c)
                .code("int main() { while (1) {} return 0; }")
                .fileName("solution.c")
                .constraints(new CodeExecutionConstraints(1000L, 65536L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("Memory Limit Exceeded: Large memory allocation triggers MEMORY_LIMIT_EXCEEDED")
    void testCMemoryLimitExceeded() {
        Language c = languageRegistry.get("c-17");
        assumeLanguageImageAvailable(c);

        // Allocating and writing to 100 MB with 32 MB limit
        String code = """
                #include <stdlib.h>
                #include <string.h>
                int main() {
                    for (int i = 0; i < 100; i++) {
                        char *p = (char*) malloc(1024 * 1024);
                        if (!p) return 1;
                        memset(p, 1, 1024 * 1024);
                    }
                    return 0;
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(c)
                .code(code)
                .fileName("solution.c")
                .constraints(new CodeExecutionConstraints(3000L, 32768L)) // 32MB limit
                .build());

        assertThat(result.executionStatus()).isIn(CodeExecutionStatus.MEMORY_LIMIT_EXCEEDED, CodeExecutionStatus.RUNTIME_ERROR);
    }

    @Test
    @DisplayName("Stack Exhaustion: Recursive stack overflow mapped to RUNTIME_ERROR")
    void testCStackExhaustion() {
        Language c = languageRegistry.get("c-17");
        assumeLanguageImageAvailable(c);

        String code = """
                int recurse(int d) {
                    volatile char arr[4096];
                    arr[0] = (char) d;
                    return recurse(d + 1) + arr[0];
                }
                int main() {
                    return recurse(0);
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(c)
                .code(code)
                .fileName("solution.c")
                .constraints(new CodeExecutionConstraints(2000L, 65536L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
    }

    @Test
    @DisplayName("Runtime Error: Integer divide by zero raises SIGFPE mapped to RUNTIME_ERROR")
    void testCDivideByZero() {
        Language c = languageRegistry.get("c-17");
        assumeLanguageImageAvailable(c);

        String code = """
                #include <stdio.h>
                int main() {
                    volatile int x = 100;
                    volatile int y = 0;
                    printf("%d\\n", x / y);
                    return 0;
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(c)
                .code(code)
                .fileName("solution.c")
                .constraints(new CodeExecutionConstraints(2000L, 65536L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
    }

    @Test
    @DisplayName("Runtime Error: Null pointer dereference raises SIGSEGV mapped to RUNTIME_ERROR")
    void testCSegmentationFault() {
        Language c = languageRegistry.get("c-17");
        assumeLanguageImageAvailable(c);

        String code = """
                #include <stdio.h>
                int main() {
                    int *p = NULL;
                    *p = 500;
                    return 0;
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(c)
                .code(code)
                .fileName("solution.c")
                .constraints(new CodeExecutionConstraints(2000L, 65536L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
    }

    @Test
    @DisplayName("Compilation Error: Syntax error preserves compiler diagnostics")
    void testCCompilationError() {
        Language c = languageRegistry.get("c-17");
        assumeLanguageImageAvailable(c);

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(c)
                .code("int main() { syntax error }")
                .fileName("solution.c")
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.COMPILATION_ERROR);
        assertThat(result.stderr()).contains("error:");
    }

    @Test
    @DisplayName("Multi-File Compilation: solution.c links helper.c")
    void testCMultiFile() {
        Language c = languageRegistry.get("c-17");
        assumeLanguageImageAvailable(c);

        String helper = """
                int square(int x) { return x * x; }
                """;
        String solution = """
                #include <stdio.h>
                int square(int x);
                int main() {
                    printf("RES=%d\\n", square(9));
                    return 0;
                }
                """;

        BatchCodeExecutionRequest request = BatchCodeExecutionRequest.builder()
                .language(c)
                .files(Map.of("solution.c", solution, "helper.c", helper))
                .executions(List.of(BatchExecutionItem.builder().stdin("").build()))
                .build();

        BatchCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.successfulExecutions()).isEqualTo(1);
        assertThat(result.results().get(0).stdout().trim()).isEqualTo("RES=81");
    }

    @Test
    @DisplayName("Batch Execution: Sequential runs compile once and execute multiple inputs")
    void testCBatchSequential() {
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
}

