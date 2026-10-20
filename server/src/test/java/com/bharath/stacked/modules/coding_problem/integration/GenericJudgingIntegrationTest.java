package com.bharath.stacked.modules.coding_problem.integration;

import com.bharath.stacked.modules.coding_problem.dto.request.CustomTestcaseRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.RunSampleTestcasesRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.SubmitCodingProblemRequest;
import com.bharath.stacked.modules.coding_problem.dto.response.CodingProblemExecutionResponse;
import com.bharath.stacked.modules.coding_problem.model.CodingProblem;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@DisplayName("Generic Coding Problem Live Judging Integration Tests")
class GenericJudgingIntegrationTest extends BaseCodingProblemLiveIT {

    @Test
    @DisplayName("Python 3.12: Fully correct submission passes all sample and hidden test cases")
    void testPythonCorrectSubmission_PassesAllCases() {
        assumeImageAvailable("python-3.12");

        CodingProblem problem = createTwoSumProblem(randomSlug("py-correct"));

        String candidateCode = """
                import sys

                def solve():
                    data = sys.stdin.read().split()
                    if not data:
                        return
                    n = int(data[0])
                    target = int(data[1])
                    nums = [int(x) for x in data[2:2+n]]
                    seen = {}
                    for i, val in enumerate(nums):
                        complement = target - val
                        if complement in seen:
                            print(f"{seen[complement]} {i}")
                            return
                        seen[val] = i

                solve()
                """;

        SubmitCodingProblemRequest submitReq = SubmitCodingProblemRequest.builder()
                .languageId("python-3.12")
                .code(candidateCode)
                .build();

        CodingProblemExecutionResponse response = codingProblemService.submitSolution(problem.getId(), submitReq);

        assertThat(response).isNotNull();
        assertThat(response.getOverallStatus()).isEqualTo("ACCEPTED");
        assertThat(response.getTotalTestcases()).isEqualTo(5); // 2 sample + 3 hidden
        assertThat(response.getPassedTestcases()).isEqualTo(5);
        assertThat(response.getFailedTestcases()).isEqualTo(0);
        assertThat(response.getTotalExecutionTimeMs()).isGreaterThan(0L);

        // Verify all testcases are PASSED
        for (var res : response.getResults()) {
            assertThat(res.getStatus()).isEqualTo("PASSED");
            assertThat(res.getActualOutput()).isNotNull();
            assertThat(res.getExpectedOutput()).isNotNull();
            assertThat(res.getExitCode()).isEqualTo(0L);
            assertThat(res.getStderr()).isBlank();
        }
    }

    @Test
    @DisplayName("Java 21 (Compiled): Fully correct submission compiles, executes, and passes all test cases")
    void testJavaCorrectSubmission_PassesAllCases() {
        assumeImageAvailable("java-21");

        CodingProblem problem = createTwoSumProblem(randomSlug("java-correct"));

        String candidateCode = """
                import java.util.*;

                public class Solution {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        if (!sc.hasNextInt()) return;
                        int n = sc.nextInt();
                        int target = sc.nextInt();
                        int[] nums = new int[n];
                        for (int i = 0; i < n; i++) {
                            nums[i] = sc.nextInt();
                        }
                        Map<Integer, Integer> map = new HashMap<>();
                        for (int i = 0; i < n; i++) {
                            int complement = target - nums[i];
                            if (map.containsKey(complement)) {
                                System.out.println(map.get(complement) + " " + i);
                                return;
                            }
                            map.put(nums[i], i);
                        }
                    }
                }
                """;

        SubmitCodingProblemRequest submitReq = SubmitCodingProblemRequest.builder()
                .languageId("java-21")
                .code(candidateCode)
                .build();

        CodingProblemExecutionResponse response = codingProblemService.submitSolution(problem.getId(), submitReq);

        assertThat(response).isNotNull();
        assertThat(response.getOverallStatus()).isEqualTo("ACCEPTED");
        assertThat(response.getTotalTestcases()).isEqualTo(5);
        assertThat(response.getPassedTestcases()).isEqualTo(5);
        assertThat(response.getFailedTestcases()).isEqualTo(0);
    }

    @Test
    @DisplayName("JavaScript Node 20: Fully correct submission passes all test cases")
    void testJavaScriptCorrectSubmission_PassesAllCases() {
        assumeImageAvailable("javascript-node-20");

        CodingProblem problem = createTwoSumProblem(randomSlug("js-correct"));

        String candidateCode = """
                const fs = require('fs');
                const tokens = fs.readFileSync(0, 'utf-8').trim().split(/\\s+/);
                if (tokens.length >= 2) {
                    const n = parseInt(tokens[0], 10);
                    const target = parseInt(tokens[1], 10);
                    const nums = tokens.slice(2, 2 + n).map(x => parseInt(x, 10));
                    const map = new Map();
                    for (let i = 0; i < nums.length; i++) {
                        const comp = target - nums[i];
                        if (map.has(comp)) {
                            console.log(map.get(comp) + " " + i);
                            break;
                        }
                        map.set(nums[i], i);
                    }
                }
                """;

        SubmitCodingProblemRequest submitReq = SubmitCodingProblemRequest.builder()
                .languageId("javascript-node-20")
                .code(candidateCode)
                .build();

        CodingProblemExecutionResponse response = codingProblemService.submitSolution(problem.getId(), submitReq);

        assertThat(response).isNotNull();
        assertThat(response.getOverallStatus()).isEqualTo("ACCEPTED");
        assertThat(response.getPassedTestcases()).isEqualTo(5);
    }

    @Test
    @DisplayName("C++ 23 (Compiled): Fully correct submission compiles, executes, and passes all test cases")
    void testCppCorrectSubmission_PassesAllCases() {
        assumeImageAvailable("cpp-23");

        CodingProblem problem = createTwoSumProblem(randomSlug("cpp-correct"));

        String candidateCode = """
                #include <iostream>
                #include <vector>
                #include <unordered_map>
                using namespace std;

                int main() {
                    int n, target;
                    if (!(cin >> n >> target)) return 0;
                    vector<int> nums(n);
                    for (int i = 0; i < n; i++) cin >> nums[i];
                    unordered_map<int, int> seen;
                    for (int i = 0; i < n; i++) {
                        int comp = target - nums[i];
                        if (seen.find(comp) != seen.end()) {
                            cout << seen[comp] << " " << i << endl;
                            return 0;
                        }
                        seen[nums[i]] = i;
                    }
                    return 0;
                }
                """;

        SubmitCodingProblemRequest submitReq = SubmitCodingProblemRequest.builder()
                .languageId("cpp-23")
                .code(candidateCode)
                .build();

        CodingProblemExecutionResponse response = codingProblemService.submitSolution(problem.getId(), submitReq);

        assertThat(response).isNotNull();
        assertThat(response.getOverallStatus()).isEqualTo("ACCEPTED");
        assertThat(response.getPassedTestcases()).isEqualTo(5);
    }

    @Test
    @DisplayName("Partially Correct: Solution works for positive numbers but fails on negative numbers")
    void testPartiallyCorrectSubmission_FailsOnEdgeCase() {
        assumeImageAvailable("python-3.12");

        CodingProblem problem = createTwoSumProblem(randomSlug("py-partial"));

        // Buggy logic: rejects negative numbers
        String candidateCode = """
                import sys

                raw = sys.stdin.read().split()
                if raw:
                    n = int(raw[0])
                    target = int(raw[1])
                    nums = [int(x) for x in raw[2:2+n]]
                    seen = {}
                    for i, x in enumerate(nums):
                        if x < 0: # flawed assumption!
                            continue
                        comp = target - x
                        if comp in seen:
                            print(f"{seen[comp]} {i}")
                            break
                        seen[x] = i
                """;

        SubmitCodingProblemRequest submitReq = SubmitCodingProblemRequest.builder()
                .languageId("python-3.12")
                .code(candidateCode)
                .build();

        CodingProblemExecutionResponse response = codingProblemService.submitSolution(problem.getId(), submitReq);

        assertThat(response).isNotNull();
        assertThat(response.getOverallStatus()).isEqualTo("WRONG_ANSWER");
        // Sample cases pass, but Hidden Case 1 (with -3 and 3 adding to 0) fails!
        assertThat(response.getPassedTestcases()).isGreaterThanOrEqualTo(2);
        assertThat(response.getFailedTestcases()).isGreaterThanOrEqualTo(1);

        // Verify failed testcase reports WRONG_ANSWER
        boolean foundFailure = response.getResults().stream()
                .anyMatch(r -> "WRONG_ANSWER".equals(r.getStatus()));
        assertThat(foundFailure).isTrue();
    }

    @Test
    @DisplayName("Wrong Answer: Completely incorrect algorithm produces WRONG_ANSWER on all cases")
    void testWrongAnswerSubmission() {
        assumeImageAvailable("python-3.12");

        CodingProblem problem = createTwoSumProblem(randomSlug("py-wrong"));

        String candidateCode = """
                print("999 999")
                """;

        SubmitCodingProblemRequest submitReq = SubmitCodingProblemRequest.builder()
                .languageId("python-3.12")
                .code(candidateCode)
                .build();

        CodingProblemExecutionResponse response = codingProblemService.submitSolution(problem.getId(), submitReq);

        assertThat(response).isNotNull();
        assertThat(response.getOverallStatus()).isEqualTo("WRONG_ANSWER");
        assertThat(response.getPassedTestcases()).isEqualTo(0);
        assertThat(response.getFailedTestcases()).isEqualTo(5);

        for (var res : response.getResults()) {
            assertThat(res.getStatus()).isEqualTo("WRONG_ANSWER");
            assertThat(res.getActualOutput().trim()).isEqualTo("999 999");
            assertThat(res.getExpectedOutput()).isNotNull();
            assertThat(res.getExpectedOutput().trim()).isNotEqualTo("999 999");
        }
    }

    @Test
    @DisplayName("Verification Script: Accepts alternative valid answer and returns reference output when candidate is wrong")
    void testVerificationScriptJudging_AlternativeAnswersAndFailureFallback() {
        assumeImageAvailable("python-3.12");

        CodingProblem problem = createTwoSumWithVerifierProblem(randomSlug("py-verifier"));

        // 1. Candidate returns alternative valid pair (0, 2) which adds to 10 (nums[0]=2, nums[2]=8)
        String altValidCode = """
                # Prints alternative valid indices 0 2 (since 2 + 8 == 10)
                print("0 2")
                """;

        SubmitCodingProblemRequest submitReq1 = SubmitCodingProblemRequest.builder()
                .languageId("python-3.12")
                .code(altValidCode)
                .build();

        CodingProblemExecutionResponse res1 = codingProblemService.submitSolution(problem.getId(), submitReq1);
        assertThat(res1.getOverallStatus()).isEqualTo("ACCEPTED");
        assertThat(res1.getResults().get(0).getStatus()).isEqualTo("PASSED");
        assertThat(res1.getResults().get(0).getActualOutput().trim()).isEqualTo("0 2");

        // 2. Candidate returns invalid indices that do not sum to target
        String wrongCode = """
                print("0 3") # nums[0]=2, nums[3]=2 -> sum is 4 != 10
                """;

        SubmitCodingProblemRequest submitReq2 = SubmitCodingProblemRequest.builder()
                .languageId("python-3.12")
                .code(wrongCode)
                .build();

        CodingProblemExecutionResponse res2 = codingProblemService.submitSolution(problem.getId(), submitReq2);
        assertThat(res2.getOverallStatus()).isEqualTo("WRONG_ANSWER");

        var tcRes = res2.getResults().get(0);
        assertThat(tcRes.getStatus()).isEqualTo("WRONG_ANSWER");
        assertThat(tcRes.getActualOutput().trim()).isEqualTo("0 3");

        // USER INVARIANT: expectedOutput MUST be reference solution output, NEVER verifier assertion output!
        assertThat(tcRes.getExpectedOutput()).isNotNull();
        assertThat(tcRes.getExpectedOutput().trim()).isEqualTo("0 1"); // Reference solution produces 0 1
        assertThat(tcRes.getExpectedOutput()).doesNotContain("AssertionError");
    }

    @Test
    @DisplayName("runSampleTestcases: Computes expected output dynamically for candidate-provided custom testcase")
    void testRunSampleTestcases_WithCustomTestcase() {
        assumeImageAvailable("python-3.12");

        CodingProblem problem = createTwoSumProblem(randomSlug("py-sample-custom"));

        String candidateCode = """
                import sys

                raw = sys.stdin.read().split()
                if raw:
                    n = int(raw[0])
                    target = int(raw[1])
                    nums = [int(x) for x in raw[2:2+n]]
                    seen = {}
                    for i, val in enumerate(nums):
                        diff = target - val
                        if diff in seen:
                            print(f"{seen[diff]} {i}")
                            break
                        seen[val] = i
                """;

        RunSampleTestcasesRequest sampleReq = RunSampleTestcasesRequest.builder()
                .languageId("python-3.12")
                .code(candidateCode)
                .customTestcases(List.of(
                        new CustomTestcaseRequest("custom-1", "3 15\n5 10 20", null)
                ))
                .build();

        CodingProblemExecutionResponse response = codingProblemService.runSampleTestcases(problem.getId(), sampleReq);

        assertThat(response).isNotNull();
        assertThat(response.getOverallStatus()).isEqualTo("ACCEPTED");
        assertThat(response.getTotalTestcases()).isEqualTo(3); // 2 sample + 1 custom
        assertThat(response.getPassedTestcases()).isEqualTo(3);

        // Check custom testcase item
        var customResult = response.getResults().get(2);
        assertThat(customResult.getTestCaseId()).isEqualTo("custom-1");
        assertThat(customResult.getIsCustom()).isTrue();
        assertThat(customResult.getIsSample()).isFalse();
        assertThat(customResult.getStatus()).isEqualTo("PASSED");
        // Reference solution for 5 + 10 = 15 yields "0 1"
        assertThat(customResult.getExpectedOutput().trim()).isEqualTo("0 1");
        assertThat(customResult.getActualOutput().trim()).isEqualTo("0 1");
    }
}

