package com.bharath.stacked.modules.analytics;

import com.bharath.stacked.common.api.PageResponse;
import com.bharath.stacked.exception.BadRequestException;
import com.bharath.stacked.modules.analytics.config.AnalyticsProperties;
import com.bharath.stacked.modules.analytics.dto.*;
import com.bharath.stacked.modules.analytics.enums.AnalyticsActorType;
import com.bharath.stacked.modules.analytics.enums.AnalyticsEntityType;
import com.bharath.stacked.modules.analytics.enums.AnalyticsEventType;
import com.bharath.stacked.modules.analytics.model.AnalyticsDailyMetric;
import com.bharath.stacked.modules.analytics.model.AnalyticsEvent;
import com.bharath.stacked.modules.analytics.repository.AnalyticsDailyMetricRepository;
import com.bharath.stacked.modules.analytics.repository.AnalyticsEventRepository;
import com.bharath.stacked.modules.analytics.service.impl.AdminAnalyticsServiceImpl;
import com.bharath.stacked.modules.assessment.enums.AssessmentStatus;
import com.bharath.stacked.modules.assessment.enums.AttemptStatus;
import com.bharath.stacked.modules.assessment.repository.AssessmentAssignmentRepository;
import com.bharath.stacked.modules.assessment.repository.AssessmentAttemptRepository;
import com.bharath.stacked.modules.assessment.repository.AssessmentRepository;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemStatus;
import com.bharath.stacked.modules.coding_problem.repository.CodingProblemRepository;
import com.bharath.stacked.modules.execution.queue.ExecutionQueueService;
import com.bharath.stacked.modules.mcq.enums.McqStatus;
import com.bharath.stacked.modules.mcq.repository.McqQuestionRepository;
import com.bharath.stacked.modules.question_bank.enums.QuestionBankStatus;
import com.bharath.stacked.modules.question_bank.repository.QuestionBankRepository;
import com.bharath.stacked.modules.security.model.Role;
import com.bharath.stacked.modules.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAnalyticsServiceTest {

    @Mock
    private CodingProblemRepository codingProblemRepository;

    @Mock
    private McqQuestionRepository mcqQuestionRepository;

    @Mock
    private QuestionBankRepository questionBankRepository;

    @Mock
    private AssessmentRepository assessmentRepository;

    @Mock
    private AssessmentAssignmentRepository assignmentRepository;

    @Mock
    private AssessmentAttemptRepository attemptRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AnalyticsEventRepository analyticsEventRepository;

    @Mock
    private AnalyticsDailyMetricRepository analyticsDailyMetricRepository;

    @Mock
    private ExecutionQueueService executionQueueService;

    private AnalyticsProperties analyticsProperties;
    private AdminAnalyticsServiceImpl adminAnalyticsService;

    @BeforeEach
    void setUp() {
        analyticsProperties = new AnalyticsProperties(true, 90, 365, 4, 1000);
        adminAnalyticsService = new AdminAnalyticsServiceImpl(
                analyticsProperties,
                analyticsEventRepository,
                analyticsDailyMetricRepository,
                codingProblemRepository,
                mcqQuestionRepository,
                questionBankRepository,
                assessmentRepository,
                assignmentRepository,
                attemptRepository,
                userRepository,
                Optional.of(executionQueueService)
        );
    }

    @Nested
    @DisplayName("Platform Overview Analytics")
    class OverviewTests {

        @Test
        @DisplayName("Should return accurate platform overview when data is present")
        void shouldReturnAccuratePlatformOverview() {
            when(codingProblemRepository.count()).thenReturn(25L);
            when(mcqQuestionRepository.count()).thenReturn(30L);
            when(questionBankRepository.count()).thenReturn(5L);

            when(assessmentRepository.count()).thenReturn(12L);
            when(assessmentRepository.countByStatus(AssessmentStatus.ACTIVE)).thenReturn(4L);
            when(assessmentRepository.countByStatus(AssessmentStatus.COMPLETED)).thenReturn(6L);
            when(assessmentRepository.countByStatus(AssessmentStatus.NOT_STARTED)).thenReturn(2L);

            when(userRepository.countByRole(Role.USER)).thenReturn(40L);
            when(assignmentRepository.count()).thenReturn(80L);

            when(attemptRepository.count()).thenReturn(100L);
            when(attemptRepository.countByStatus(AttemptStatus.SUBMITTED)).thenReturn(75L);
            when(attemptRepository.findAllByStatus(AttemptStatus.SUBMITTED))
                    .thenReturn(Collections.emptyList());

            PlatformOverviewResponse response = adminAnalyticsService.getPlatformOverview();

            assertThat(response).isNotNull();
            assertThat(response.getTotalCodingProblems()).isEqualTo(25L);
            assertThat(response.getTotalMcqs()).isEqualTo(30L);
            assertThat(response.getTotalQuestionBanks()).isEqualTo(5L);
            assertThat(response.getTotalAssessments()).isEqualTo(12L);
            assertThat(response.getActiveAssessments()).isEqualTo(4L);
            assertThat(response.getCompletedAssessments()).isEqualTo(6L);
            assertThat(response.getScheduledAssessments()).isEqualTo(2L);
            assertThat(response.getTotalCandidates()).isEqualTo(40L);
            assertThat(response.getTotalEnrollments()).isEqualTo(80L);
            assertThat(response.getTotalAttemptsStarted()).isEqualTo(100L);
            assertThat(response.getTotalAttemptsSubmitted()).isEqualTo(75L);
            assertThat(response.getOverallCompletionRate()).isEqualTo(75.0);
        }
    }

    @Nested
    @DisplayName("Content Analytics")
    class ContentTests {

        @Test
        @DisplayName("Should return content breakdown and creation trends")
        void shouldReturnContentAnalytics() {
            when(codingProblemRepository.countByStatus(CodingProblemStatus.PUBLISHED)).thenReturn(8L);
            when(codingProblemRepository.countByStatus(CodingProblemStatus.DRAFT)).thenReturn(2L);

            when(mcqQuestionRepository.countByStatus(McqStatus.PUBLISHED)).thenReturn(15L);
            when(mcqQuestionRepository.countByStatus(McqStatus.DRAFT)).thenReturn(5L);

            when(questionBankRepository.countByStatus(QuestionBankStatus.PUBLISHED)).thenReturn(3L);
            when(questionBankRepository.countByStatus(QuestionBankStatus.DRAFT)).thenReturn(1L);

            AnalyticsDailyMetric dm = AnalyticsDailyMetric.builder()
                    .date(LocalDate.of(2026, 10, 20))
                    .category("CONTENT")
                    .metricKey("CODING_PROBLEM_CREATED")
                    .count(3L)
                    .build();

            when(analyticsDailyMetricRepository.findAllByCategoryAndDateBetweenOrderByDateAsc(eq("CONTENT"), any(), any()))
                    .thenReturn(List.of(dm));

            ContentAnalyticsResponse response = adminAnalyticsService.getContentAnalytics(
                    LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 24)
            );

            assertThat(response).isNotNull();
            assertThat(response.getCodingProblemsPublished()).isEqualTo(8L);
            assertThat(response.getCodingProblemsDraft()).isEqualTo(2L);
            assertThat(response.getMcqsPublished()).isEqualTo(15L);
            assertThat(response.getMcqsDraft()).isEqualTo(5L);
            assertThat(response.getQuestionBanksPublished()).isEqualTo(3L);
            assertThat(response.getQuestionBanksDraft()).isEqualTo(1L);
            assertThat(response.getCreationTrends()).hasSize(1);
        }

        @Test
        @DisplayName("Should reject invalid date range where start date is after end date")
        void shouldRejectInvalidDateRange() {
            LocalDate start = LocalDate.of(2026, 10, 24);
            LocalDate end = LocalDate.of(2026, 10, 1);

            assertThatThrownBy(() -> adminAnalyticsService.getContentAnalytics(start, end))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("cannot be after end date");
        }
    }

    @Nested
    @DisplayName("Assessment Analytics")
    class AssessmentTests {

        @Test
        @DisplayName("Should aggregate lifecycle, participation funnel, and rates")
        void shouldReturnAssessmentAnalytics() {
            AnalyticsDailyMetric asmtCreated = AnalyticsDailyMetric.builder()
                    .date(LocalDate.of(2026, 10, 20))
                    .category("ASSESSMENT")
                    .metricKey("ASSESSMENT_CREATED")
                    .count(2L)
                    .build();

            AnalyticsDailyMetric asmtActivatedMode = AnalyticsDailyMetric.builder()
                    .date(LocalDate.of(2026, 10, 21))
                    .category("ASSESSMENT")
                    .metricKey("ASSESSMENT_ACTIVATED_BY_MODE")
                    .subCounters(Map.of("MANUAL", 2L, "AUTOMATIC", 1L))
                    .build();

            AnalyticsDailyMetric invitedMetric = AnalyticsDailyMetric.builder()
                    .date(LocalDate.of(2026, 10, 21))
                    .category("ASSESSMENT")
                    .metricKey("CANDIDATE_INVITED")
                    .count(50L)
                    .build();

            AnalyticsDailyMetric attemptStarted = AnalyticsDailyMetric.builder()
                    .date(LocalDate.of(2026, 10, 22))
                    .category("ATTEMPT")
                    .metricKey("ATTEMPT_STARTED")
                    .count(40L)
                    .build();

            AnalyticsDailyMetric attemptSubmitted = AnalyticsDailyMetric.builder()
                    .date(LocalDate.of(2026, 10, 22))
                    .category("ATTEMPT")
                    .metricKey("ATTEMPT_SUBMITTED")
                    .count(32L)
                    .numericSum(38400.0) // 1200 sec avg
                    .build();

            when(analyticsDailyMetricRepository.findAllByCategoryAndDateBetweenOrderByDateAsc(eq("ASSESSMENT"), any(), any()))
                    .thenReturn(List.of(asmtCreated, asmtActivatedMode, invitedMetric));
            when(analyticsDailyMetricRepository.findAllByCategoryAndDateBetweenOrderByDateAsc(eq("ATTEMPT"), any(), any()))
                    .thenReturn(List.of(attemptStarted, attemptSubmitted));

            AssessmentAnalyticsResponse response = adminAnalyticsService.getAssessmentAnalytics(
                    LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 24)
            );

            assertThat(response).isNotNull();
            assertThat(response.getAssessmentsCreated()).isEqualTo(2L);
            assertThat(response.getManualActivations()).isEqualTo(2L);
            assertThat(response.getAutoActivations()).isEqualTo(1L);
            assertThat(response.getCandidatesInvited()).isEqualTo(50L);
            assertThat(response.getAttemptsStarted()).isEqualTo(40L);
            assertThat(response.getAttemptsSubmitted()).isEqualTo(32L);
            // 40 / 50 = 80.0%
            assertThat(response.getParticipationRate()).isEqualTo(80.0);
            // 32 / 40 = 80.0%
            assertThat(response.getCompletionRate()).isEqualTo(80.0);
            // 38400 / 32 / 60 = 20.0 minutes
            assertThat(response.getAverageAttemptDurationMinutes()).isEqualTo(20.0);
        }
    }

    @Nested
    @DisplayName("Submission Analytics")
    class SubmissionTests {

        @Test
        @DisplayName("Should aggregate verdicts, languages, and pass rate")
        void shouldReturnSubmissionAnalytics() {
            AnalyticsDailyMetric submissions = AnalyticsDailyMetric.builder()
                    .date(LocalDate.of(2026, 10, 22))
                    .category("SUBMISSION")
                    .metricKey("CODE_SUBMISSION_EVALUATED")
                    .count(100L)
                    .numericSum(25000.0)
                    .build();

            AnalyticsDailyMetric verdicts = AnalyticsDailyMetric.builder()
                    .date(LocalDate.of(2026, 10, 22))
                    .category("SUBMISSION")
                    .metricKey("SUBMISSION_BY_VERDICT")
                    .subCounters(Map.of("ACCEPTED", 70L, "WRONG_ANSWER", 30L))
                    .build();

            AnalyticsDailyMetric languages = AnalyticsDailyMetric.builder()
                    .date(LocalDate.of(2026, 10, 22))
                    .category("SUBMISSION")
                    .metricKey("SUBMISSION_BY_LANGUAGE")
                    .subCounters(Map.of("python", 60L, "java", 40L))
                    .build();

            when(analyticsDailyMetricRepository.findAllByCategoryAndDateBetweenOrderByDateAsc(eq("SUBMISSION"), any(), any()))
                    .thenReturn(List.of(submissions, verdicts, languages));

            SubmissionAnalyticsResponse response = adminAnalyticsService.getSubmissionAnalytics(
                    LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 24)
            );

            assertThat(response).isNotNull();
            assertThat(response.getTotalSubmissions()).isEqualTo(100L);
            assertThat(response.getAcceptedSubmissions()).isEqualTo(70L);
            assertThat(response.getRejectedSubmissions()).isEqualTo(30L);
            assertThat(response.getPassRate()).isEqualTo(70.0);
            assertThat(response.getAvgExecutionTimeMs()).isEqualTo(250.0);
            assertThat(response.getVerdictDistribution()).containsEntry("ACCEPTED", 70L);
            assertThat(response.getLanguageDistribution()).containsEntry("python", 60L);
        }
    }

    @Nested
    @DisplayName("Operational Analytics & Activity Timeline")
    class OperationalTests {

        @Test
        @DisplayName("Should return execution queue depth, worker count, and logged events")
        void shouldReturnOperationalAnalytics() {
            when(executionQueueService.getActiveWorkerCount()).thenReturn(4);
            when(executionQueueService.getWaitingQueueSize()).thenReturn(2);
            when(analyticsEventRepository.count()).thenReturn(1500L);

            OperationalAnalyticsResponse response = adminAnalyticsService.getOperationalAnalytics();

            assertThat(response).isNotNull();
            assertThat(response.getActiveExecutionWorkers()).isEqualTo(4);
            assertThat(response.getCurrentQueueDepth()).isEqualTo(2);
            assertThat(response.getTotalEventsLogged()).isEqualTo(1500L);
            assertThat(response.getSystemStatus()).isEqualTo("HEALTHY");
        }

        @Test
        @DisplayName("Should return paginated activity timeline")
        void shouldReturnActivityTimeline() {
            AnalyticsEvent event = AnalyticsEvent.builder()
                    .id("evt-1")
                    .eventType(AnalyticsEventType.ASSESSMENT_ACTIVATED)
                    .entityType(AnalyticsEntityType.ASSESSMENT)
                    .entityId("asmt-100")
                    .actorType(AnalyticsActorType.ADMIN)
                    .actorId("admin-1")
                    .assessmentId("asmt-100")
                    .timestamp(Instant.now())
                    .attributes(Map.of("title", "Spring Test", "isManual", true))
                    .build();

            Pageable pageable = PageRequest.of(0, 10);
            when(analyticsEventRepository.findAllByOrderByTimestampDesc(pageable))
                    .thenReturn(new PageImpl<>(List.of(event), pageable, 1));

            PageResponse<ActivityTimelineItemResponse> result = adminAnalyticsService.getActivityTimeline(null, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().getFirst().getEventType()).isEqualTo(AnalyticsEventType.ASSESSMENT_ACTIVATED);
            assertThat(result.getItems().getFirst().getEntityId()).isEqualTo("asmt-100");
        }
    }
}
