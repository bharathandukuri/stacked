package com.bharath.stacked.modules.coding_problem.integration;

import com.bharath.stacked.exception.ResourceNotFoundException;
import com.bharath.stacked.modules.coding_problem.dto.request.CustomTestcaseRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.RunSampleTestcasesRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.SubmitCodingProblemRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.TestcaseRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.UpdateCodingProblemRequest;
import com.bharath.stacked.modules.coding_problem.dto.response.CodingProblemExecutionResponse;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemDifficulty;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemStatus;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemType;
import com.bharath.stacked.modules.coding_problem.model.CodingProblem;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
@DisplayName("Full Judging & Problem Lifecycle Live Integration Tests")
class FullJudgingLifecycleIntegrationTest extends BaseCodingProblemLiveIT {

    @Test
    @DisplayName("Complete Lifecycle: Create -> Verify -> Candidate Run -> Submit -> Update -> Publish/Unpublish -> Delete")
    void testCompleteJudgingAndProblemLifecycle() {
        assumeImageAvailable("python-3.12");

        String slug = randomSlug("full-lifecycle");

        // 1. Create Problem via actual service method
        CodingProblem problem = createTwoSumProblem(slug);
        assertThat(problem).isNotNull();
        assertThat(problem.getId()).isNotBlank();
        assertThat(problem.getStatus()).isEqualTo(CodingProblemStatus.UNPUBLISHED);
        assertThat(problem.getSampleTestCases()).hasSize(2);
        assertThat(problem.getHiddenTestCases()).hasSize(3);

        // Verify reference solution outputs were initialized during problem creation
        for (var tc : problem.getSampleTestCases()) {
            assertThat(tc.getOutput()).isNotBlank();
        }
        for (var tc : problem.getHiddenTestCases()) {
            assertThat(tc.getOutput()).isNotBlank();
        }

        // 2. Fetch Problem Details
        CodingProblem detailsById = codingProblemService.getProblemDetails(problem.getId());
        CodingProblem detailsBySlug = codingProblemService.findByIdOrSlug(slug);
        assertThat(detailsById.getId()).isEqualTo(problem.getId());
        assertThat(detailsBySlug.getId()).isEqualTo(problem.getId());

        // 3. Candidate Run Sample Testcases + Custom Testcase
        String candidateCode = """
                import sys

                data = sys.stdin.read().split()
                if data:
                    n = int(data[0])
                    target = int(data[1])
                    nums = [int(x) for x in data[2:2+n]]
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
                        new CustomTestcaseRequest("my-custom-tc", "3 20\n5 15 25", null)
                ))
                .build();

        CodingProblemExecutionResponse sampleResponse = codingProblemService.runSampleTestcases(problem.getId(), sampleReq);
        assertThat(sampleResponse.getOverallStatus()).isEqualTo("ACCEPTED");
        assertThat(sampleResponse.getTotalTestcases()).isEqualTo(3);
        assertThat(sampleResponse.getPassedTestcases()).isEqualTo(3);

        // Security check: Verify no internal reference solution leaked in response
        for (var r : sampleResponse.getResults()) {
            assertThat(r.getActualOutput()).isNotEqualTo(problem.getReferenceSolution());
        }

        // 4. Candidate Full Submission
        SubmitCodingProblemRequest submitReq = SubmitCodingProblemRequest.builder()
                .languageId("python-3.12")
                .code(candidateCode)
                .build();

        CodingProblemExecutionResponse submitResponse = codingProblemService.submitSolution(slug, submitReq);
        assertThat(submitResponse.getOverallStatus()).isEqualTo("ACCEPTED");
        assertThat(submitResponse.getTotalTestcases()).isEqualTo(5);
        assertThat(submitResponse.getPassedTestcases()).isEqualTo(5);
        assertThat(submitResponse.getFailedTestcases()).isEqualTo(0);

        // 5. Update Problem: Change difficulty and add statement notes
        String updatedSlug = slug + "-updated";
        UpdateCodingProblemRequest updateReq = new UpdateCodingProblemRequest(
                problem.getTitle() + " Updated",
                updatedSlug,
                CodingProblemType.GENERIC,
                CodingProblemDifficulty.MEDIUM,
                problem.getSupportedLanguageIds(),
                problem.getStatement() + " [Updated in test]",
                problem.getTopics(),
                problem.getHints(),
                problem.getStarterCode(),
                problem.getReferenceSolutionLanguage(),
                problem.getReferenceSolution(),
                problem.isRequireVerificationScript(),
                problem.getVerificationScriptLanguage(),
                problem.getVerificationScript(),
                List.of(new TestcaseRequest("4 9\n2 7 11 15", null)),
                List.of(new TestcaseRequest("2 6\n3 3", null)),
                null,
                null,
                null,
                null,
                null,
                List.of()
        );

        CodingProblem updatedProblem = codingProblemService.update(problem.getId(), updateReq);
        assertThat(updatedProblem.getSlug()).isEqualTo(updatedSlug);
        assertThat(updatedProblem.getDifficulty()).isEqualTo(CodingProblemDifficulty.MEDIUM);
        assertThat(updatedProblem.getStatement()).contains("[Updated in test]");

        // 6. Check Slug Availability
        assertThat(codingProblemService.isSlugAvailable(updatedSlug, problem.getId())).isTrue();
        assertThat(codingProblemService.isSlugAvailable(updatedSlug, "other-id")).isFalse();
        assertThat(codingProblemService.isSlugAvailable("brand-new-slug-" + UUID.randomUUID())).isTrue();

        // 7. Publish and Unpublish
        CodingProblem published = codingProblemService.publish(problem.getId());
        assertThat(published.getStatus()).isEqualTo(CodingProblemStatus.PUBLISHED);

        CodingProblem unpublished = codingProblemService.unpublish(problem.getId());
        assertThat(unpublished.getStatus()).isEqualTo(CodingProblemStatus.UNPUBLISHED);

        // 8. Delete Problem
        codingProblemService.delete(problem.getId());

        // Verify problem is removed from repository
        assertThatThrownBy(() -> codingProblemService.getProblemDetails(problem.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(codingProblemRepository.findById(problem.getId())).isEmpty();

        // Remove from teardown list since it's already deleted
        createdProblemIds.remove(problem.getId());
    }
}
