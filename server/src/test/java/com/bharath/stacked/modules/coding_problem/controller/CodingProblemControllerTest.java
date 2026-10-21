package com.bharath.stacked.modules.coding_problem.controller;

import com.bharath.stacked.exception.GlobalExceptionHandler;
import com.bharath.stacked.exception.ResourceNotFoundException;
import com.bharath.stacked.modules.coding_problem.dto.request.CreateCodingProblemRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.PreviewTestcaseRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.RunCodingProblemPreviewRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.RunTestcaseGeneratorRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.TestcaseRequest;
import com.bharath.stacked.modules.coding_problem.dto.response.PreviewRunResponse;
import com.bharath.stacked.modules.coding_problem.dto.response.PreviewTestcaseResultResponse;
import com.bharath.stacked.modules.coding_problem.dto.response.SupportedLanguageResponse;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemDifficulty;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemStatus;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemType;
import com.bharath.stacked.modules.coding_problem.model.CodingProblem;
import com.bharath.stacked.modules.coding_problem.service.CodingProblemService;
import com.bharath.stacked.modules.language.LanguageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CodingProblemControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CodingProblemService codingProblemService;

    @InjectMocks
    private CodingProblemController codingProblemController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(codingProblemController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/coding-problem/check-slug returns 200 with available=true when slug is free")
    void checkSlugAvailability_WhenAvailable() throws Exception {
        when(codingProblemService.isSlugAvailable("two-sum")).thenReturn(true);

        mockMvc.perform(get("/api/coding-problem/check-slug")
                        .param("slug", "two-sum"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.slug").value("two-sum"))
                .andExpect(jsonPath("$.data.available").value(true));

        verify(codingProblemService).isSlugAvailable("two-sum");
    }

    @Test
    @DisplayName("GET /api/coding-problem/check-slug returns 200 with available=false when slug is taken")
    void checkSlugAvailability_WhenTaken() throws Exception {
        when(codingProblemService.isSlugAvailable("two-sum")).thenReturn(false);

        mockMvc.perform(get("/api/coding-problem/check-slug")
                        .param("slug", "two-sum"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.slug").value("two-sum"))
                .andExpect(jsonPath("$.data.available").value(false));

        verify(codingProblemService).isSlugAvailable("two-sum");
    }

    @Test
    @DisplayName("PATCH /api/coding-problem/{id}/publish publishes problem successfully")
    void publishProblem_Success() throws Exception {
        CodingProblem problem = CodingProblem.builder()
                .id("prob-123")
                .slug("two-sum")
                .status(CodingProblemStatus.PUBLISHED)
                .build();

        when(codingProblemService.publish("prob-123")).thenReturn(problem);

        mockMvc.perform(patch("/api/coding-problem/prob-123/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Coding problem published successfully"))
                .andExpect(jsonPath("$.data.id").value("prob-123"))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        verify(codingProblemService).publish("prob-123");
    }

    @Test
    @DisplayName("PATCH /api/coding-problem/{id}/unpublish unpublishes problem successfully")
    void unpublishProblem_Success() throws Exception {
        CodingProblem problem = CodingProblem.builder()
                .id("prob-123")
                .slug("two-sum")
                .status(CodingProblemStatus.UNPUBLISHED)
                .build();

        when(codingProblemService.unpublish("prob-123")).thenReturn(problem);

        mockMvc.perform(patch("/api/coding-problem/prob-123/unpublish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Coding problem unpublished successfully"))
                .andExpect(jsonPath("$.data.id").value("prob-123"))
                .andExpect(jsonPath("$.data.status").value("UNPUBLISHED"));

        verify(codingProblemService).unpublish("prob-123");
    }

    @Test
    @DisplayName("PATCH /api/coding-problem/{id}/archive archives problem successfully")
    void archiveProblem_Success() throws Exception {
        CodingProblem problem = CodingProblem.builder()
                .id("prob-123")
                .slug("two-sum")
                .status(CodingProblemStatus.ARCHIVED)
                .build();

        when(codingProblemService.archive("prob-123")).thenReturn(problem);

        mockMvc.perform(patch("/api/coding-problem/prob-123/archive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Coding problem archived successfully"))
                .andExpect(jsonPath("$.data.id").value("prob-123"))
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));

        verify(codingProblemService).archive("prob-123");
    }

    @Test
    @DisplayName("PATCH /api/coding-problem/{id}/status updates problem status successfully")
    void updateProblemStatus_Success() throws Exception {
        CodingProblem problem = CodingProblem.builder()
                .id("prob-123")
                .slug("two-sum")
                .status(CodingProblemStatus.PUBLISHED)
                .build();

        when(codingProblemService.updateStatus("prob-123", CodingProblemStatus.PUBLISHED)).thenReturn(problem);

        mockMvc.perform(patch("/api/coding-problem/prob-123/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "status": "PUBLISHED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Coding problem status updated successfully"))
                .andExpect(jsonPath("$.data.id").value("prob-123"))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        verify(codingProblemService).updateStatus("prob-123", CodingProblemStatus.PUBLISHED);
    }

    @Test
    @DisplayName("PATCH /api/coding-problem/{id}/status returns 422 when status is missing")
    void updateProblemStatus_MissingStatus_UnprocessableEntity() throws Exception {
        mockMvc.perform(patch("/api/coding-problem/prob-123/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PATCH /api/coding-problem/{id}/publish returns 404 when problem not found")
    void publishProblem_NotFound() throws Exception {
        when(codingProblemService.publish("unknown"))
                .thenThrow(new ResourceNotFoundException("CodingProblem", "id or slug", "unknown"));

        mockMvc.perform(patch("/api/coding-problem/unknown/publish"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/coding-problem creates problem and returns 201 Created")
    void createCodingProblem_Success() throws Exception {
        CodingProblem problem = CodingProblem.builder()
                .id("prob-123")
                .title("Two Sum")
                .slug("two-sum")
                .status(CodingProblemStatus.UNPUBLISHED)
                .problemType(CodingProblemType.GENERIC)
                .difficulty(CodingProblemDifficulty.EASY)
                .supportedLanguageIds(List.of("python-3.12"))
                .build();

        when(codingProblemService.create(any(CreateCodingProblemRequest.class))).thenReturn(problem);

        mockMvc.perform(post("/api/coding-problem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Two Sum",
                                    "slug": "two-sum",
                                    "problemType": "GENERIC",
                                    "difficulty": "EASY",
                                    "supportedLanguageIds": ["python-3.12"],
                                    "statement": "Given numbers, find pair.",
                                    "topics": ["Array"],
                                    "hints": [],
                                    "starterCode": { "python-3.12": "def solve(): pass" },
                                    "referenceSolutionLanguage": "python-3.12",
                                    "referenceSolution": "print(1)",
                                    "requireVerificationScript": false,
                                    "sampleTestcases": [ { "input": "1", "setupScript": null } ],
                                    "hiddenTestcases": [ { "input": "2", "setupScript": null } ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("prob-123"))
                .andExpect(jsonPath("$.data.status").value("UNPUBLISHED"));
    }

    @Test
    @DisplayName("GET /api/coding-problem/supported-languages returns 200 with list of supported languages")
    void getSupportedLanguages_All_Success() throws Exception {
        SupportedLanguageResponse javaLang = new SupportedLanguageResponse(
                "java-21", "Java 21", LanguageType.COMPILED, ".java", "Solution.java", 2000L, 262144L);
        SupportedLanguageResponse pythonLang = new SupportedLanguageResponse(
                "python-3.12", "Python 3.12", LanguageType.INTERPRETED, ".py", "solution.py", 3000L, 131072L);

        when(codingProblemService.getSupportedLanguages(null)).thenReturn(List.of(javaLang, pythonLang));

        mockMvc.perform(get("/api/coding-problem/supported-languages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Supported languages retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value("java-21"))
                .andExpect(jsonPath("$.data[0].name").value("Java 21"))
                .andExpect(jsonPath("$.data[0].type").value("COMPILED"))
                .andExpect(jsonPath("$.data[0].fileExtension").value(".java"))
                .andExpect(jsonPath("$.data[0].defaultFileName").value("Solution.java"))
                .andExpect(jsonPath("$.data[0].defaultTimeLimitMs").value(2000))
                .andExpect(jsonPath("$.data[0].defaultMemoryLimitKb").value(262144))
                .andExpect(jsonPath("$.data[1].id").value("python-3.12"));

        verify(codingProblemService).getSupportedLanguages(null);
    }

    @Test
    @DisplayName("GET /api/coding-problem/supported-languages?problemType=GENERIC filters by problem type")
    void getSupportedLanguages_FilteredByProblemType_Success() throws Exception {
        SupportedLanguageResponse pythonLang = new SupportedLanguageResponse(
                "python-3.12", "Python 3.12", LanguageType.INTERPRETED, ".py", "solution.py", 3000L, 131072L);

        when(codingProblemService.getSupportedLanguages(CodingProblemType.GENERIC)).thenReturn(List.of(pythonLang));

        mockMvc.perform(get("/api/coding-problem/supported-languages")
                        .param("problemType", "GENERIC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value("python-3.12"));

        verify(codingProblemService).getSupportedLanguages(CodingProblemType.GENERIC);
    }

    @Test
    @DisplayName("GET /api/coding-problem/languages alias returns 200 with supported languages")
    void getLanguages_Alias_Success() throws Exception {
        SupportedLanguageResponse sqliteLang = new SupportedLanguageResponse(
                "sqlite-3", "SQLite 3", LanguageType.DATABASE, ".sql", "solution.sql", 1000L, 65536L);

        when(codingProblemService.getSupportedLanguages(null)).thenReturn(List.of(sqliteLang));

        mockMvc.perform(get("/api/coding-problem/languages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value("sqlite-3"));

        verify(codingProblemService).getSupportedLanguages(null);
    }

    @Test
    @DisplayName("POST /api/coding-problem/run-preview returns 200 with preview execution results")
    void runPreview_Success() throws Exception {
        PreviewTestcaseResultResponse res1 = PreviewTestcaseResultResponse.builder()
                .testCaseId("case-1")
                .caseIndex(1)
                .status("SUCCESS")
                .stdout("output-1")
                .exitCode(0L)
                .runtimeMs(14L)
                .memoryMb(18.2)
                .build();

        PreviewRunResponse previewResponse = PreviewRunResponse.builder()
                .overallStatus("SUCCESS")
                .totalExecutionTimeMs(14L)
                .totalTestcases(1)
                .passedTestcases(1)
                .failedTestcases(0)
                .logs("[Execution Summary]\nPassed: 1/1")
                .results(List.of(res1))
                .build();

        when(codingProblemService.runPreview(any(RunCodingProblemPreviewRequest.class))).thenReturn(previewResponse);

        String json = """
                {
                    "problemType": "GENERIC",
                    "referenceSolutionLanguage": "python-3.12",
                    "referenceSolution": "print('hello')",
                    "requireVerificationScript": false,
                    "testcases": [
                        {
                            "id": "case-1",
                            "stdin": "input-1"
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/api/coding-problem/run-preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Preview testcases executed successfully"))
                .andExpect(jsonPath("$.data.overallStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.data.results[0].stdout").value("output-1"));

        verify(codingProblemService).runPreview(any(RunCodingProblemPreviewRequest.class));
    }

    @Test
    @DisplayName("POST /api/coding-problem/run-generator returns 200 on successful script execution")
    void testRunGenerator_Success() throws Exception {
        com.bharath.stacked.modules.coding_problem.dto.response.TestcaseGeneratorRunResponse generatorResponse =
                com.bharath.stacked.modules.coding_problem.dto.response.TestcaseGeneratorRunResponse.builder()
                        .status(com.bharath.stacked.modules.execution.enums.CodeExecutionStatus.SUCCESS)
                        .stdout("10\n---STACKED_TESTCASE_DELIMITER---\n20\n")
                        .stderr("")
                        .exitCode(0L)
                        .executionTimeMs(120L)
                        .memoryUsageKb(15000L)
                        .build();

        when(codingProblemService.runGenerator(any(RunTestcaseGeneratorRequest.class))).thenReturn(generatorResponse);

        String json = """
                {
                    "language": "python-3.12",
                    "script": "print('10\\\\n---STACKED_TESTCASE_DELIMITER---\\\\n20')",
                    "stdin": ""
                }
                """;

        mockMvc.perform(post("/api/coding-problem/run-generator")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Generator script executed successfully"))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.stdout").value("10\n---STACKED_TESTCASE_DELIMITER---\n20\n"))
                .andExpect(jsonPath("$.data.executionTimeMs").value(120));

        verify(codingProblemService).runGenerator(any(RunTestcaseGeneratorRequest.class));
    }

    @Test
    @DisplayName("POST /api/coding-problem/run-generator returns 400 when script is blank")
    void testRunGenerator_BlankScript_Returns400() throws Exception {
        String json = """
                {
                    "language": "python-3.12",
                    "script": ""
                }
                """;

        mockMvc.perform(post("/api/coding-problem/run-generator")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("GET /api/coding-problem returns paginated problem summaries")
    void testGetCodingProblems_ReturnsPageResponse() throws Exception {
        com.bharath.stacked.modules.coding_problem.dto.response.CodingProblemSummaryResponse summary =
                com.bharath.stacked.modules.coding_problem.dto.response.CodingProblemSummaryResponse.builder()
                        .id("prob-123")
                        .title("Two Sum")
                        .slug("two-sum")
                        .status(CodingProblemStatus.PUBLISHED)
                        .problemType(CodingProblemType.GENERIC)
                        .difficulty(CodingProblemDifficulty.EASY)
                        .supportedLanguageIds(List.of("python-3.12", "javascript-node-20"))
                        .sampleTestCaseCount(2)
                        .hiddenTestCaseCount(5)
                        .build();

        com.bharath.stacked.common.api.PageResponse<com.bharath.stacked.modules.coding_problem.dto.response.CodingProblemSummaryResponse> pageResponse =
                com.bharath.stacked.common.api.PageResponse.of(List.of(summary), 0, 20, 1);

        when(codingProblemService.findAll(any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/coding-problem")
                        .param("page", "0")
                        .param("size", "20")
                        .param("status", "PUBLISHED")
                        .param("search", "two"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].id").value("prob-123"))
                .andExpect(jsonPath("$.data.items[0].title").value("Two Sum"))
                .andExpect(jsonPath("$.data.items[0].slug").value("two-sum"))
                .andExpect(jsonPath("$.data.items[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1));

        verify(codingProblemService).findAll(any());
    }

    @Test
    @DisplayName("GET /api/coding-problem/{idOrSlug} returns problem details")
    void testGetCodingProblem_ReturnsDetails() throws Exception {
        CodingProblem problem = CodingProblem.builder()
                .id("prob-123")
                .title("Two Sum")
                .slug("two-sum")
                .status(CodingProblemStatus.PUBLISHED)
                .problemType(CodingProblemType.GENERIC)
                .difficulty(CodingProblemDifficulty.EASY)
                .statement("Solve two sum")
                .build();

        when(codingProblemService.findByIdOrSlug("two-sum")).thenReturn(problem);

        mockMvc.perform(get("/api/coding-problem/two-sum"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("prob-123"))
                .andExpect(jsonPath("$.data.title").value("Two Sum"))
                .andExpect(jsonPath("$.data.slug").value("two-sum"));

        verify(codingProblemService).findByIdOrSlug("two-sum");
    }

    @Test
    @DisplayName("PUT /api/coding-problem/{idOrSlug} updates problem successfully")
    void testUpdateCodingProblem_Success() throws Exception {
        CodingProblem updatedProblem = CodingProblem.builder()
                .id("prob-123")
                .title("Two Sum Updated")
                .slug("two-sum")
                .status(CodingProblemStatus.PUBLISHED)
                .problemType(CodingProblemType.GENERIC)
                .difficulty(CodingProblemDifficulty.MEDIUM)
                .build();

        when(codingProblemService.update(eq("prob-123"), any())).thenReturn(updatedProblem);

        String json = """
                {
                    "title": "Two Sum Updated",
                    "slug": "two-sum",
                    "problemType": "GENERIC",
                    "difficulty": "MEDIUM",
                    "supportedLanguageIds": ["python-3.12"],
                    "statement": "Updated statement",
                    "topics": ["Array"],
                    "hints": ["Hint 1"],
                    "starterCode": { "python-3.12": "def solution(): pass" },
                    "referenceSolutionLanguage": "python-3.12",
                    "referenceSolution": "def solution(): return 42",
                    "requireVerificationScript": false,
                    "sampleTestcases": [
                        { "stdin": "1\\n", "dbInit": null }
                    ],
                    "hiddenTestcases": [
                        { "stdin": "2\\n", "dbInit": null }
                    ]
                }
                """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/coding-problem/prob-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Two Sum Updated"))
                .andExpect(jsonPath("$.data.difficulty").value("MEDIUM"));

        verify(codingProblemService).update(eq("prob-123"), any());
    }

    @Test
    @DisplayName("GET /api/coding-problem/check-slug with excludeId returns available")
    void testCheckSlug_WithExcludeId() throws Exception {
        when(codingProblemService.isSlugAvailable("two-sum", "prob-123")).thenReturn(true);

        mockMvc.perform(get("/api/coding-problem/check-slug")
                        .param("slug", "two-sum")
                        .param("excludeId", "prob-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.slug").value("two-sum"))
                .andExpect(jsonPath("$.data.available").value(true));

        verify(codingProblemService).isSlugAvailable("two-sum", "prob-123");
    }

    @Test
    @DisplayName("DELETE /api/coding-problem/{idOrSlug} returns success")
    void testDeleteCodingProblem_Success() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/coding-problem/prob-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Coding problem deleted successfully"));

        verify(codingProblemService).delete("prob-123");
    }

    @Test
    @DisplayName("DELETE /api/coding-problem/{idOrSlug} returns 404 when not found")
    void testDeleteCodingProblem_NotFound() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("CodingProblem", "id or slug", "prob-999"))
                .when(codingProblemService).delete("prob-999");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/coding-problem/prob-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        verify(codingProblemService).delete("prob-999");
    }
}
