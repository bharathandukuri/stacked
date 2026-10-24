package com.bharath.stacked.modules.assessment.controller;

import com.bharath.stacked.common.api.PageResponse;
import com.bharath.stacked.exception.GlobalExceptionHandler;
import com.bharath.stacked.modules.assessment.dto.*;
import com.bharath.stacked.modules.assessment.enums.AssessmentAuditEventSource;
import com.bharath.stacked.modules.assessment.enums.AssessmentAuditEventType;
import com.bharath.stacked.modules.assessment.enums.AssessmentStatus;
import com.bharath.stacked.modules.assessment.model.AssessmentAuditEvent;
import com.bharath.stacked.modules.assessment.service.AssessmentService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminAssessmentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AssessmentService assessmentService;

    @InjectMocks
    private AdminAssessmentController adminAssessmentController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(adminAssessmentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/admin/assessments should return 201 on valid request")
    void shouldCreateAssessmentSuccessfully() throws Exception {
        CreateAssessmentRequest req = CreateAssessmentRequest.builder()
                .title("Java Junior Test")
                .durationMinutes(60)
                .build();

        AssessmentSummaryResponse res = AssessmentSummaryResponse.builder()
                .id("ass-1")
                .slug("java-junior-test")
                .title("Java Junior Test")
                .status(AssessmentStatus.NOT_STARTED)
                .durationMinutes(60)
                .build();

        when(assessmentService.createAssessment(any(), any())).thenReturn(res);

        mockMvc.perform(post("/api/admin/assessments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("ass-1"))
                .andExpect(jsonPath("$.data.title").value("Java Junior Test"));
    }

    @Test
    @DisplayName("PUT /api/admin/assessments/{id} should be rejected with 405 Method Not Allowed because assessments are immutable")
    void shouldRejectAssessmentModification() throws Exception {
        mockMvc.perform(put("/api/admin/assessments/ass-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated Title\"}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("POST /api/admin/assessments/validate should return 200 with validation response")
    void shouldValidateAssessmentConfig() throws Exception {
        CreateAssessmentRequest req = CreateAssessmentRequest.builder()
                .title("Validation Test")
                .durationMinutes(45)
                .questionBankId("qb-1")
                .build();

        AssessmentValidationResponse res = AssessmentValidationResponse.builder()
                .valid(true)
                .calculatedTotalQuestions(5)
                .calculatedTotalScore(50.0)
                .calculatedDurationMinutes(45)
                .build();

        when(assessmentService.validateAssessmentConfig(any())).thenReturn(res);

        mockMvc.perform(post("/api/admin/assessments/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.calculatedTotalQuestions").value(5))
                .andExpect(jsonPath("$.data.calculatedTotalScore").value(50.0));
    }

    @Test
    @DisplayName("GET /api/admin/assessments/question-bank-preview/{bankId} should return 200 with preview")
    void shouldGetQuestionBankPreview() throws Exception {
        QuestionBankPreviewDto preview = QuestionBankPreviewDto.builder()
                .id("qb-1")
                .title("Core Java Bank")
                .totalQuestions(10)
                .mcqCount(6)
                .codingCount(4)
                .totalPoints(100.0)
                .eligibleForAssessment(true)
                .build();

        when(assessmentService.getQuestionBankPreview("qb-1")).thenReturn(preview);

        mockMvc.perform(get("/api/admin/assessments/question-bank-preview/qb-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("qb-1"))
                .andExpect(jsonPath("$.data.title").value("Core Java Bank"))
                .andExpect(jsonPath("$.data.eligibleForAssessment").value(true));
    }

    @Test
    @DisplayName("GET /api/admin/assessments should return 200 with page data")
    void shouldGetAssessmentsList() throws Exception {
        AssessmentSummaryResponse item = AssessmentSummaryResponse.builder()
                .id("ass-1")
                .title("Assessment 1")
                .status(AssessmentStatus.ACTIVE)
                .build();

        PageResponse<AssessmentSummaryResponse> page = PageResponse.of(List.of(item), 0, 10, 1);
        when(assessmentService.getAssessments(any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/assessments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value("ass-1"));
    }

    @Test
    @DisplayName("POST /api/admin/assessments/{id}/candidates should return 200")
    void shouldBulkAssignCandidates() throws Exception {
        BulkAssignCandidatesRequest req = BulkAssignCandidatesRequest.builder()
                .emailsRaw("cand1@test.com, cand2@test.com")
                .build();

        BulkAssignCandidatesResponse res = BulkAssignCandidatesResponse.builder()
                .successfullyAssigned(2)
                .totalProcessed(2)
                .build();

        when(assessmentService.bulkAssignCandidates(eq("ass-1"), any())).thenReturn(res);

        mockMvc.perform(post("/api/admin/assessments/ass-1/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successfullyAssigned").value(2));
    }

    @Test
    @DisplayName("GET /api/admin/assessments/{id}/metrics should return 200 with aggregated metrics")
    void shouldGetAssessmentMetrics() throws Exception {
        AssessmentMetricsResponse metrics = AssessmentMetricsResponse.builder()
                .assessmentId("ass-1")
                .assessmentTitle("Screening Test")
                .totalCandidatesAssigned(10)
                .submittedCount(8)
                .completionRate(80.0)
                .passRate(75.0)
                .averageScore(15.5)
                .medianScore(16.0)
                .maxPossibleScore(20.0)
                .build();

        when(assessmentService.getAssessmentMetrics("ass-1")).thenReturn(metrics);

        mockMvc.perform(get("/api/admin/assessments/ass-1/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assessmentId").value("ass-1"))
                .andExpect(jsonPath("$.data.completionRate").value(80.0))
                .andExpect(jsonPath("$.data.passRate").value(75.0));
    }

    @Test
    @DisplayName("GET /api/admin/assessments/attempts/{attemptId}/detail should return 200")
    void shouldGetAttemptDetailForAdmin() throws Exception {
        CandidateAssessmentResultResponse res = CandidateAssessmentResultResponse.builder()
                .assessmentId("ass-1")
                .attemptId("att-100")
                .score(18.0)
                .maxPossibleScore(20.0)
                .resultsVisible(true)
                .build();

        when(assessmentService.getAttemptDetailForAdmin("att-100")).thenReturn(res);

        mockMvc.perform(get("/api/admin/assessments/attempts/att-100/detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attemptId").value("att-100"))
                .andExpect(jsonPath("$.data.score").value(18.0));
    }

    @Test
    @DisplayName("GET /api/admin/assessments/attempts/{attemptId}/audit should return 200 with event timeline")
    void shouldGetAttemptAuditTrail() throws Exception {
        AssessmentAuditEvent event = AssessmentAuditEvent.builder()
                .id("evt-1")
                .assessmentId("ass-1")
                .attemptId("att-100")
                .eventType(AssessmentAuditEventType.CODE_SUBMITTED)
                .source(AssessmentAuditEventSource.SERVER_VERIFIED)
                .build();

        when(assessmentService.getAttemptAuditTrail("att-100")).thenReturn(List.of(event));

        mockMvc.perform(get("/api/admin/assessments/attempts/att-100/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("evt-1"))
                .andExpect(jsonPath("$.data[0].eventType").value("CODE_SUBMITTED"))
                .andExpect(jsonPath("$.data[0].source").value("SERVER_VERIFIED"));
    }

    @Test
    @DisplayName("PATCH /api/admin/assessments/{id} should be rejected with 405 Method Not Allowed because manual status changes are prohibited")
    void shouldRejectManualStatusUpdate() throws Exception {
        mockMvc.perform(patch("/api/admin/assessments/ass-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("POST /api/admin/assessments/{id}/activate should return 200 with activated assessment")
    void shouldActivateAssessmentSuccessfully() throws Exception {
        AssessmentDetailResponse res = AssessmentDetailResponse.builder()
                .id("ass-1")
                .title("Activated Test")
                .status(AssessmentStatus.ACTIVE)
                .manuallyActivated(true)
                .build();

        when(assessmentService.activateAssessment("ass-1")).thenReturn(res);

        mockMvc.perform(post("/api/admin/assessments/ass-1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.manuallyActivated").value(true));
    }

    @Test
    @DisplayName("POST /api/admin/assessments/{id}/complete should return 200 with completed assessment")
    void shouldCompleteAssessmentSuccessfully() throws Exception {
        AssessmentDetailResponse res = AssessmentDetailResponse.builder()
                .id("ass-1")
                .title("Completed Test")
                .status(AssessmentStatus.COMPLETED)
                .build();

        when(assessmentService.completeAssessment("ass-1")).thenReturn(res);

        mockMvc.perform(post("/api/admin/assessments/ass-1/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }
}

