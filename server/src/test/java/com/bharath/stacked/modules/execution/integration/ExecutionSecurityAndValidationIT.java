package com.bharath.stacked.modules.execution.integration;

import com.bharath.stacked.modules.execution.dto.request.BatchCodeExecutionRequest;
import com.bharath.stacked.modules.execution.dto.request.BatchExecutionItem;
import com.bharath.stacked.modules.execution.dto.request.SimpleCodeExecutionRequest;
import com.bharath.stacked.modules.execution.dto.response.BatchCodeExecutionResult;
import com.bharath.stacked.modules.execution.dto.response.SimpleCodeExecutionResult;
import com.bharath.stacked.modules.execution.enums.CodeExecutionStatus;
import com.bharath.stacked.modules.language.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Execution Security and Path Validation Live Integration Tests")
class ExecutionSecurityAndValidationIT extends BaseExecutionIT {

    @ParameterizedTest
    @ValueSource(strings = {
            "../secret.txt",
            "../../etc/passwd",
            "..\\windows\\system32",
            "/etc/shadow",
            "C:\\Windows\\System32\\cmd.exe",
            "main\0.cpp"
    })
    @DisplayName("Single execution rejects malicious file paths before allocating Docker or Isolate resources")
    void singleExecutionRejectsMaliciousPaths(String maliciousPath) {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(cpp)
                .code("int main() { return 0; }")
                .fileName(maliciousPath)
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SYSTEM_ERROR);
        assertThat(result.logs().get(0)).contains("Validation failed");
    }

    @Test
    @DisplayName("Batch execution rejects path traversal in files map keys before resource allocation")
    void batchExecutionRejectsPathTraversalInFiles() {
        Language python = languageRegistry.get("python-3.12");
        assumeLanguageImageAvailable(python);

        BatchCodeExecutionRequest request = BatchCodeExecutionRequest.builder()
                .language(python)
                .files(Map.of("../escape.py", "print('hacked')"))
                .executions(List.of(BatchExecutionItem.builder().executablePath("../escape.py").build()))
                .build();

        BatchCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.totalExecutions()).isEqualTo(1);
        assertThat(result.successfulExecutions()).isEqualTo(0);
        assertThat(result.failedExecutions()).isEqualTo(1);
        assertThat(result.results().get(0).executionStatus()).isEqualTo(CodeExecutionStatus.SYSTEM_ERROR);
        assertThat(result.results().get(0).logs().get(0)).contains("Path traversal");
    }

    @Test
    @DisplayName("Batch execution rejects executablePath that does not exist in files map")
    void batchExecutionRejectsMissingExecutablePath() {
        Language python = languageRegistry.get("python-3.12");
        assumeLanguageImageAvailable(python);

        BatchCodeExecutionRequest request = BatchCodeExecutionRequest.builder()
                .language(python)
                .files(Map.of("actual.py", "print(1)"))
                .executions(List.of(BatchExecutionItem.builder().executablePath("missing.py").build()))
                .build();

        BatchCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.totalExecutions()).isEqualTo(1);
        assertThat(result.failedExecutions()).isEqualTo(1);
        assertThat(result.results().get(0).executionStatus()).isEqualTo(CodeExecutionStatus.SYSTEM_ERROR);
        assertThat(result.results().get(0).logs().get(0)).contains("does not exist in request files");
    }

    @Test
    @DisplayName("Batch execution rejects exceeding max batch size before allocating containers")
    void batchExecutionRejectsExcessiveBatchSize() {
        Language python = languageRegistry.get("python-3.12");
        assumeLanguageImageAvailable(python);

        List<BatchExecutionItem> items = new ArrayList<>();
        for (int i = 0; i < 55; i++) { // Max limit is 50
            items.add(BatchExecutionItem.builder().executablePath("main.py").build());
        }

        BatchCodeExecutionRequest request = BatchCodeExecutionRequest.builder()
                .language(python)
                .files(Map.of("main.py", "print(1)"))
                .executions(items)
                .build();

        BatchCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.totalExecutions()).isEqualTo(55);
        assertThat(result.failedExecutions()).isEqualTo(55);
        assertThat(result.results().get(0).executionStatus()).isEqualTo(CodeExecutionStatus.SYSTEM_ERROR);
        assertThat(result.results().get(0).logs().get(0)).contains("exceeds maximum allowed batch size");
    }
}

