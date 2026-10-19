package com.bharath.stacked.modules.coding_problem.controller;

import com.bharath.stacked.exception.GlobalExceptionHandler;
import com.bharath.stacked.exception.ResourceNotFoundException;
import com.bharath.stacked.modules.coding_problem.dto.request.CreateCodingProblemRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.TestcaseRequest;
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
}
