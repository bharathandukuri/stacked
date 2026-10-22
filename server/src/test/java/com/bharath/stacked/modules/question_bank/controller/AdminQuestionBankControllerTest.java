package com.bharath.stacked.modules.question_bank.controller;

import com.bharath.stacked.exception.GlobalExceptionHandler;
import com.bharath.stacked.common.api.PageResponse;
import com.bharath.stacked.modules.question_bank.dto.*;
import com.bharath.stacked.modules.question_bank.enums.BankQuestionType;
import com.bharath.stacked.modules.question_bank.enums.QuestionBankStatus;
import com.bharath.stacked.modules.question_bank.service.QuestionBankService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminQuestionBankControllerTest {

    private MockMvc mockMvc;

    @Mock
    private QuestionBankService questionBankService;

    @InjectMocks
    private AdminQuestionBankController controller;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private QuestionBankSummaryResponse sampleSummary;
    private QuestionBankDetailResponse sampleDetail;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleSummary = QuestionBankSummaryResponse.builder()
                .id("qb-101")
                .slug("spring-boot-assessment-bank")
                .title("Spring Boot Assessment Bank")
                .description("Curated Spring Boot questions")
                .category("Backend")
                .status(QuestionBankStatus.PUBLISHED)
                .tags(List.of("spring-boot", "java"))
                .totalQuestions(5)
                .mcqCount(4)
                .codingCount(1)
                .totalPoints(35.0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        sampleDetail = QuestionBankDetailResponse.builder()
                .id("qb-101")
                .slug("spring-boot-assessment-bank")
                .title("Spring Boot Assessment Bank")
                .description("Curated Spring Boot questions")
                .category("Backend")
                .status(QuestionBankStatus.PUBLISHED)
                .tags(List.of("spring-boot", "java"))
                .totalQuestions(1)
                .mcqCount(1)
                .codingCount(0)
                .totalPoints(5.0)
                .items(List.of(
                        HydratedQuestionItemResponse.builder()
                                .questionType(BankQuestionType.MCQ)
                                .questionId("mcq-1")
                                .order(1)
                                .effectivePoints(5.0)
                                .title("Dependency Injection in Spring")
                                .difficulty("MEDIUM")
                                .build()
                ))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("GET /api/admin/question-banks returns paginated summaries")
    void listQuestionBanks_Success() throws Exception {
        PageResponse<QuestionBankSummaryResponse> paginated =
                PageResponse.of(List.of(sampleSummary), 0, 10, 1);

        when(questionBankService.getQuestionBanks(any(QuestionBankFilterRequest.class))).thenReturn(paginated);

        mockMvc.perform(get("/api/admin/question-banks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].id").value("qb-101"))
                .andExpect(jsonPath("$.data.items[0].title").value("Spring Boot Assessment Bank"));
    }

    @Test
    @DisplayName("GET /api/admin/question-banks/{idOrSlug} returns detail")
    void getQuestionBank_Success() throws Exception {
        when(questionBankService.getQuestionBank("qb-101")).thenReturn(sampleDetail);

        mockMvc.perform(get("/api/admin/question-banks/qb-101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("qb-101"))
                .andExpect(jsonPath("$.data.items[0].title").value("Dependency Injection in Spring"));
    }

    @Test
    @DisplayName("POST /api/admin/question-banks creates new bank and returns 201")
    void createQuestionBank_Success() throws Exception {
        CreateQuestionBankRequest request = CreateQuestionBankRequest.builder()
                .title("Spring Boot Assessment Bank")
                .category("Backend")
                .status(QuestionBankStatus.PUBLISHED)
                .items(List.of(
                        QuestionBankItemRequest.builder()
                                .questionType(BankQuestionType.MCQ)
                                .questionId("mcq-1")
                                .build()
                ))
                .build();

        when(questionBankService.createQuestionBank(any(CreateQuestionBankRequest.class), any())).thenReturn(sampleSummary);

        mockMvc.perform(post("/api/admin/question-banks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("qb-101"));
    }

    @Test
    @DisplayName("POST /api/admin/question-banks with blank title returns 422")
    void createQuestionBank_WhenTitleBlank_Returns422() throws Exception {
        CreateQuestionBankRequest request = CreateQuestionBankRequest.builder()
                .title("")
                .build();

        mockMvc.perform(post("/api/admin/question-banks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("PUT /api/admin/question-banks/{idOrSlug} updates bank and returns 200")
    void updateQuestionBank_Success() throws Exception {
        UpdateQuestionBankRequest request = UpdateQuestionBankRequest.builder()
                .title("Updated Bank")
                .status(QuestionBankStatus.PUBLISHED)
                .build();

        when(questionBankService.updateQuestionBank(eq("qb-101"), any(UpdateQuestionBankRequest.class))).thenReturn(sampleSummary);

        mockMvc.perform(put("/api/admin/question-banks/qb-101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("qb-101"));
    }

    @Test
    @DisplayName("DELETE /api/admin/question-banks/{idOrSlug} deletes bank")
    void deleteQuestionBank_Success() throws Exception {
        mockMvc.perform(delete("/api/admin/question-banks/qb-101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(questionBankService).deleteQuestionBank("qb-101");
    }

    @Test
    @DisplayName("POST /api/admin/question-banks/{idOrSlug}/duplicate duplicates bank and returns 201")
    void duplicateQuestionBank_Success() throws Exception {
        when(questionBankService.duplicateQuestionBank(eq("qb-101"), any())).thenReturn(sampleSummary);

        mockMvc.perform(post("/api/admin/question-banks/qb-101/duplicate"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("qb-101"));
    }

    @Test
    @DisplayName("GET /api/admin/question-banks/categories returns categories list")
    void getCategories_Success() throws Exception {
        when(questionBankService.getDistinctCategories()).thenReturn(List.of("Backend", "Frontend", "DevOps"));

        mockMvc.perform(get("/api/admin/question-banks/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0]").value("Backend"))
                .andExpect(jsonPath("$.data[1]").value("Frontend"));
    }

    @Test
    @DisplayName("GET /api/admin/question-banks/stats returns summary statistics")
    void getStats_Success() throws Exception {
        QuestionBankStatsResponse stats = QuestionBankStatsResponse.builder()
                .totalBanks(12)
                .publishedBanks(8)
                .draftBanks(4)
                .totalCuratedQuestions(140)
                .totalCategories(5)
                .build();

        when(questionBankService.getStats()).thenReturn(stats);

        mockMvc.perform(get("/api/admin/question-banks/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalBanks").value(12))
                .andExpect(jsonPath("$.data.publishedBanks").value(8));
    }

    @Test
    @DisplayName("PATCH /api/admin/question-banks/{idOrSlug}/status updates status and returns 200")
    void updateQuestionBankStatus_Success() throws Exception {
        UpdateQuestionBankStatusRequest req = new UpdateQuestionBankStatusRequest(QuestionBankStatus.PUBLISHED);
        when(questionBankService.updateStatus("qb-101", QuestionBankStatus.PUBLISHED)).thenReturn(sampleSummary);

        mockMvc.perform(patch("/api/admin/question-banks/qb-101/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        verify(questionBankService).updateStatus("qb-101", QuestionBankStatus.PUBLISHED);
    }

    @Test
    @DisplayName("PATCH /api/admin/question-banks/{idOrSlug}/publish publishes bank and returns 200")
    void publishQuestionBank_Success() throws Exception {
        when(questionBankService.publish("qb-101")).thenReturn(sampleSummary);

        mockMvc.perform(patch("/api/admin/question-banks/qb-101/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("qb-101"));

        verify(questionBankService).publish("qb-101");
    }

    @Test
    @DisplayName("PATCH /api/admin/question-banks/{idOrSlug}/unpublish unpublishes bank and returns 200")
    void unpublishQuestionBank_Success() throws Exception {
        QuestionBankSummaryResponse draftSummary = QuestionBankSummaryResponse.builder()
                .id("qb-101")
                .status(QuestionBankStatus.DRAFT)
                .build();
        when(questionBankService.unpublish("qb-101")).thenReturn(draftSummary);

        mockMvc.perform(patch("/api/admin/question-banks/qb-101/unpublish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        verify(questionBankService).unpublish("qb-101");
    }
}
