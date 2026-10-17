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
@DisplayName("Execution Resource Cleanup Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ExecutionCleanupIntegrationTest extends BaseExecutionIT {

    @Test
    @Order(1)
    @DisplayName("Cleanup: Normal execution cleans up Docker container and Isolate sandbox")
    void testCleanupOnSuccess() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code("int main() { return 0; }")
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(1000L, 65536L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
    }

    @Test
    @Order(2)
    @DisplayName("Cleanup: Timeout terminates execution and deletes container cleanly")
    void testCleanupOnTimeout() {
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
    @DisplayName("Cleanup: Memory violation cleans up container and Isolate sandbox")
    void testCleanupOnMemoryViolation() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code("#include <vector>\nint main() { std::vector<int> v(50 * 1024 * 1024); return 0; }")
                .fileName("solution.cpp")
                .constraints(new CodeExecutionConstraints(3000L, 32768L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.MEMORY_LIMIT_EXCEEDED);
    }

    @Test
    @Order(4)
    @DisplayName("Cleanup: Compilation failure cleans up container and Isolate sandbox")
    void testCleanupOnCompilationFailure() {
        Language java = languageRegistry.get("java-21");
        assumeLanguageImageAvailable(java);

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(java)
                .code("public class Solution { broken syntax }")
                .fileName("Solution.java")
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.COMPILATION_ERROR);
    }

    @Test
    @Order(5)
    @DisplayName("Cleanup: Runtime crash (SIGSEGV) safely terminates and cleans up")
    void testCleanupOnCrash() {
        Language c = languageRegistry.get("c-17");
        assumeLanguageImageAvailable(c);

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(c)
                .code("#include <stdio.h>\nint main() { int *p = NULL; *p = 1; return 0; }")
                .fileName("main.c")
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
    }

    @Test
    @Order(6)
    @DisplayName("Cleanup: Validation failure allocates zero Docker or Isolate resources")
    void testZeroAllocationOnValidationFailure() {
        Language java = languageRegistry.get("java-21");
        assumeLanguageImageAvailable(java);

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(java)
                .code("public class Solution {}")
                .fileName("../escape.java")
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SYSTEM_ERROR);
        assertThat(result.logs().get(0)).contains("Validation failed");
    }
}

