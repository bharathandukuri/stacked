package com.bharath.stacked.modules.coding_problem.integration;

import com.bharath.stacked.modules.coding_problem.dto.request.SubmitCodingProblemRequest;
import com.bharath.stacked.modules.coding_problem.dto.response.CodingProblemExecutionResponse;
import com.bharath.stacked.modules.coding_problem.model.CodingProblem;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@DisplayName("Database Coding Problem Live Judging Integration Tests")
class DatabaseJudgingIntegrationTest extends BaseCodingProblemLiveIT {

    @Test
    @DisplayName("SQLite 3: Fully correct SQL query passes all test cases (duplicates, empty table, NULL dept)")
    void testSqliteCorrectSubmission_PassesAllCases() {
        assumeImageAvailable("sqlite-3");

        CodingProblem problem = createEmployeeSalarySqliteProblem(randomSlug("sql-correct"));

        String correctQuery = """
                SELECT d.name AS Department, e.name AS Employee, e.salary AS Salary
                FROM employee e
                JOIN department d ON e.department_id = d.id
                WHERE e.salary = (
                    SELECT MAX(e2.salary)
                    FROM employee e2
                    WHERE e2.department_id = e.department_id
                )
                ORDER BY Department ASC, Employee ASC;
                """;

        SubmitCodingProblemRequest submitReq = SubmitCodingProblemRequest.builder()
                .languageId("sqlite-3")
                .code(correctQuery)
                .build();

        CodingProblemExecutionResponse response = codingProblemService.submitSolution(problem.getId(), submitReq);

        assertThat(response).isNotNull();
        assertThat(response.getOverallStatus()).isEqualTo("ACCEPTED");
        assertThat(response.getTotalTestcases()).isEqualTo(4); // 2 sample + 2 hidden
        assertThat(response.getPassedTestcases()).isEqualTo(4);
        assertThat(response.getFailedTestcases()).isEqualTo(0);

        for (var res : response.getResults()) {
            assertThat(res.getStatus()).isEqualTo("PASSED");
            assertThat(res.getActualOutput()).isNotNull();
            assertThat(res.getExpectedOutput()).isNotNull();
            assertThat(res.getExitCode()).isEqualTo(0L);
        }
    }

    @Test
    @DisplayName("SQLite 3: Query with incorrect logic (e.g. MIN instead of MAX) returns WRONG_ANSWER")
    void testSqliteWrongLogicSubmission_ReturnsWrongAnswer() {
        assumeImageAvailable("sqlite-3");

        CodingProblem problem = createEmployeeSalarySqliteProblem(randomSlug("sql-wrong"));

        // Buggy: returns MIN salary instead of MAX
        String wrongQuery = """
                SELECT d.name AS Department, e.name AS Employee, e.salary AS Salary
                FROM employee e
                JOIN department d ON e.department_id = d.id
                WHERE e.salary = (
                    SELECT MIN(e2.salary)
                    FROM employee e2
                    WHERE e2.department_id = e.department_id
                )
                ORDER BY Department ASC, Employee ASC;
                """;

        SubmitCodingProblemRequest submitReq = SubmitCodingProblemRequest.builder()
                .languageId("sqlite-3")
                .code(wrongQuery)
                .build();

        CodingProblemExecutionResponse response = codingProblemService.submitSolution(problem.getId(), submitReq);

        assertThat(response).isNotNull();
        assertThat(response.getOverallStatus()).isEqualTo("WRONG_ANSWER");
        assertThat(response.getFailedTestcases()).isGreaterThan(0);

        var firstFailed = response.getResults().stream()
                .filter(r -> "WRONG_ANSWER".equals(r.getStatus()))
                .findFirst();
        assertThat(firstFailed).isPresent();
        assertThat(firstFailed.get().getActualOutput()).isNotEqualTo(firstFailed.get().getExpectedOutput());
    }

    @Test
    @DisplayName("SQLite 3: SQL Syntax error produces RUNTIME_ERROR status with engine diagnostics")
    void testSqliteSyntaxErrorSubmission_ReturnsRuntimeError() {
        assumeImageAvailable("sqlite-3");

        CodingProblem problem = createEmployeeSalarySqliteProblem(randomSlug("sql-syntax-err"));

        String syntaxErrorQuery = "SELECT * FORM employee;";

        SubmitCodingProblemRequest submitReq = SubmitCodingProblemRequest.builder()
                .languageId("sqlite-3")
                .code(syntaxErrorQuery)
                .build();

        CodingProblemExecutionResponse response = codingProblemService.submitSolution(problem.getId(), submitReq);

        assertThat(response).isNotNull();
        assertThat(response.getOverallStatus()).isEqualTo("RUNTIME_ERROR");
        assertThat(response.getFailedTestcases()).isEqualTo(4);

        for (var res : response.getResults()) {
            assertThat(res.getStatus()).isEqualTo("RUNTIME_ERROR");
            assertThat(res.getStderr()).containsIgnoringCase("syntax error");
            assertThat(res.getError()).isNotBlank();
        }
    }

    @Test
    @DisplayName("PostgreSQL 16: Correct query using LEFT JOIN, GROUP BY, and COALESCE passes judging")
    void testPostgresCorrectSubmission_PassesAllCases() {
        assumeImageAvailable("postgresql-16");

        CodingProblem problem = createCustomerOrdersPostgresProblem(randomSlug("pg-correct"));

        String candidateSql = """
                SELECT c.name, COALESCE(SUM(o.amount), 0) AS total_spent
                FROM customers c
                LEFT JOIN orders o ON c.id = o.customer_id AND o.status = 'COMPLETED'
                GROUP BY c.id, c.name
                ORDER BY total_spent DESC, c.name ASC;
                """;

        SubmitCodingProblemRequest submitReq = SubmitCodingProblemRequest.builder()
                .languageId("postgresql-16")
                .code(candidateSql)
                .build();

        CodingProblemExecutionResponse response = codingProblemService.submitSolution(problem.getId(), submitReq);

        assertThat(response).isNotNull();
        assertThat(response.getOverallStatus()).isEqualTo("ACCEPTED");
        assertThat(response.getTotalTestcases()).isEqualTo(2); // 1 sample + 1 hidden
        assertThat(response.getPassedTestcases()).isEqualTo(2);
        assertThat(response.getFailedTestcases()).isEqualTo(0);
    }
}

