package com.bharath.stacked.modules.execution.integration;

import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import com.bharath.stacked.modules.execution.dto.request.SimpleCodeExecutionRequest;
import com.bharath.stacked.modules.execution.dto.response.SimpleCodeExecutionResult;
import com.bharath.stacked.modules.execution.enums.CodeExecutionStatus;
import com.bharath.stacked.modules.language.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MySQL 8.0 Live Integration Tests")
class MySqlExecutionIT extends BaseExecutionIT {

    @Test
    @DisplayName("Waits for MySQL socket readiness, executes query, and sanitizes CLI password warning")
    void executeMySqlSuccess() {
        Language mysql = languageRegistry.get("mysql-8.0");
        assumeLanguageImageAvailable(mysql);

        String sql = "SELECT 40 + 2 AS answer;";

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(mysql)
                .code(sql)
                .fileName("main.sql")
                .constraints(new CodeExecutionConstraints(10000L))
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.SUCCESS);
        // Verify stderr sanitization of mysql password warning
        if (result.stderr() != null) {
            assertThat(result.stderr()).doesNotContain("Using a password on the command line interface can be insecure");
        }
    }

    @Test
    @DisplayName("MySQL query timeout terminates SLEEP()")
    void executeMySqlTimeout() {
        Language mysql = languageRegistry.get("mysql-8.0");
        assumeLanguageImageAvailable(mysql);

        String sql = "SELECT SLEEP(10);";

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(mysql)
                .code(sql)
                .fileName("main.sql")
                .constraints(new CodeExecutionConstraints(1000L))
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.TIME_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("MySQL syntax error reports RUNTIME_ERROR")
    void executeMySqlSyntaxError() {
        Language mysql = languageRegistry.get("mysql-8.0");
        assumeLanguageImageAvailable(mysql);

        SimpleCodeExecutionRequest request = SimpleCodeExecutionRequest.builder()
                .language(mysql)
                .code("SELEC 1;")
                .fileName("main.sql")
                .build();

        SimpleCodeExecutionResult result = codeExecutionService.run(request);

        assertThat(result.executionStatus()).isEqualTo(CodeExecutionStatus.RUNTIME_ERROR);
    }
}

