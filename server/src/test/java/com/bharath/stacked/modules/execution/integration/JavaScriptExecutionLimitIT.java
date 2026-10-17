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
@DisplayName("JavaScript (Node.js 20) Comprehensive Execution Limit & Edge Case Tests")
public class JavaScriptExecutionLimitIT extends BaseExecutionIT {

    @Test
    @DisplayName("Normal Execution: Reading stdin and producing correct stdout")
    void testJavaScriptNormalExecution() {
        Language js = languageRegistry.get("javascript-node-20");
        assumeLanguageImageAvailable(js);

        String code = """
                const fs = require('fs');
                const input = fs.readFileSync(0, 'utf-8').trim();
                const nums = input.split(/\\s+/).map(Number);
                console.log(nums.reduce((acc, x) => acc + x, 0));
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(js)
                .code(code)
                .fileName("solution.js")
                .stdin("5 15 25\n")
                .constraints(new CodeExecutionConstraints(3000L, 131072L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout().trim()).isEqualTo("45");
    }

    @Test
    @DisplayName("Empty Input: Handles empty stdin stream cleanly")
    void testJavaScriptEmptyInput() {
        Language js = languageRegistry.get("javascript-node-20");
        assumeLanguageImageAvailable(js);

        String code = """
                const fs = require('fs');
                const input = fs.readFileSync(0, 'utf-8');
                console.log(input.length === 0 ? "EMPTY_OK" : "NOT_EMPTY");
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(js)
                .code(code)
                .fileName("solution.js")
                .stdin("")
                .constraints(new CodeExecutionConstraints(2000L, 131072L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout().trim()).isEqualTo("EMPTY_OK");
    }

    @Test
    @DisplayName("Large Input: Processes 20,000 numbers streamed into stdin")
    void testJavaScriptLargeInput() {
        Language js = languageRegistry.get("javascript-node-20");
        assumeLanguageImageAvailable(js);

        String code = """
                const fs = require('fs');
                const input = fs.readFileSync(0, 'utf-8').trim();
                const nums = input.split(/\\s+/);
                let sum = 0;
                for (let i = 0; i < nums.length; i++) {
                    sum += Number(nums[i]);
                }
                console.log(`COUNT=${nums.length} SUM=${sum}`);
                """;

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 20000; i++) {
            sb.append("4 ");
        }
        sb.append("\n");

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(js)
                .code(code)
                .fileName("solution.js")
                .stdin(sb.toString())
                .constraints(new CodeExecutionConstraints(3000L, 131072L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout().trim()).isEqualTo("COUNT=20000 SUM=80000");
    }

    @Test
    @DisplayName("Time Limit Exceeded: Infinite loop is terminated cleanly")
    void testJavaScriptTimeLimitExceeded() {
        Language js = languageRegistry.get("javascript-node-20");
        assumeLanguageImageAvailable(js);

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(js)
                .code("while (true) {}")
                .fileName("solution.js")
                .constraints(new CodeExecutionConstraints(1000L, 131072L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("Memory Limit Exceeded: V8 heap exhaustion mapped to MEMORY_LIMIT_EXCEEDED")
    void testJavaScriptMemoryLimitExceeded() {
        Language js = languageRegistry.get("javascript-node-20");
        assumeLanguageImageAvailable(js);

        // 32MB limit sets --max-old-space-size=24. Allocating on V8 heap triggers heap OOM
        String code = """
                const chunks = [];
                for (let i = 0; i < 50; i++) {
                    chunks.push(new Array(1000000).fill(42));
                }
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(js)
                .code(code)
                .fileName("solution.js")
                .constraints(new CodeExecutionConstraints(4000L, 32768L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.MEMORY_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("Stack Exhaustion: RangeError maximum call stack mapped to RUNTIME_ERROR")
    void testJavaScriptStackExhaustion() {
        Language js = languageRegistry.get("javascript-node-20");
        assumeLanguageImageAvailable(js);

        String code = """
                function recurse() {
                    recurse();
                }
                recurse();
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(js)
                .code(code)
                .fileName("solution.js")
                .constraints(new CodeExecutionConstraints(3000L, 131072L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
        assertThat(result.stderr()).contains("Maximum call stack size exceeded");
    }

    @Test
    @DisplayName("Runtime Error: TypeError mapped to RUNTIME_ERROR")
    void testJavaScriptTypeError() {
        Language js = languageRegistry.get("javascript-node-20");
        assumeLanguageImageAvailable(js);

        String code = """
                const obj = null;
                obj.doSomething();
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(js)
                .code(code)
                .fileName("solution.js")
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
        assertThat(result.stderr()).contains("TypeError");
    }

    @Test
    @DisplayName("Runtime Error: Missing module require mapped to RUNTIME_ERROR")
    void testJavaScriptMissingModule() {
        Language js = languageRegistry.get("javascript-node-20");
        assumeLanguageImageAvailable(js);

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(js)
                .code("const m = require('./non_existent_module');")
                .fileName("solution.js")
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
        assertThat(result.stderr()).contains("MODULE_NOT_FOUND");
    }

    @Test
    @DisplayName("Multi-File JavaScript: solution.js requires ./helper.js")
    void testJavaScriptMultiFile() {
        Language js = languageRegistry.get("javascript-node-20");
        assumeLanguageImageAvailable(js);

        String helper = """
                function formatMessage(msg) {
                    return `[FORMATTED]: ${msg}`;
                }
                module.exports = { formatMessage };
                """;
        String solution = """
                const { formatMessage } = require('./helper.js');
                console.log(formatMessage("Stacked Engine"));
                """;

        BatchCodeExecutionRequest request = BatchCodeExecutionRequest.builder()
                .language(js)
                .files(Map.of("solution.js", solution, "helper.js", helper))
                .executions(List.of(BatchExecutionItem.builder().executablePath("solution.js").stdin("").build()))
                .build();

        BatchCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.successfulExecutions()).isEqualTo(1);
        assertThat(result.results().get(0).stdout().trim()).isEqualTo("[FORMATTED]: Stacked Engine");
    }

    @Test
    @DisplayName("Batch Execution: Processes multiple inputs in single container")
    void testJavaScriptBatchSequential() {
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
}

