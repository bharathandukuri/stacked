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
@DisplayName("MySQL 8.0 Comprehensive Execution Limit & Edge Case Tests")
public class MySqlExecutionLimitIT extends BaseExecutionIT {

    @Test
    @DisplayName("Normal Execution: Scalar query and password warning sanitization")
    void testMySqlNormalExecution() {
        Language mysql = languageRegistry.get("mysql-8.0");
        assumeLanguageImageAvailable(mysql);

        String sql = "SELECT 40 + 2 AS answer;";

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(mysql)
                .code(sql)
                .fileName("main.sql")
                .constraints(new CodeExecutionConstraints(10000L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        if (result.stderr() != null) {
            assertThat(result.stderr()).doesNotContain("Using a password on the command line interface can be insecure");
        }
    }

    @Test
    @DisplayName("Table Operations: Creates table, inserts data, and verifies aggregation")
    void testMySqlTableOperations() {
        Language mysql = languageRegistry.get("mysql-8.0");
        assumeLanguageImageAvailable(mysql);

        String sql = """
                CREATE TABLE IF NOT EXISTS inventory (item_id INT PRIMARY KEY, qty INT);
                INSERT INTO inventory VALUES (1, 150), (2, 350);
                SELECT SUM(qty) AS total_qty FROM inventory;
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(mysql)
                .code(sql)
                .fileName("main.sql")
                .constraints(new CodeExecutionConstraints(10000L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout()).contains("500");
    }

    @Test
    @DisplayName("Time Limit Exceeded: SLEEP(10) terminated on 1000ms query timeout")
    void testMySqlTimeLimitExceeded() {
        Language mysql = languageRegistry.get("mysql-8.0");
        assumeLanguageImageAvailable(mysql);

        String sql = "SELECT SLEEP(10);";

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(mysql)
                .code(sql)
                .fileName("main.sql")
                .constraints(new CodeExecutionConstraints(1000L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("Runtime Error: Syntax error returns RUNTIME_ERROR")
    void testMySqlSyntaxError() {
        Language mysql = languageRegistry.get("mysql-8.0");
        assumeLanguageImageAvailable(mysql);

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(mysql)
                .code("SELEC 1;")
                .fileName("main.sql")
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
        assertThat(result.stderr()).contains("syntax");
    }

    @Test
    @DisplayName("Runtime Error: Duplicate key constraint violation returns RUNTIME_ERROR")
    void testMySqlDuplicateKeyError() {
        Language mysql = languageRegistry.get("mysql-8.0");
        assumeLanguageImageAvailable(mysql);

        String sql = """
                CREATE TABLE unique_items (id INT PRIMARY KEY);
                INSERT INTO unique_items VALUES (1);
                INSERT INTO unique_items VALUES (1);
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(mysql)
                .code(sql)
                .fileName("main.sql")
                .constraints(new CodeExecutionConstraints(5000L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
        assertThat(result.stderr()).contains("Duplicate entry");
    }

    @Test
    @DisplayName("Batch Execution: State persists across sequential queries in same MySQL container")
    void testMySqlBatchStatePersistence() {
        Language mysql = languageRegistry.get("mysql-8.0");
        assumeLanguageImageAvailable(mysql);

        String createSql = """
                CREATE TABLE batch_mysql (k VARCHAR(20) PRIMARY KEY, v INT);
                INSERT INTO batch_mysql VALUES ('alpha', 100);
                """;
        String insertSql = """
                INSERT INTO batch_mysql VALUES ('beta', 200);
                """;
        String selectSql = """
                SELECT SUM(v) FROM batch_mysql;
                """;

        BatchCodeExecutionRequest request = BatchCodeExecutionRequest.builder()
                .language(mysql)
                .files(Map.of(
                        "create.sql", createSql,
                        "insert.sql", insertSql,
                        "select.sql", selectSql
                ))
                .constraints(new CodeExecutionConstraints(5000L))
                .executions(List.of(
                        BatchExecutionItem.builder().executablePath("create.sql").build(),
                        BatchExecutionItem.builder().executablePath("insert.sql").build(),
                        BatchExecutionItem.builder().executablePath("select.sql").build()
                ))
                .build();

        BatchCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.totalExecutions()).isEqualTo(3);
        assertThat(result.successfulExecutions()).isEqualTo(3);
        assertThat(result.results().get(2).stdout()).contains("300");
    }
}

