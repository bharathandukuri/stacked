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
@DisplayName("PostgreSQL 16 Comprehensive Execution Limit & Edge Case Tests")
public class PostgresExecutionLimitIT extends BaseExecutionIT {

    @Test
    @DisplayName("Normal Execution: Scalar query execution")
    void testPostgresNormalExecution() {
        Language postgres = languageRegistry.get("postgresql-16");
        assumeLanguageImageAvailable(postgres);

        String sql = "SELECT 1 + 1 AS result;";

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(postgres)
                .code(sql)
                .fileName("main.sql")
                .constraints(new CodeExecutionConstraints(5000L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout()).contains("result").contains("2");
    }

    @Test
    @DisplayName("Table Operations: Creates table, inserts data, and verifies aggregate")
    void testPostgresTableOperations() {
        Language postgres = languageRegistry.get("postgresql-16");
        assumeLanguageImageAvailable(postgres);

        String sql = """
                CREATE TABLE items (id SERIAL PRIMARY KEY, title VARCHAR(50), price NUMERIC);
                INSERT INTO items (title, price) VALUES ('Widget', 9.99), ('Gadget', 19.99);
                SELECT COUNT(*), AVG(price) FROM items;
                """;

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(postgres)
                .code(sql)
                .fileName("main.sql")
                .constraints(new CodeExecutionConstraints(5000L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout()).contains("2").contains("14.99");
    }

    @Test
    @DisplayName("Time Limit Exceeded: pg_sleep(10) terminated on 1000ms timeout")
    void testPostgresTimeLimitExceeded() {
        Language postgres = languageRegistry.get("postgresql-16");
        assumeLanguageImageAvailable(postgres);

        String sql = "SELECT pg_sleep(10);";

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(postgres)
                .code(sql)
                .fileName("main.sql")
                .constraints(new CodeExecutionConstraints(1000L))
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("Runtime Error: Syntax error returns RUNTIME_ERROR")
    void testPostgresSyntaxError() {
        Language postgres = languageRegistry.get("postgresql-16");
        assumeLanguageImageAvailable(postgres);

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(postgres)
                .code("SELECCT * FROM non_existent;")
                .fileName("main.sql")
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
        assertThat(result.stderr()).contains("syntax error");
    }

    @Test
    @DisplayName("Runtime Error: Non-existent relation returns RUNTIME_ERROR")
    void testPostgresNoSuchRelationError() {
        Language postgres = languageRegistry.get("postgresql-16");
        assumeLanguageImageAvailable(postgres);

        SimpleCodeExecutionResult result = codeExecutionService.run(SimpleCodeExecutionRequest.builder()
                .language(postgres)
                .code("SELECT * FROM non_existent_relation_xyz;")
                .fileName("main.sql")
                .build());

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
        assertThat(result.stderr()).contains("relation \"non_existent_relation_xyz\" does not exist");
    }

    @Test
    @DisplayName("Batch Execution: State persists across sequential queries in same container")
    void testPostgresBatchStatePersistence() {
        Language postgres = languageRegistry.get("postgresql-16");
        assumeLanguageImageAvailable(postgres);

        String createSql = """
                CREATE TABLE batch_test (id INT PRIMARY KEY, val TEXT);
                INSERT INTO batch_test VALUES (1, 'First');
                """;
        String insertSql = """
                INSERT INTO batch_test VALUES (2, 'Second');
                """;
        String selectSql = """
                SELECT string_agg(val, ', ' ORDER BY id) FROM batch_test;
                """;

        BatchCodeExecutionRequest request = BatchCodeExecutionRequest.builder()
                .language(postgres)
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
        assertThat(result.results().get(2).stdout()).contains("First, Second");
    }
}

