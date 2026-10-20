package com.bharath.stacked.modules.coding_problem.integration;

import com.bharath.stacked.exception.BadRequestException;
import com.bharath.stacked.modules.coding_problem.dto.request.SubmitCodingProblemRequest;
import com.bharath.stacked.modules.coding_problem.dto.response.CodingProblemExecutionResponse;
import com.bharath.stacked.modules.coding_problem.model.CodingProblem;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
@DisplayName("Language Compilation & Validation Live Integration Tests")
class LanguageAndCompilationIntegrationTest extends BaseCodingProblemLiveIT {

    @Test
    @DisplayName("Java 21: Compilation failure marks overallStatus as COMPILATION_ERROR and captures javac stderr")
    void testJavaCompilationError() {
        assumeImageAvailable("java-21");

        CodingProblem problem = createTwoSumProblem(randomSlug("java-comp-err"));

        String invalidJava = """
                public class Solution {
                    syntax error here invalid
                }
                """;

        SubmitCodingProblemRequest submitReq = SubmitCodingProblemRequest.builder()
                .languageId("java-21")
                .code(invalidJava)
                .build();

        CodingProblemExecutionResponse response = codingProblemService.submitSolution(problem.getId(), submitReq);

        assertThat(response).isNotNull();
        assertThat(response.getOverallStatus()).isEqualTo("COMPILATION_ERROR");
        assertThat(response.getPassedTestcases()).isEqualTo(0);
        assertThat(response.getFailedTestcases()).isEqualTo(problem.getSampleTestCases().size() + problem.getHiddenTestCases().size());

        for (var res : response.getResults()) {
            assertThat(res.getStatus()).isEqualTo("COMPILATION_ERROR");
            assertThat(res.getStderr()).containsIgnoringCase("error:");
        }
    }

    @Test
    @DisplayName("C++ 23: Compilation failure marks overallStatus as COMPILATION_ERROR and captures g++ stderr")
    void testCppCompilationError() {
        assumeImageAvailable("cpp-23");

        CodingProblem problem = createTwoSumProblem(randomSlug("cpp-comp-err"));

        String invalidCpp = """
                #include <iostream>
                int main() {
                    this_is_not_valid_cpp syntax error
                    return 0;
                }
                """;

        SubmitCodingProblemRequest submitReq = SubmitCodingProblemRequest.builder()
                .languageId("cpp-23")
                .code(invalidCpp)
                .build();

        CodingProblemExecutionResponse response = codingProblemService.submitSolution(problem.getId(), submitReq);

        assertThat(response).isNotNull();
        assertThat(response.getOverallStatus()).isEqualTo("COMPILATION_ERROR");

        for (var res : response.getResults()) {
            assertThat(res.getStatus()).isEqualTo("COMPILATION_ERROR");
            assertThat(res.getStderr()).containsIgnoringCase("error:");
        }
    }

    @Test
    @DisplayName("Python 3.12: Syntax error at parse time yields RUNTIME_ERROR with SyntaxError details")
    void testPythonSyntaxError() {
        assumeImageAvailable("python-3.12");

        CodingProblem problem = createTwoSumProblem(randomSlug("py-syntax-err"));

        String invalidPython = """
                def broken_function(:
                    pass
                """;

        SubmitCodingProblemRequest submitReq = SubmitCodingProblemRequest.builder()
                .languageId("python-3.12")
                .code(invalidPython)
                .build();

        CodingProblemExecutionResponse response = codingProblemService.submitSolution(problem.getId(), submitReq);

        assertThat(response).isNotNull();
        assertThat(response.getOverallStatus()).isEqualTo("RUNTIME_ERROR");

        for (var res : response.getResults()) {
            assertThat(res.getStatus()).isEqualTo("RUNTIME_ERROR");
            assertThat(res.getStderr()).contains("SyntaxError");
        }
    }

    @Test
    @DisplayName("Validation: Submitting language not supported by the problem throws BadRequestException")
    void testUnsupportedLanguageSubmission_ThrowsBadRequestException() {
        CodingProblem problem = createTwoSumProblem(randomSlug("unsupported-lang"));

        SubmitCodingProblemRequest submitReq = SubmitCodingProblemRequest.builder()
                .languageId("c-17") // c-17 was not added to supportedLanguageIds for this problem
                .code("int main() { return 0; }")
                .build();

        assertThatThrownBy(() -> codingProblemService.submitSolution(problem.getId(), submitReq))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not supported by this problem");
    }

    @Test
    @DisplayName("Validation: Empty or blank code submission throws BadRequestException")
    void testBlankCodeSubmission_ThrowsBadRequestException() {
        CodingProblem problem = createTwoSumProblem(randomSlug("blank-code"));

        SubmitCodingProblemRequest submitReq = SubmitCodingProblemRequest.builder()
                .languageId("python-3.12")
                .code("   ")
                .build();

        assertThatThrownBy(() -> codingProblemService.submitSolution(problem.getId(), submitReq))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Code cannot be empty");
    }
}

