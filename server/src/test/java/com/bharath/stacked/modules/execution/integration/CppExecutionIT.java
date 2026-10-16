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

@DisplayName("C++ 23 Live Integration Tests")
class CppExecutionIT extends BaseExecutionIT {

    @Test
    @DisplayName("Executes C++ 23 single program with stdin and vector sorting")
    void executeCpp23Success() {
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

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(code)
                .fileName("solution.cpp")
                .stdin("5\n5 1 4 2 3\n")
                .constraints(new CodeExecutionConstraints(3000L, 65536L))
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout().trim()).isEqualTo("1 2 3 4 5");
    }

    @Test
    @DisplayName("C++ 23 batch execution runs multiple items sequentially")
    void executeCpp23BatchSuccess() {
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

    @Test
    @DisplayName("C++ 23 enforces memory limit and terminates with MEMORY_LIMIT_EXCEEDED or RUNTIME_ERROR")
    void executeCpp23EnforcesMemoryLimit() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        String oomCode = """
                #include <vector>
                int main() {
                    std::vector<char*> ptrs;
                    for (int i = 0; i < 2000; ++i) {
                        ptrs.push_back(new char[1024 * 1024]); // 1MB per iter
                    }
                    return 0;
                }
                """;

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(oomCode)
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(3000L, 32768L)) // 32MB limit
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus())
                .isIn(CodeExecutionStatus.MEMORY_LIMIT_EXCEEDED, CodeExecutionStatus.RUNTIME_ERROR);
    }

    @Test
    @DisplayName("C++ 23 enforces execution timeout for infinite loops")
    void executeCpp23TimeLimitExceeded() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        String infiniteLoop = "int main() { while (true) {} return 0; }";

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(infiniteLoop)
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(1000L))
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
    }
}

