package com.bharath.stacked.modules.coding_problem.integration;

import com.bharath.stacked.exception.BadRequestException;
import com.bharath.stacked.modules.coding_problem.dto.request.CreateCodingProblemRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.ExecutionConstraintRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.SubmitCodingProblemRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.TestcaseRequest;
import com.bharath.stacked.modules.coding_problem.dto.response.CodingProblemExecutionResponse;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemDifficulty;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemType;
import com.bharath.stacked.modules.coding_problem.model.CodingProblem;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
@DisplayName("Verification Script Live Judging Integration Tests")
class VerificationScriptIntegrationTest extends BaseCodingProblemLiveIT {

    @Test
    @DisplayName("Cross-Language: Java 21 candidate judged by Python 3.12 verification script passes")
    void testCrossLanguage_JavaCandidate_WithPythonVerifier() {
        assumeImageAvailable("python-3.12");
        assumeImageAvailable("java-21");

        CodingProblem problem = createTwoSumWithVerifierProblem(randomSlug("ver-java"));

        // Java solution returning alternative valid indices (0, 2)
        String javaCandidateCode = """
                public class Solution {
                    public static void main(String[] args) {
                        System.out.println("0 2");
                    }
                }
                """;

        SubmitCodingProblemRequest req = SubmitCodingProblemRequest.builder()
                .languageId("java-21")
                .code(javaCandidateCode)
                .build();

        CodingProblemExecutionResponse response = codingProblemService.submitSolution(problem.getId(), req);

        assertThat(response).isNotNull();
        // Sample case 1 has (0, 2) as valid (2 + 8 == 10)
        var firstResult = response.getResults().get(0);
        assertThat(firstResult.getStatus()).isEqualTo("PASSED");
        assertThat(firstResult.getActualOutput().trim()).isEqualTo("0 2");
    }

    @Test
    @DisplayName("Cross-Language: C++ 23 candidate judged by Python 3.12 verification script passes")
    void testCrossLanguage_CppCandidate_WithPythonVerifier() {
        assumeImageAvailable("python-3.12");
        assumeImageAvailable("cpp-23");

        CodingProblem problem = createTwoSumWithVerifierProblem(randomSlug("ver-cpp"));

        // C++ solution returning valid indices
        String cppCandidateCode = """
                #include <iostream>
                using namespace std;
                int main() {
                    cout << "0 2" << endl;
                    return 0;
                }
                """;

        SubmitCodingProblemRequest req = SubmitCodingProblemRequest.builder()
                .languageId("cpp-23")
                .code(cppCandidateCode)
                .build();

        CodingProblemExecutionResponse response = codingProblemService.submitSolution(problem.getId(), req);

        assertThat(response).isNotNull();
        var firstResult = response.getResults().get(0);
        assertThat(firstResult.getStatus()).isEqualTo("PASSED");
        assertThat(firstResult.getActualOutput().trim()).isEqualTo("0 2");
    }

    @Test
    @DisplayName("Cross-Language: JavaScript Node 20 candidate judged by Python 3.12 verification script passes")
    void testCrossLanguage_JsCandidate_WithPythonVerifier() {
        assumeImageAvailable("python-3.12");
        assumeImageAvailable("javascript-node-20");

        CodingProblem problem = createTwoSumWithVerifierProblem(randomSlug("ver-js"));

        String jsCandidateCode = """
                console.log("0 2");
                """;

        SubmitCodingProblemRequest req = SubmitCodingProblemRequest.builder()
                .languageId("javascript-node-20")
                .code(jsCandidateCode)
                .build();

        CodingProblemExecutionResponse response = codingProblemService.submitSolution(problem.getId(), req);

        assertThat(response).isNotNull();
        var firstResult = response.getResults().get(0);
        assertThat(firstResult.getStatus()).isEqualTo("PASSED");
        assertThat(firstResult.getActualOutput().trim()).isEqualTo("0 2");
    }

    @Test
    @DisplayName("Floating Point Tolerance: Candidate output within 1e-4 tolerance is ACCEPTED")
    void testFloatingPointTolerance_WithinEpsilon_Accepted() {
        assumeImageAvailable("python-3.12");

        CodingProblem problem = createFloatToleranceVerifierProblem(randomSlug("ver-float-ok"));

        // Euclidean distance for (0,0) to (3,4) is 5.0. Outputting 5.0000001 is within 1e-4
        String candidateCode = """
                import sys, math
                data = [float(x) for x in sys.stdin.read().split()]
                if data:
                    d = math.hypot(data[2]-data[0], data[3]-data[1])
                    print(f"{d + 0.00001:.7f}")
                """;

        SubmitCodingProblemRequest req = SubmitCodingProblemRequest.builder()
                .languageId("python-3.12")
                .code(candidateCode)
                .build();

        CodingProblemExecutionResponse response = codingProblemService.submitSolution(problem.getId(), req);

        assertThat(response).isNotNull();
        assertThat(response.getOverallStatus()).isEqualTo("ACCEPTED");
        assertThat(response.getPassedTestcases()).isEqualTo(3);
    }

    @Test
    @DisplayName("Floating Point Tolerance: Candidate output exceeding 1e-4 tolerance returns WRONG_ANSWER with reference output")
    void testFloatingPointTolerance_OutsideEpsilon_Rejected() {
        assumeImageAvailable("python-3.12");

        CodingProblem problem = createFloatToleranceVerifierProblem(randomSlug("ver-float-err"));

        // Off by 0.05, well above 1e-4 threshold
        String candidateCode = """
                import sys, math
                data = [float(x) for x in sys.stdin.read().split()]
                if data:
                    d = math.hypot(data[2]-data[0], data[3]-data[1])
                    print(f"{d + 0.05:.4f}")
                """;

        SubmitCodingProblemRequest req = SubmitCodingProblemRequest.builder()
                .languageId("python-3.12")
                .code(candidateCode)
                .build();

        CodingProblemExecutionResponse response = codingProblemService.submitSolution(problem.getId(), req);

        assertThat(response).isNotNull();
        assertThat(response.getOverallStatus()).isEqualTo("WRONG_ANSWER");
        assertThat(response.getFailedTestcases()).isEqualTo(3);

        for (var res : response.getResults()) {
            assertThat(res.getStatus()).isEqualTo("WRONG_ANSWER");
            assertThat(res.getExpectedOutput()).isNotNull();
            // Expected output is reference solution output (e.g. 5.000000), not verifier assertion text!
            assertThat(res.getExpectedOutput().trim()).startsWith("5.");
            assertThat(res.getExpectedOutput()).doesNotContain("AssertionError");
        }
    }

    @Test
    @DisplayName("Order-Agnostic Output: Factors output in arbitrary permutation is ACCEPTED by verifier")
    void testOrderAgnosticOutput_PermutationAccepted() {
        assumeImageAvailable("python-3.12");

        CodingProblem problem = createOrderAgnosticVerifierProblem(randomSlug("ver-order-ok"));

        // Factors of 60 are 2, 3, 5. Output in descending order: 5 3 2
        String candidateCode = """
                import sys
                raw = sys.stdin.read().strip()
                if raw:
                    n = int(raw)
                    # Correct algorithm but outputs factors sorted in reverse
                    d = 2
                    factors = []
                    while d * d <= n:
                        if n % d == 0:
                            factors.append(d)
                            while n % d == 0:
                                n //= d
                        d += 1
                    if n > 1:
                        factors.append(n)
                    factors.reverse() # descending order
                    print(" ".join(str(x) for x in factors))
                """;

        SubmitCodingProblemRequest req = SubmitCodingProblemRequest.builder()
                .languageId("python-3.12")
                .code(candidateCode)
                .build();

        CodingProblemExecutionResponse response = codingProblemService.submitSolution(problem.getId(), req);

        assertThat(response).isNotNull();
        assertThat(response.getOverallStatus()).isEqualTo("ACCEPTED");
        assertThat(response.getPassedTestcases()).isEqualTo(3);
    }

    @Test
    @DisplayName("Order-Agnostic Output: Missing prime factor causes verification assertion failure and WRONG_ANSWER")
    void testOrderAgnosticOutput_MissingFactor_Rejected() {
        assumeImageAvailable("python-3.12");

        CodingProblem problem = createOrderAgnosticVerifierProblem(randomSlug("ver-order-err"));

        // Drops the last prime factor
        String candidateCode = """
                import sys
                raw = sys.stdin.read().strip()
                if raw:
                    print("2") # Only prints 2, missing other prime factors
                """;

        SubmitCodingProblemRequest req = SubmitCodingProblemRequest.builder()
                .languageId("python-3.12")
                .code(candidateCode)
                .build();

        CodingProblemExecutionResponse response = codingProblemService.submitSolution(problem.getId(), req);

        assertThat(response).isNotNull();
        assertThat(response.getOverallStatus()).isEqualTo("WRONG_ANSWER");

        var firstResult = response.getResults().get(0);
        assertThat(firstResult.getStatus()).isEqualTo("WRONG_ANSWER");
        assertThat(firstResult.getActualOutput().trim()).isEqualTo("2");
        assertThat(firstResult.getExpectedOutput().trim()).isEqualTo("2 3 5");
    }

    @Test
    @DisplayName("Database Verification Script (SQLite 3): Correct UPDATE query modifies table matching SQL verifier")
    void testDatabase_SqliteUpdateQuery_WithSqlVerifier_Accepted() {
        assumeImageAvailable("sqlite-3");

        CodingProblem problem = createSalaryUpdateSqliteVerifierProblem(randomSlug("ver-sql-ok"));

        String candidateSql = """
                UPDATE employee SET salary = salary * 1.10 WHERE department_id = 1;
                """;

        SubmitCodingProblemRequest req = SubmitCodingProblemRequest.builder()
                .languageId("sqlite-3")
                .code(candidateSql)
                .build();

        CodingProblemExecutionResponse response = codingProblemService.submitSolution(problem.getId(), req);

        assertThat(response).isNotNull();
        assertThat(response.getOverallStatus()).isEqualTo("ACCEPTED");
        assertThat(response.getTotalTestcases()).isEqualTo(2);
        assertThat(response.getPassedTestcases()).isEqualTo(2);

        for (var res : response.getResults()) {
            assertThat(res.getStatus()).isEqualTo("PASSED");
            assertThat(res.getActualOutput()).isNotNull();
            assertThat(res.getExpectedOutput()).isNotNull();
            assertThat(res.getActualOutput().trim()).isEqualTo(res.getExpectedOutput().trim());
        }
    }

    @Test
    @DisplayName("Database Verification Script (SQLite 3): Incorrect UPDATE query causes table state mismatch and WRONG_ANSWER")
    void testDatabase_SqliteUpdateQuery_WithSqlVerifier_WrongDepartment_Rejected() {
        assumeImageAvailable("sqlite-3");

        CodingProblem problem = createSalaryUpdateSqliteVerifierProblem(randomSlug("ver-sql-err"));

        // Buggy SQL: updates department 2 instead of department 1!
        String candidateSql = """
                UPDATE employee SET salary = salary * 1.10 WHERE department_id = 2;
                """;

        SubmitCodingProblemRequest req = SubmitCodingProblemRequest.builder()
                .languageId("sqlite-3")
                .code(candidateSql)
                .build();

        CodingProblemExecutionResponse response = codingProblemService.submitSolution(problem.getId(), req);

        assertThat(response).isNotNull();
        assertThat(response.getOverallStatus()).isEqualTo("WRONG_ANSWER");
        assertThat(response.getFailedTestcases()).isGreaterThan(0);

        var firstResult = response.getResults().get(0);
        assertThat(firstResult.getStatus()).isEqualTo("WRONG_ANSWER");
        assertThat(firstResult.getActualOutput()).isNotEqualTo(firstResult.getExpectedOutput());
    }

    @Test
    @DisplayName("Problem Creation Validation: Problem with failing verification script is rejected with BadRequestException")
    void testProblemCreation_WithFailingVerificationScript_Rejected() {
        assumeImageAvailable("python-3.12");

        String brokenVerifier = """
                import sys
                # Deliberate failure regardless of input
                sys.exit(1)
                """;

        CreateCodingProblemRequest req = new CreateCodingProblemRequest(
                "Broken Verifier Problem",
                randomSlug("broken-verifier"),
                CodingProblemType.GENERIC,
                CodingProblemDifficulty.EASY,
                List.of("python-3.12"),
                "A problem with an invalid verifier script.",
                List.of("Math"),
                List.of(),
                Map.of("python-3.12", "print('hello')\n"),
                "python-3.12",
                "print('hello')\n",
                true,
                "python-3.12",
                brokenVerifier,
                List.of(new TestcaseRequest("test", null)),
                List.of(new TestcaseRequest("test2", null)),
                Map.of("python-3.12", new ExecutionConstraintRequest(2000L, 262144L)),
                null,
                null,
                null,
                null,
                List.of()
        );

        assertThatThrownBy(() -> codingProblemService.create(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Verification script");
    }
}
