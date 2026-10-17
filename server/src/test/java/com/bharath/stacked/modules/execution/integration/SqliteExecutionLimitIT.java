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
@DisplayName("SQLite 3 Comprehensive Execution Limit & Edge Case Tests")
public class SqliteExecutionLimitIT extends BaseExecutionIT {

    @Test
    @DisplayName("Normal Execution: DDL, INSERT, and SELECT queries")
    void testSqliteNormalExecution() {
        Language sqlite = languageRegistry.get("sqlite-3");
        assumeLanguageImageAvailable(sqlite);

        String sql = """
                CREATE TABLE products (id INTEGER PRIMARY KEY, name TEXT, price REAL);
                INSERT INTO products VALUES (1, 'Book', 12.50);
                INSERT INTO products VALUES (2, 'Pen', 1.50);
                SELECT name, price FROM products ORDER BY id;
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(sqlite)
                .code(sql)
                .fileName("main.sql")
                .constraints(new CodeExecutionConstraints(5000L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout()).contains("Book").contains("12.5");
        assertThat(result.stdout()).contains("Pen").contains("1.5");
    }

    @Test
    @DisplayName("Empty Script: Handles empty SQL script cleanly")
    void testSqliteEmptyScript() {
        Language sqlite = languageRegistry.get("sqlite-3");
        assumeLanguageImageAvailable(sqlite);

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(sqlite)
                .code("-- empty script comment")
                .fileName("main.sql")
                .constraints(new CodeExecutionConstraints(3000L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
    }

    @Test
    @DisplayName("Large Dataset: Generates 5,000 rows via recursive CTE and computes aggregate")
    void testSqliteLargeAggregation() {
        Language sqlite = languageRegistry.get("sqlite-3");
        assumeLanguageImageAvailable(sqlite);

        String sql = """
                WITH RECURSIVE nums(x) AS (
                    SELECT 1
                    UNION ALL
                    SELECT x + 1 FROM nums WHERE x < 5000
                )
                SELECT COUNT(*), SUM(x) FROM nums;
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(sqlite)
                .code(sql)
                .fileName("main.sql")
                .constraints(new CodeExecutionConstraints(5000L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout()).contains("5000").contains("12502500");
    }

    @Test
    @DisplayName("Time Limit Exceeded: Infinite recursive CTE query terminated with TIME_LIMIT_EXCEEDED")
    void testSqliteTimeLimitExceeded() {
        Language sqlite = languageRegistry.get("sqlite-3");
        assumeLanguageImageAvailable(sqlite);

        String sql = """
                WITH RECURSIVE inf(x) AS (
                    SELECT 1
                    UNION ALL
                    SELECT x + 1 FROM inf
                )
                SELECT count(*) FROM inf;
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(sqlite)
                .code(sql)
                .fileName("main.sql")
                .constraints(new CodeExecutionConstraints(1000L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("Runtime Error: Syntax error returns RUNTIME_ERROR with database diagnostics")
    void testSqliteSyntaxError() {
        Language sqlite = languageRegistry.get("sqlite-3");
        assumeLanguageImageAvailable(sqlite);

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(sqlite)
                .code("SELEC * FROM non_existent_table;")
                .fileName("main.sql")
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
        assertThat(result.stderr()).contains("near \"SELEC\": syntax error");
    }

    @Test
    @DisplayName("Runtime Error: Table does not exist returns RUNTIME_ERROR")
    void testSqliteNoSuchTableError() {
        Language sqlite = languageRegistry.get("sqlite-3");
        assumeLanguageImageAvailable(sqlite);

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(sqlite)
                .code("SELECT * FROM non_existent_table;")
                .fileName("main.sql")
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
        assertThat(result.stderr()).contains("no such table: non_existent_table");
    }

    @Test
    @DisplayName("Batch Execution: Sequential query files persist database state across execution items")
    void testSqliteBatchStatePersistence() {
        Language sqlite = languageRegistry.get("sqlite-3");
        assumeLanguageImageAvailable(sqlite);

        String initSql = """
                CREATE TABLE employees (id INT, role TEXT);
                INSERT INTO employees VALUES (1, 'Developer');
                INSERT INTO employees VALUES (2, 'Manager');
                """;
        String q1 = "SELECT role FROM employees WHERE id = 1;";
        String q2 = "SELECT COUNT(*) FROM employees;";

        BatchCodeExecutionRequest request = BatchCodeExecutionRequest.builder()
                .language(sqlite)
                .files(Map.of(
                        "init.sql", initSql,
                        "query1.sql", q1,
                        "query2.sql", q2
                ))
                .constraints(new CodeExecutionConstraints(5000L))
                .executions(List.of(
                        BatchExecutionItem.builder().executablePath("init.sql").build(),
                        BatchExecutionItem.builder().executablePath("query1.sql").build(),
                        BatchExecutionItem.builder().executablePath("query2.sql").build()
                ))
                .build();

        BatchCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.totalExecutions()).isEqualTo(3);
        assertThat(result.successfulExecutions()).isEqualTo(3);
        assertThat(result.results().get(0).executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.results().get(1).stdout()).contains("Developer");
        assertThat(result.results().get(2).stdout().trim()).contains("2");
    }
}

