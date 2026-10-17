package com.bharath.stacked.modules.execution.integration;

import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import com.bharath.stacked.modules.execution.dto.request.BatchCodeExecutionRequest;
import com.bharath.stacked.modules.execution.dto.request.BatchExecutionItem;
import com.bharath.stacked.modules.execution.dto.response.BatchCodeExecutionResult;
import com.bharath.stacked.modules.execution.enums.CodeExecutionStatus;
import com.bharath.stacked.modules.language.Language;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@DisplayName("Execution Batch Limit & Item Independence Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ExecutionBatchLimitIntegrationTest extends BaseExecutionIT {

    @Test
    @Order(1)
    @DisplayName("Batch Item Independence: Item 1 PASS, Item 2 TLE, Item 3 PASS (C++ 23)")
    void testBatchIndependentExecutionOutcome() {
        Language cpp = languageRegistry.get("cpp-23");
        assumeLanguageImageAvailable(cpp);

        String code = """
                #include <iostream>
                int main() {
                    int mode;
                    if (!(std::cin >> mode)) return 0;
                    if (mode == 1) {
                        std::cout << "FAST_PASS\\n";
                    } else if (mode == 2) {
                        while (true) {} // TLE
                    } else if (mode == 3) {
                        std::cout << "RECOVERY_PASS\\n";
                    }
                    return 0;
                }
                """;

        BatchCodeExecutionRequest request = BatchCodeExecutionRequest.builder()
                .language(cpp)
                .files(Map.of("solution.cpp", code))
                .constraints(new CodeExecutionConstraints(1000L, 65536L)) // 1000ms batch default
                .executions(List.of(
                        BatchExecutionItem.builder().stdin("1\n").build(),
                        BatchExecutionItem.builder().stdin("2\n").build(),
                        BatchExecutionItem.builder().stdin("3\n").build()
                ))
                .build();

        BatchCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result).isNotNull();
        assertThat(result.totalExecutions()).isEqualTo(3);
        assertThat(result.successfulExecutions()).isEqualTo(2);
        assertThat(result.failedExecutions()).isEqualTo(1);
        assertThat(result.results()).hasSize(3);

        // Item 1: FAST_PASS
        assertThat(result.results().get(0).executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.results().get(0).stdout().trim()).isEqualTo("FAST_PASS");

        // Item 2: TIME_LIMIT_EXCEEDED
        assertThat(result.results().get(1).executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);

        // Item 3: RECOVERY_PASS
        assertThat(result.results().get(2).executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.results().get(2).stdout().trim()).isEqualTo("RECOVERY_PASS");
    }

    @Test
    @Order(2)
    @DisplayName("Batch Item Specific Limits: Overriding batch-level constraints per execution item (Python 3.12)")
    void testBatchItemSpecificConstraints() {
        Language python = languageRegistry.get("python-3.12");
        assumeLanguageImageAvailable(python);

        // A workload that takes ~0.8s
        String code = """
                import sys
                count = 0
                for i in range(10_000_000):
                    count += 1
                print("DONE")
                """;

        BatchCodeExecutionRequest request = BatchCodeExecutionRequest.builder()
                .language(python)
                .files(Map.of("solution.py", code))
                .constraints(new CodeExecutionConstraints(3000L, 262144L)) // Batch default 3000ms
                .executions(List.of(
                        // Item 1 inherits batch default (3000ms) -> PASS
                        BatchExecutionItem.builder().stdin("").build(),
                        // Item 2 overrides with strict 300ms limit -> TLE
                        BatchExecutionItem.builder().stdin("").constraints(new CodeExecutionConstraints(300L, 262144L)).build()
                ))
                .build();

        BatchCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.totalExecutions()).isEqualTo(2);
        assertThat(result.successfulExecutions()).isEqualTo(1);
        assertThat(result.failedExecutions()).isEqualTo(1);
        assertThat(result.results().get(0).executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.results().get(1).executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
    }

    @Test
    @Order(3)
    @DisplayName("Batch Compilation Error: All items receive COMPILATION_ERROR with diagnostics (Java 21)")
    void testBatchCompilationErrorCascade() {
        Language java = languageRegistry.get("java-21");
        assumeLanguageImageAvailable(java);

        String invalidCode = "public class Solution { invalid syntax error }";

        BatchCodeExecutionRequest request = BatchCodeExecutionRequest.builder()
                .language(java)
                .files(Map.of("Solution.java", invalidCode))
                .executions(List.of(
                        BatchExecutionItem.builder().stdin("1\n").build(),
                        BatchExecutionItem.builder().stdin("2\n").build()
                ))
                .build();

        BatchCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.totalExecutions()).isEqualTo(2);
        assertThat(result.successfulExecutions()).isEqualTo(0);
        assertThat(result.failedExecutions()).isEqualTo(2);
        assertThat(result.results().get(0).executionStatus()).isEqualTo(CodeExecutionStatus.COMPILATION_ERROR);
        assertThat(result.results().get(1).executionStatus()).isEqualTo(CodeExecutionStatus.COMPILATION_ERROR);
        assertThat(result.results().get(0).stderr()).contains("error");
    }

    @Test
    @Order(4)
    @DisplayName("Batch Database State Persistence: Sequential queries against same container (SQLite 3)")
    void testBatchDatabaseStatePersistence() {
        Language sqlite = languageRegistry.get("sqlite-3");
        assumeLanguageImageAvailable(sqlite);

        String initSql = """
                CREATE TABLE counters (name TEXT PRIMARY KEY, val INT);
                INSERT INTO counters VALUES ('hits', 10);
                """;
        String incSql = """
                UPDATE counters SET val = val + 5 WHERE name = 'hits';
                SELECT val FROM counters WHERE name = 'hits';
                """;
        String readSql = """
                SELECT val FROM counters WHERE name = 'hits';
                """;

        BatchCodeExecutionRequest request = BatchCodeExecutionRequest.builder()
                .language(sqlite)
                .files(Map.of(
                        "init.sql", initSql,
                        "inc.sql", incSql,
                        "read.sql", readSql
                ))
                .constraints(new CodeExecutionConstraints(5000L))
                .executions(List.of(
                        BatchExecutionItem.builder().executablePath("init.sql").build(),
                        BatchExecutionItem.builder().executablePath("inc.sql").build(),
                        BatchExecutionItem.builder().executablePath("read.sql").build()
                ))
                .build();

        BatchCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.totalExecutions()).isEqualTo(3);
        assertThat(result.successfulExecutions()).isEqualTo(3);
        assertThat(result.results().get(1).stdout()).contains("15");
        assertThat(result.results().get(2).stdout().trim()).contains("15");
    }
}

