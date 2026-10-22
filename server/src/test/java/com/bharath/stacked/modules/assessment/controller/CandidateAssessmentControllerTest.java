package com.bharath.stacked.modules.assessment.controller;

import com.bharath.stacked.exception.GlobalExceptionHandler;
import com.bharath.stacked.modules.assessment.dto.*;
import com.bharath.stacked.modules.assessment.enums.AssessmentAuditEventType;
import com.bharath.stacked.modules.assessment.enums.AttemptStatus;
import com.bharath.stacked.modules.assessment.service.AssessmentService;
import com.bharath.stacked.modules.security.model.User;
import com.bharath.stacked.modules.security.repository.UserRepository;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CandidateAssessmentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AssessmentService assessmentService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CandidateAssessmentController candidateAssessmentController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(candidateAssessmentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/assessments/{id}/start should start attempt")
    void shouldStartAttemptSuccessfully() throws Exception {
        User user = User.builder().id("user-1").email("cand@test.com").build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        StartAttemptResponse res = StartAttemptResponse.builder()
                .attemptId("att-101")
                .assessmentId("ass-1")
                .attemptNumber(1)
                .status(AttemptStatus.IN_PROGRESS)
                .remainingSeconds(3600)
                .build();

        when(assessmentService.startAttempt(eq("ass-1"), eq("user-1"), eq("cand@test.com"))).thenReturn(res);

        mockMvc.perform(post("/api/assessments/ass-1/start")
                        .principal(() -> "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attemptId").value("att-101"))
                .andExpect(jsonPath("$.data.remainingSeconds").value(3600));
    }

    @Test
    @DisplayName("POST /api/assessments/{id}/response/mcq should save candidate choice")
    void shouldSaveMcqResponse() throws Exception {
        SaveMcqResponseRequest req = SaveMcqResponseRequest.builder()
                .questionId("mcq-1")
                .selectedOptionIds(List.of("opt-1"))
                .build();

        SavedQuestionResponseDto res = SavedQuestionResponseDto.builder()
                .questionId("mcq-1")
                .selectedOptionIds(List.of("opt-1"))
                .savedAt(Instant.now())
                .build();

        when(assessmentService.saveMcqResponse(eq("ass-1"), eq("user-1"), any())).thenReturn(res);

        mockMvc.perform(post("/api/assessments/ass-1/response/mcq")
                        .principal(() -> "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questionId").value("mcq-1"));
    }

    @Test
    @DisplayName("POST /api/assessments/{id}/submit should finalize attempt")
    void shouldFinalizeSubmission() throws Exception {
        FinalSubmitResponse res = FinalSubmitResponse.builder()
                .attemptId("att-101")
                .status(AttemptStatus.SUBMITTED)
                .score(85.0)
                .maxScore(100.0)
                .passed(true)
                .message("Assessment submitted successfully.")
                .build();

        when(assessmentService.finalSubmit("ass-1", "user-1")).thenReturn(res);

        mockMvc.perform(post("/api/assessments/ass-1/submit")
                        .principal(() -> "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attemptId").value("att-101"))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.passed").value(true));
    }

    @Test
    @DisplayName("POST /api/assessments/join-by-code should redeem code and register candidate")
    void shouldJoinByCode() throws Exception {
        User user = User.builder().id("user-1").email("cand@test.com").build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        CandidateAssessmentSummaryResponse summary = CandidateAssessmentSummaryResponse.builder()
                .assessmentId("ass-1")
                .title("Algorithms Screen")
                .canStart(true)
                .build();

        when(assessmentService.joinByInvitationCode(eq("user-1"), eq("cand@test.com"), any(JoinByCodeRequest.class)))
                .thenReturn(summary);

        mockMvc.perform(post("/api/assessments/join-by-code")
                        .principal(() -> "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinByCodeRequest("STK-XYZ-123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assessmentId").value("ass-1"))
                .andExpect(jsonPath("$.data.title").value("Algorithms Screen"));
    }

    @Test
    @DisplayName("GET /api/assessments/{id}/readiness should return readiness evaluation")
    void shouldGetReadiness() throws Exception {
        User user = User.builder().id("user-1").email("cand@test.com").build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        AssessmentReadinessResponse readiness = AssessmentReadinessResponse.builder()
                .assessmentId("ass-1")
                .authenticated(true)
                .canStart(true)
                .backendReady(true)
                .build();

        when(assessmentService.getReadiness("ass-1", "user-1", "cand@test.com")).thenReturn(readiness);

        mockMvc.perform(get("/api/assessments/ass-1/readiness")
                        .principal(() -> "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assessmentId").value("ass-1"))
                .andExpect(jsonPath("$.data.backendReady").value(true))
                .andExpect(jsonPath("$.data.canStart").value(true));
    }

    @Test
    @DisplayName("POST /api/assessments/{id}/response/mark-for-review should set review flag")
    void shouldMarkForReview() throws Exception {
        SavedQuestionResponseDto resp = SavedQuestionResponseDto.builder()
                .questionId("q-1")
                .markedForReview(true)
                .build();

        when(assessmentService.markForReview(eq("ass-1"), eq("user-1"), any(MarkQuestionForReviewRequest.class)))
                .thenReturn(resp);

        mockMvc.perform(post("/api/assessments/ass-1/response/mark-for-review")
                        .principal(() -> "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MarkQuestionForReviewRequest("q-1", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questionId").value("q-1"))
                .andExpect(jsonPath("$.data.markedForReview").value(true));
    }

    @Test
    @DisplayName("GET /api/assessments/{id}/results should return candidate results")
    void shouldGetCandidateResults() throws Exception {
        CandidateAssessmentResultResponse res = CandidateAssessmentResultResponse.builder()
                .assessmentId("ass-1")
                .attemptId("att-1")
                .attemptNumber(1)
                .status(AttemptStatus.SUBMITTED)
                .score(16.0)
                .maxPossibleScore(20.0)
                .percentage(80.0)
                .passed(true)
                .resultsVisible(true)
                .build();

        when(assessmentService.getCandidateResults("ass-1", "user-1")).thenReturn(res);

        mockMvc.perform(get("/api/assessments/ass-1/results")
                        .principal(() -> "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assessmentId").value("ass-1"))
                .andExpect(jsonPath("$.data.score").value(16.0))
                .andExpect(jsonPath("$.data.passed").value(true));
    }

    @Test
    @DisplayName("POST /api/assessments/{id}/telemetry should record telemetry events")
    void shouldRecordTelemetry() throws Exception {
        String json = """
                [
                    {
                        "eventType": "WINDOW_BLUR",
                        "questionId": "q-1",
                        "clientTimestamp": "2026-10-21T15:30:00Z"
                    }
                ]
                """;

        mockMvc.perform(post("/api/assessments/ass-1/telemetry")
                        .principal(() -> "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }
}

