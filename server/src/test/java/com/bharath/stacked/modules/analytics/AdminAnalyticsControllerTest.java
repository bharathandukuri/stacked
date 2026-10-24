package com.bharath.stacked.modules.analytics;

import com.bharath.stacked.common.api.PageResponse;
import com.bharath.stacked.exception.GlobalExceptionHandler;
import com.bharath.stacked.modules.analytics.controller.AdminAnalyticsController;
import com.bharath.stacked.modules.analytics.dto.*;
import com.bharath.stacked.modules.analytics.enums.AnalyticsActorType;
import com.bharath.stacked.modules.analytics.enums.AnalyticsEntityType;
import com.bharath.stacked.modules.analytics.enums.AnalyticsEventType;
import com.bharath.stacked.modules.analytics.service.AdminAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAnalyticsController Unit Tests")
class AdminAnalyticsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminAnalyticsService adminAnalyticsService;

    @InjectMocks
    private AdminAnalyticsController adminAnalyticsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminAnalyticsController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/admin/analytics/overview returns 200 with platform metrics")
    void shouldReturnPlatformOverview() throws Exception {
        PlatformOverviewResponse overview = PlatformOverviewResponse.builder()
                .totalCodingProblems(50L)
                .totalMcqs(100L)
                .totalQuestionBanks(10L)
                .totalAssessments(15L)
                .activeAssessments(5L)
                .completedAssessments(8L)
                .totalCandidates(80L)
                .totalEnrollments(120L)
                .totalAttemptsStarted(200L)
                .totalAttemptsSubmitted(180L)
                .overallCompletionRate(90.0)
                .build();

        when(adminAnalyticsService.getPlatformOverview()).thenReturn(overview);

        mockMvc.perform(get("/api/admin/analytics/overview")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCodingProblems").value(50))
                .andExpect(jsonPath("$.data.totalAssessments").value(15))
                .andExpect(jsonPath("$.data.overallCompletionRate").value(90.0));
    }

    @Test
    @DisplayName("GET /api/admin/analytics/content returns 200 with content stats")
    void shouldReturnContentAnalytics() throws Exception {
        ContentAnalyticsResponse content = ContentAnalyticsResponse.builder()
                .codingProblemsPublished(40L)
                .codingProblemsDraft(10L)
                .mcqsPublished(100L)
                .mcqsDraft(20L)
                .questionBanksPublished(8L)
                .questionBanksDraft(2L)
                .build();

        when(adminAnalyticsService.getContentAnalytics(any(), any())).thenReturn(content);

        mockMvc.perform(get("/api/admin/analytics/content")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.codingProblemsPublished").value(40))
                .andExpect(jsonPath("$.data.mcqsPublished").value(100));
    }

    @Test
    @DisplayName("GET /api/admin/analytics/assessments returns 200 with assessment funnel")
    void shouldReturnAssessmentAnalytics() throws Exception {
        AssessmentAnalyticsResponse response = AssessmentAnalyticsResponse.builder()
                .assessmentsCreated(10L)
                .assessmentsActivated(8L)
                .assessmentsCompleted(6L)
                .candidatesInvited(100L)
                .attemptsStarted(80L)
                .attemptsSubmitted(70L)
                .completionRate(87.5)
                .participationRate(80.0)
                .build();

        when(adminAnalyticsService.getAssessmentAnalytics(any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/admin/analytics/assessments")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assessmentsCreated").value(10))
                .andExpect(jsonPath("$.data.completionRate").value(87.5));
    }

    @Test
    @DisplayName("GET /api/admin/analytics/submissions returns 200 with submission breakdowns")
    void shouldReturnSubmissionAnalytics() throws Exception {
        SubmissionAnalyticsResponse response = SubmissionAnalyticsResponse.builder()
                .totalSubmissions(500L)
                .acceptedSubmissions(350L)
                .rejectedSubmissions(150L)
                .passRate(70.0)
                .avgExecutionTimeMs(120.5)
                .verdictDistribution(Map.of("ACCEPTED", 350L, "WRONG_ANSWER", 150L))
                .build();

        when(adminAnalyticsService.getSubmissionAnalytics(any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/admin/analytics/submissions")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalSubmissions").value(500))
                .andExpect(jsonPath("$.data.passRate").value(70.0));
    }

    @Test
    @DisplayName("GET /api/admin/analytics/operational returns 200 with operational metrics")
    void shouldReturnOperationalAnalytics() throws Exception {
        OperationalAnalyticsResponse response = OperationalAnalyticsResponse.builder()
                .activeExecutionWorkers(4)
                .currentQueueDepth(1)
                .maxQueueCapacity(50)
                .totalEventsLogged(1250L)
                .systemStatus("HEALTHY")
                .build();

        when(adminAnalyticsService.getOperationalAnalytics()).thenReturn(response);

        mockMvc.perform(get("/api/admin/analytics/operational")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.activeExecutionWorkers").value(4))
                .andExpect(jsonPath("$.data.totalEventsLogged").value(1250));
    }

    @Test
    @DisplayName("GET /api/admin/analytics/timeline returns 200 with paginated events")
    void shouldReturnActivityTimeline() throws Exception {
        ActivityTimelineItemResponse item = ActivityTimelineItemResponse.builder()
                .id("evt-123")
                .eventType(AnalyticsEventType.ATTEMPT_STARTED)
                .entityType(AnalyticsEntityType.ATTEMPT)
                .entityId("attempt-456")
                .actorType(AnalyticsActorType.CANDIDATE)
                .actorId("user-789")
                .assessmentId("asmt-100")
                .timestamp(Instant.now())
                .attributes(Map.of("attemptNumber", 1))
                .build();

        PageResponse<ActivityTimelineItemResponse> page = PageResponse.of(List.of(item), 0, 10, 1);
        when(adminAnalyticsService.getActivityTimeline(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/analytics/timeline")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].id").value("evt-123"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }
}
