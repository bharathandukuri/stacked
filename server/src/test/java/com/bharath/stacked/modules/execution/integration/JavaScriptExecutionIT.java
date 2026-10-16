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

@DisplayName("JavaScript (Node.js 20) Live Integration Tests")
class JavaScriptExecutionIT extends BaseExecutionIT {

    @Test
    @DisplayName("Executes JavaScript single program and verifies V8 address space does not crash in Isolate")
    void executeJavaScriptSuccess() {
        Language js = languageRegistry.get("javascript-node-20");
        assumeLanguageImageAvailable(js);

        String code = """
                const fs = require('fs');
                const input = fs.readFileSync(0, 'utf-8').trim();
                const nums = input.split(/\\s+/).map(Number);
                console.log(nums.reduce((acc, x) => acc + x, 0));
                """;

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(js)
                .code(code)
                .fileName("solution.js")
                .stdin("5 15 25\n")
                .constraints(new CodeExecutionConstraints(3000L, 131072L))
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout().trim()).isEqualTo("45");
    }

    @Test
    @DisplayName("JavaScript batch execution processes multiple inputs in single container")
    void executeJavaScriptBatchSuccess() {
        Language js = languageRegistry.get("javascript-node-20");
        assumeLanguageImageAvailable(js);

        String code = """
                const fs = require('fs');
                const text = fs.readFileSync(0, 'utf-8').trim();
                console.log(text.toUpperCase());
                """;

        BatchCodeExecutionRequest request = BatchCodeExecutionRequest.builder()
                .language(js)
                .files(Map.of("solution.js", code))
                .executions(List.of(
                        BatchExecutionItem.builder().stdin("hello\n").build(),
                        BatchExecutionItem.builder().stdin("world\n").build()
                ))
                .build();

        BatchCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.totalExecutions()).isEqualTo(2);
        assertThat(result.successfulExecutions()).isEqualTo(2);
        assertThat(result.results().get(0).stdout().trim()).isEqualTo("HELLO");
        assertThat(result.results().get(1).stdout().trim()).isEqualTo("WORLD");
    }

    @Test
    @DisplayName("JavaScript uncaught exception returns RUNTIME_ERROR")
    void executeJavaScriptRuntimeError() {
        Language js = languageRegistry.get("javascript-node-20");
        assumeLanguageImageAvailable(js);

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(js)
                .code("throw new Error('boom');")
                .fileName("solution.js")
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
        assertThat(result.stderr()).contains("Error: boom");
    }
}

