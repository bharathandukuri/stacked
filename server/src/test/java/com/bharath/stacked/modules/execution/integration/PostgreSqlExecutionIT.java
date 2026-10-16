package com.bharath.stacked.modules.execution.integration;

import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import com.bharath.stacked.modules.execution.dto.request.SimpleCodeExecutionRequest;
import com.bharath.stacked.modules.execution.dto.response.SimpleCodeExecutionResult;
import com.bharath.stacked.modules.execution.enums.CodeExecutionStatus;
import com.bharath.stacked.modules.language.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PostgreSQL 16 Live Integration Tests")
class PostgreSqlExecutionIT extends BaseExecutionIT {

    @Test
    @DisplayName("Waits for PostgreSQL daemon readiness and executes query")
    void executePostgreSqlSuccess() {
        Language postgres = languageRegistry.get("postgresql-16");
        assumeLanguageImageAvailable(postgres);

        String sql = "SELECT 1 + 1 AS result;";

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(postgres)
                .code(sql)
                .fileName("main.sql")
                .constraints(new CodeExecutionConstraints(5000L))
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        assertThat(result.stdout()).contains("result").contains("2");
    }

    @Test
    @DisplayName("PostgreSQL query timeout terminates long-running pg_sleep")
    void executePostgreSqlTimeout() {
        Language postgres = languageRegistry.get("postgresql-16");
        assumeLanguageImageAvailable(postgres);

        String sql = "SELECT pg_sleep(10);";

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(postgres)
                .code(sql)
                .fileName("main.sql")
                .constraints(new CodeExecutionConstraints(1000L))
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("PostgreSQL syntax error returns RUNTIME_ERROR")
    void executePostgreSqlSyntaxError() {
        Language postgres = languageRegistry.get("postgresql-16");
        assumeLanguageImageAvailable(postgres);

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(postgres)
                .code("SELEC * FROM non_existent;")
                .fileName("main.sql")
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
        assertThat(result.stderr()).contains("syntax error");
    }
}

