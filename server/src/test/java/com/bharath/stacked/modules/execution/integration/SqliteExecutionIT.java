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

@DisplayName("SQLite 3 Live Integration Tests")
class SqliteExecutionIT extends BaseExecutionIT {

    @Test
    @DisplayName("Executes SQLite 3 DDL, INSERT, and SELECT queries")
    void executeSqlite3Success() {
        Language sqlite = languageRegistry.get("sqlite-3");
        assumeLanguageImageAvailable(sqlite);

        String sql = """
                CREATE TABLE products (id INTEGER PRIMARY KEY, name TEXT, price REAL);
                INSERT INTO products VALUES (1, 'Book', 12.50);
                INSERT INTO products VALUES (2, 'Pen', 1.50);
                SELECT name, price FROM products ORDER BY id;
                """;

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(sqlite)
                .code(sql)
                .fileName("main.sql")
                .constraints(new CodeExecutionConstraints(5000L))
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout()).contains("Book").contains("12.5");
        assertThat(result.stdout()).contains("Pen").contains("1.5");
    }

    @Test
    @DisplayName("SQLite 3 batch execution executes multiple query files sequentially without stdin substitution")
    void executeSqlite3BatchSuccess() {
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

    @Test
    @DisplayName("SQLite 3 syntax error returns RUNTIME_ERROR with database diagnostics")
    void executeSqlite3SyntaxError() {
        Language sqlite = languageRegistry.get("sqlite-3");
        assumeLanguageImageAvailable(sqlite);

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(sqlite)
                .code("SELEC * FROM non_existent_table;")
                .fileName("main.sql")
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
        assertThat(result.stderr()).contains("syntax error");
    }
}

