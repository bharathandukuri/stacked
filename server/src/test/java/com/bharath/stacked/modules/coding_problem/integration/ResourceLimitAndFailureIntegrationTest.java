package com.bharath.stacked.modules.coding_problem.integration;

import com.bharath.stacked.modules.coding_problem.dto.request.SubmitCodingProblemRequest;
import com.bharath.stacked.modules.coding_problem.dto.response.CodingProblemExecutionResponse;
import com.bharath.stacked.modules.coding_problem.model.CodingProblem;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@DisplayName("Resource Limits & Runtime Failures Live Integration Tests")
class ResourceLimitAndFailureIntegrationTest extends BaseCodingProblemLiveIT {

    @Test
    @DisplayName("Python 3.12: Infinite loop triggers TIME_LIMIT_EXCEEDED status within configured timeout")
    void testPythonTimeLimitExceeded() {
        assumeImageAvailable("python-3.12");

        CodingProblem problem = createTwoSumProblem(randomSlug("py-tle"));

        String tleCode = """
                import sys

                # Infinite loop to exceed sandbox time limit
                while True:
                    pass
                """;

        SubmitCodingProblemRequest submitReq = SubmitCodingProblemRequest.builder()
                .languageId("python-3.12")
                .code(tleCode)
                .build();

        CodingProblemExecutionResponse response = codingProblemService.submitSolution(problem.getId(), submitReq);

        assertThat(response).isNotNull();
        assertThat(response.getOverallStatus()).isEqualTo("TIME_LIMIT_EXCEEDED");
        assertThat(response.getFailedTestcases()).isEqualTo(problem.getSampleTestCases().size() + problem.getHiddenTestCases().size());

        for (var res : response.getResults()) {
            assertThat(res.getStatus()).isEqualTo("TIME_LIMIT_EXCEEDED");
        }
    }

    @Test
    @DisplayName("Python 3.12: Uncaught runtime exception maps to RUNTIME_ERROR and captures exception message")
    void testPythonUnhandledException() {
        assumeImageAvailable("python-3.12");

        CodingProblem problem = createTwoSumProblem(randomSlug("py-runtime-err"));

        String crashingCode = """
                import sys
                raw = sys.stdin.read().split()
                # Deliberate ZeroDivisionError
                x = 10 / 0
                """;

        SubmitCodingProblemRequest submitReq = SubmitCodingProblemRequest.builder()
                .languageId("python-3.12")
                .code(crashingCode)
                .build();

        CodingProblemExecutionResponse response = codingProblemService.submitSolution(problem.getId(), submitReq);

        assertThat(response).isNotNull();
        assertThat(response.getOverallStatus()).isEqualTo("RUNTIME_ERROR");

        for (var res : response.getResults()) {
            assertThat(res.getStatus()).isEqualTo("RUNTIME_ERROR");
            assertThat(res.getStderr()).contains("ZeroDivisionError: division by zero");
        }
    }

    @Test
    @DisplayName("Java 21: NullPointerException maps to RUNTIME_ERROR with Java stacktrace details")
    void testJavaNullPointerException() {
        assumeImageAvailable("java-21");

        CodingProblem problem = createTwoSumProblem(randomSlug("java-npe"));

        String npeCode = """
                import java.util.*;

                public class Solution {
                    public static void main(String[] args) {
                        String s = null;
                        System.out.println(s.length()); // Triggers NullPointerException
                    }
                }
                """;

        SubmitCodingProblemRequest submitReq = SubmitCodingProblemRequest.builder()
                .languageId("java-21")
                .code(npeCode)
                .build();

        CodingProblemExecutionResponse response = codingProblemService.submitSolution(problem.getId(), submitReq);

        assertThat(response).isNotNull();
        assertThat(response.getOverallStatus()).isEqualTo("RUNTIME_ERROR");

        for (var res : response.getResults()) {
            assertThat(res.getStatus()).isEqualTo("RUNTIME_ERROR");
            assertThat(res.getStderr()).contains("NullPointerException");
        }
    }

    @Test
    @DisplayName("Python 3.12: Bounded computation that executes near limits still completes and reports ACCEPTED")
    void testNearLimitComputationSucceeds() {
        assumeImageAvailable("python-3.12");

        CodingProblem problem = createTwoSumProblem(randomSlug("py-near-limit"));

        // Computes answer after a reasonable busy loop (10,000 iterations), well within 2000ms limit
        String candidateCode = """
                import sys

                data = sys.stdin.read().split()
                if data:
                    # Bounded loop simulating computation overhead
                    total = 0
                    for k in range(10000):
                        total += k

                    n = int(data[0])
                    target = int(data[1])
                    nums = [int(x) for x in data[2:2+n]]
                    seen = {}
                    for i, val in enumerate(nums):
                        complement = target - val
                        if complement in seen:
                            print(f"{seen[complement]} {i}")
                            break
                        seen[val] = i
                """;

        SubmitCodingProblemRequest submitReq = SubmitCodingProblemRequest.builder()
                .languageId("python-3.12")
                .code(candidateCode)
                .build();

        CodingProblemExecutionResponse response = codingProblemService.submitSolution(problem.getId(), submitReq);

        assertThat(response).isNotNull();
        assertThat(response.getOverallStatus()).isEqualTo("ACCEPTED");
        assertThat(response.getPassedTestcases()).isEqualTo(5);
    }
}

