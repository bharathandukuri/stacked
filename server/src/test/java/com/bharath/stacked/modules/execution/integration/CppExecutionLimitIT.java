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
@DisplayName("C++ 23 Comprehensive Execution Limit & Edge Case Tests")
public class CppExecutionLimitIT extends BaseExecutionIT {

    @Test
    @DisplayName("Normal Execution: Vector sorting with stdin input")
    void testCppNormalExecution() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        String code = """
                #include <iostream>
                #include <vector>
                #include <algorithm>

                int main() {
                    int n;
                    if (!(std::cin >> n)) return 0;
                    std::vector<int> v(n);
                    for (int i = 0; i < n; ++i) std::cin >> v[i];
                    std::sort(v.begin(), v.end());
                    for (int i = 0; i < n; ++i) {
                        if (i > 0) std::cout << " ";
                        std::cout << v[i];
                    }
                    std::cout << "\\n";
                    return 0;
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(code)
                .fileName("solution.cpp")
                .stdin("5\n5 1 4 2 3\n")
                .constraints(new CodeExecutionConstraints(3000L, 65536L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout().trim()).isEqualTo("1 2 3 4 5");
    }

    @Test
    @DisplayName("Empty Input: Handles empty stdin cleanly")
    void testCppEmptyInput() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        String code = """
                #include <iostream>
                int main() {
                    int x;
                    if (!(std::cin >> x)) {
                        std::cout << "EMPTY_STREAM\\n";
                    }
                    return 0;
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(code)
                .fileName("solution.cpp")
                .stdin("")
                .constraints(new CodeExecutionConstraints(1000L, 65536L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout().trim()).isEqualTo("EMPTY_STREAM");
    }

    @Test
    @DisplayName("Large Input: Streams 25,000 integers using fast I/O")
    void testCppLargeInput() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        String code = """
                #include <iostream>
                int main() {
                    std::ios_base::sync_with_stdio(false);
                    std::cin.tie(NULL);
                    int x;
                    long long sum = 0;
                    int count = 0;
                    while (std::cin >> x) {
                        sum += x;
                        count++;
                    }
                    std::cout << "COUNT=" << count << " SUM=" << sum << "\\n";
                    return 0;
                }
                """;

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 25000; i++) {
            sb.append("2 ");
        }
        sb.append("\n");

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(code)
                .fileName("solution.cpp")
                .stdin(sb.toString())
                .constraints(new CodeExecutionConstraints(2000L, 65536L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout().trim()).isEqualTo("COUNT=25000 SUM=50000");
    }

    @Test
    @DisplayName("Time Limit Exceeded: Infinite loop is terminated cleanly")
    void testCppTimeLimitExceeded() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code("int main() { while (true) {} return 0; }")
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(1000L, 65536L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("Memory Limit Exceeded: std::bad_alloc mapped directly to MEMORY_LIMIT_EXCEEDED")
    void testCppMemoryLimitExceeded() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        String code = """
                #include <vector>
                int main() {
                    std::vector<int> v(50 * 1024 * 1024); // 200 MB
                    return 0;
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(code)
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(3000L, 32768L)) // 32MB limit
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.MEMORY_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("Stack Exhaustion: Deep recursion terminates with RUNTIME_ERROR (not MLE)")
    void testCppStackExhaustion() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

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
                .language(cpp)
                .code(code)
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(2000L, 65536L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
    }

    @Test
    @DisplayName("Runtime Error: Integer divide by zero raises SIGFPE mapped to RUNTIME_ERROR")
    void testCppDivideByZero() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        String code = """
                #include <iostream>
                int main() {
                    volatile int x = 42;
                    volatile int y = 0;
                    std::cout << (x / y) << "\\n";
                    return 0;
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(code)
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(2000L, 65536L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
    }

    @Test
    @DisplayName("Runtime Error: Segmentation fault (null pointer dereference) mapped to RUNTIME_ERROR")
    void testCppSegmentationFault() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        String code = """
                int main() {
                    int *ptr = nullptr;
                    *ptr = 123;
                    return 0;
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(code)
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(2000L, 65536L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
    }

    @Test
    @DisplayName("Compilation Error: Syntax error preserves compiler diagnostics in stderr")
    void testCppCompilationError() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code("int main() { syntax error }")
                .fileName("solution.cpp")
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.COMPILATION_ERROR);
        assertThat(result.stderr()).contains("error:");
    }

    @Test
    @DisplayName("Multi-File Compilation: solution.cpp includes helper.h and links helper.cpp")
    void testCppMultiFile() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        String header = """
                #ifndef HELPER_H
                #define HELPER_H
                int multiply(int a, int b);
                #endif
                """;
        String helperCpp = """
                #include "helper.h"
                int multiply(int a, int b) { return a * b; }
                """;
        String solutionCpp = """
                #include <iostream>
                #include "helper.h"
                int main() {
                    std::cout << "RES=" << multiply(6, 7) << "\\n";
                    return 0;
                }
                """;

        BatchCodeExecutionRequest request = BatchCodeExecutionRequest.builder()
                .language(cpp)
                .files(Map.of(
                        "solution.cpp", solutionCpp,
                        "helper.h", header,
                        "helper.cpp", helperCpp
                ))
                .executions(List.of(BatchExecutionItem.builder().stdin("").build()))
                .build();

        BatchCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.successfulExecutions()).isEqualTo(1);
        assertThat(result.results().get(0).stdout().trim()).isEqualTo("RES=42");
    }

    @Test
    @DisplayName("Batch Execution: Sequential runs execute multiple inputs in single compiled container")
    void testCppBatchSequential() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        String code = """
                #include <iostream>
                int main() {
                    int a, b;
                    if (std::cin >> a >> b) {
                        std::cout << (a * b) << "\\n";
                    }
                    return 0;
                }
                """;

        BatchCodeExecutionRequest request = BatchCodeExecutionRequest.builder()
                .language(cpp)
                .files(Map.of("solution.cpp", code))
                .executions(List.of(
                        BatchExecutionItem.builder().stdin("3 7\n").build(),
                        BatchExecutionItem.builder().stdin("4 5\n").build()
                ))
                .build();

        BatchCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.totalExecutions()).isEqualTo(2);
        assertThat(result.successfulExecutions()).isEqualTo(2);
        assertThat(result.results().get(0).stdout().trim()).isEqualTo("21");
        assertThat(result.results().get(1).stdout().trim()).isEqualTo("20");
    }
}

