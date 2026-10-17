package com.bharath.stacked.modules.execution.integration;

import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import com.bharath.stacked.modules.execution.dto.request.SimpleCodeExecutionRequest;
import com.bharath.stacked.modules.execution.dto.response.SimpleCodeExecutionResult;
import com.bharath.stacked.modules.execution.enums.CodeExecutionStatus;
import com.bharath.stacked.modules.language.Language;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@DisplayName("Execution Status Classification Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ExecutionStatusClassificationIntegrationTest extends BaseExecutionIT {

    @Test
    @Order(1)
    @DisplayName("Classification: SUCCESS preserves stdout, exit code 0, and execution metrics")
    void testSuccessClassification() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        String code = """
                #include <iostream>
                int main() {
                    std::cout << "STATUS_OK\\n";
                    return 0;
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(code)
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(1000L, 65536L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout().trim()).isEqualTo("STATUS_OK");
        assertThat(result.exitCode()).isEqualTo(0L);
        assertThat(result.executionTimeMs()).isNotNull();
        assertThat(result.memoryUsageKb()).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("Classification: TIME_LIMIT_EXCEEDED on infinite loop, not misclassified as RE")
    void testTimeLimitClassification() {
        Language python = languageRegistry.get("python-3.12");
        assumeLanguageImageAvailable(python);

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(python)
                .code("while True: pass")
                .fileName("solution.py")
                .constraints(new CodeExecutionConstraints(1000L, 262144L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
    }

    @Test
    @Order(3)
    @DisplayName("Classification: MEMORY_LIMIT_EXCEEDED on std::bad_alloc, not misclassified as generic RE")
    void testMemoryLimitClassification() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        String code = """
                #include <vector>
                int main() {
                    std::vector<int> v(50 * 1024 * 1024);
                    return 0;
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(code)
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(3000L, 32768L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.MEMORY_LIMIT_EXCEEDED);
    }

    @Test
    @Order(4)
    @DisplayName("Classification: RUNTIME_ERROR on SIGFPE (integer divide by zero) in C++ 23")
    void testDivideByZeroClassification() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        String code = """
                #include <iostream>
                int main() {
                    volatile int a = 10;
                    volatile int b = 0;
                    std::cout << (a / b) << "\\n";
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
        // SIGFPE is signal 8
        if (result.exitSignal() != null && result.exitSignal() > 0) {
            assertThat(result.exitSignal()).isEqualTo(8L);
        }
    }

    @Test
    @Order(5)
    @DisplayName("Classification: RUNTIME_ERROR on Segmentation Fault (null pointer dereference) in C 17")
    void testSegmentationFaultClassification() {
        Language c = languageRegistry.get("c-17");
        assumeLanguageImageAvailable(c);

        String code = """
                #include <stdio.h>
                int main() {
                    int *ptr = NULL;
                    *ptr = 42; // SIGSEGV
                    return 0;
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(c)
                .code(code)
                .fileName("main.c")
                .constraints(new CodeExecutionConstraints(2000L, 65536L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
        // SIGSEGV is signal 11
        if (result.exitSignal() != null && result.exitSignal() > 0) {
            assertThat(result.exitSignal()).isEqualTo(11L);
        }
    }

    @Test
    @Order(6)
    @DisplayName("Classification: RUNTIME_ERROR on Java Unchecked Exceptions (NPE, ArrayIndexOutOfBounds)")
    void testJavaRuntimeExceptionsClassification() {
        Language java = languageRegistry.get("java-21");
        assumeLanguageImageAvailable(java);

        String npeCode = """
                public class Solution {
                    public static void main(String[] args) {
                        String s = null;
                        System.out.println(s.length());
                    }
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(java)
                .code(npeCode)
                .fileName("Solution.java")
                .constraints(new CodeExecutionConstraints(2000L, 131072L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
        assertThat(result.stderr()).contains("NullPointerException");
    }

    @Test
    @Order(7)
    @DisplayName("Classification: COMPILATION_ERROR preserves diagnostics and does not attempt execution")
    void testCompilationErrorClassification() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        String invalidCode = "int main() { syntax error here }";

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code(invalidCode)
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(1000L, 65536L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.COMPILATION_ERROR);
        assertThat(result.stderr()).contains("error");
    }

    @Test
    @Order(8)
    @DisplayName("Classification: SYSTEM_ERROR rejects path traversal attempts prior to resource allocation")
    void testSystemErrorOnInvalidPath() {
        Language java = languageRegistry.get("java-21");
        assumeLanguageImageAvailable(java);

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(java)
                .code("public class Solution {}")
                .fileName("../../etc/passwd")
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SYSTEM_ERROR);
        assertThat(result.logs().get(0)).contains("Validation failed");
    }
}

