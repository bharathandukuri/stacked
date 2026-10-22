package com.bharath.stacked.modules.assessment.service.impl;

import com.bharath.stacked.modules.assessment.dto.*;
import com.bharath.stacked.modules.assessment.enums.*;
import com.bharath.stacked.modules.assessment.model.*;
import com.bharath.stacked.modules.assessment.repository.*;
import com.bharath.stacked.modules.coding_problem.judge.CodingProblemJudgeService;
import com.bharath.stacked.modules.coding_problem.repository.CodingProblemRepository;
import com.bharath.stacked.modules.execution.resolver.ExecutionConstraintResolver;
import com.bharath.stacked.modules.language.registry.LanguageRegistry;
import com.bharath.stacked.modules.mcq.repository.McqQuestionRepository;
import com.bharath.stacked.modules.question_bank.enums.BankQuestionType;
import com.bharath.stacked.modules.question_bank.repository.QuestionBankRepository;
import com.bharath.stacked.modules.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssessmentAuditAndResultsServiceTest {

    @Mock
    private AssessmentRepository assessmentRepository;
    @Mock
    private AssessmentAssignmentRepository assignmentRepository;
    @Mock
    private AssessmentAttemptRepository attemptRepository;
    @Mock
    private AssessmentQuestionResponseRepository responseRepository;
    @Mock
    private AssessmentConnectionEventRepository connectionEventRepository;
    @Mock
    private AssessmentAuditEventRepository auditEventRepository;
    @Mock
    private QuestionBankRepository questionBankRepository;
    @Mock
    private CodingProblemRepository codingProblemRepository;
    @Mock
    private McqQuestionRepository mcqQuestionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CodingProblemJudgeService judgeService;
    @Mock
    private LanguageRegistry languageRegistry;
    @Mock
    private ExecutionConstraintResolver executionConstraintResolver;
    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private AssessmentServiceImpl assessmentService;

    private Assessment assessment;
    private AssessmentAttempt attempt;
    private AssessmentAssignment assignment;

    @BeforeEach
    void setUp() {
        assessment = Assessment.builder()
                .id("ass-1")
                .slug("tech-screen-2026")
                .title("Technical Screening 2026")
                .description("Core Java and Algorithms")
                .status(AssessmentStatus.ACTIVE)
                .durationMinutes(60)
                .totalQuestions(2)
                .totalScore(100.0)
                .passingScore(60.0)
                .mcqCount(1)
                .codingCount(1)
                .items(List.of(
                        AssessmentItem.builder()
                                .questionId("q-mcq-1")
                                .questionType(BankQuestionType.MCQ)
                                .points(40.0)
                                .snapshot(QuestionSnapshot.builder()
                                        .title("Java Immutability")
                                        .mcqContent("Which of the following is immutable?")
                                        .options(List.of(
                                                OptionSnapshot.builder().id("opt-1").content("java.lang.String").order(0).build(),
                                                OptionSnapshot.builder().id("opt-2").content("java.lang.StringBuilder").order(1).build()
                                        ))
                                        .correctOptionIds(List.of("opt-1"))
                                        .build())
                                .build(),
                        AssessmentItem.builder()
                                .questionId("q-code-2")
                                .questionType(BankQuestionType.CODING)
                                .points(60.0)
                                .snapshot(QuestionSnapshot.builder()
                                        .title("Two Sum")
                                        .codingStatement("Return indices of two numbers that add up to target.")
                                        .build())
                                .build()
                ))
                .build();

        attempt = AssessmentAttempt.builder()
                .id("att-101")
                .assessmentId("ass-1")
                .candidateUserId("user-42")
                .candidateEmail("candidate@test.com")
                .attemptNumber(1)
                .status(AttemptStatus.SUBMITTED)
                .startedAt(Instant.now().minusSeconds(1800))
                .effectiveDeadline(Instant.now().plusSeconds(1800))
                .submittedAt(Instant.now())
                .score(100.0)
                .maxPossibleScore(100.0)
                .passed(true)
                .build();

        assignment = AssessmentAssignment.builder()
                .id("as-1")
                .assessmentId("ass-1")
                .candidateUserId("user-42")
                .candidateEmail("candidate@test.com")
                .status(CandidateParticipationStatus.SUBMITTED)
                .lastAttemptId("att-101")
                .build();
    }

    @Test
    @DisplayName("recordTelemetry persists CLIENT_TELEMETRY audit events")
    void recordTelemetry_ShouldPersistClientTelemetryEvents() {
        when(assessmentRepository.findById("ass-1")).thenReturn(Optional.of(assessment));
        when(attemptRepository.findByAssessmentIdAndCandidateUserIdAndStatus("ass-1", "user-42", AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(attempt));

        ClientTelemetryEventDto navEvent = ClientTelemetryEventDto.builder()
                .eventType(AssessmentAuditEventType.NAVIGATE_QUESTION)
                .questionId("q-code-2")
                .metadata(Map.of("fromIndex", 0, "toIndex", 1))
                .clientTimestamp(Instant.now())
                .build();

        ClientTelemetryEventDto blurEvent = ClientTelemetryEventDto.builder()
                .eventType(AssessmentAuditEventType.WINDOW_BLUR)
                .metadata(Map.of("durationMs", 1500))
                .clientTimestamp(Instant.now())
                .build();

        assessmentService.recordTelemetry("ass-1", "user-42", List.of(navEvent, blurEvent), "127.0.0.1", "Mozilla/5.0");

        ArgumentCaptor<AssessmentAuditEvent> captor = ArgumentCaptor.forClass(AssessmentAuditEvent.class);
        verify(auditEventRepository, times(2)).save(captor.capture());

        List<AssessmentAuditEvent> savedEvents = captor.getAllValues();
        assertThat(savedEvents).hasSize(2);

        AssessmentAuditEvent first = savedEvents.get(0);
        assertThat(first.getEventType()).isEqualTo(AssessmentAuditEventType.NAVIGATE_QUESTION);
        assertThat(first.getSource()).isEqualTo(AssessmentAuditEventSource.CLIENT_TELEMETRY);
        assertThat(first.getQuestionId()).isEqualTo("q-code-2");

        AssessmentAuditEvent second = savedEvents.get(1);
        assertThat(second.getEventType()).isEqualTo(AssessmentAuditEventType.WINDOW_BLUR);
        assertThat(second.getSource()).isEqualTo(AssessmentAuditEventSource.CLIENT_TELEMETRY);
    }

    @Test
    @DisplayName("getCandidateResults returns accurate score, completion, and question outcomes")
    void getCandidateResults_ShouldReturnComprehensiveResults() {
        when(assessmentRepository.findById("ass-1")).thenReturn(Optional.of(assessment));
        when(assignmentRepository.findByAssessmentIdAndCandidateUserId("ass-1", "user-42")).thenReturn(Optional.of(assignment));
        when(attemptRepository.findById("att-101")).thenReturn(Optional.of(attempt));

        AssessmentQuestionResponse mcqResp = AssessmentQuestionResponse.builder()
                .attemptId("att-101")
                .questionId("q-mcq-1")
                .questionType(BankQuestionType.MCQ)
                .selectedOptionIds(List.of("opt-1"))
                .isCorrect(true)
                .awardedPoints(40.0)
                .maxPoints(40.0)
                .build();

        AssessmentQuestionResponse codeResp = AssessmentQuestionResponse.builder()
                .attemptId("att-101")
                .questionId("q-code-2")
                .questionType(BankQuestionType.CODING)
                .languageId("java-21")
                .code("class Solution { ... }")
                .allTestcasesPassed(true)
                .awardedPoints(60.0)
                .maxPoints(60.0)
                .build();

        when(responseRepository.findAllByAttemptId("att-101")).thenReturn(List.of(mcqResp, codeResp));

        CandidateAssessmentResultResponse result = assessmentService.getCandidateResults("ass-1", "user-42");

        assertThat(result).isNotNull();
        assertThat(result.getAssessmentId()).isEqualTo("ass-1");
        assertThat(result.getScore()).isEqualTo(100.0);
        assertThat(result.getMaxPossibleScore()).isEqualTo(100.0);
        assertThat(result.getPercentage()).isEqualTo(100.0);
        assertThat(result.getPassed()).isTrue();
        assertThat(result.getAnsweredCount()).isEqualTo(2);
        assertThat(result.getUnansweredCount()).isEqualTo(0);
        assertThat(result.getQuestionResults()).hasSize(2);

        QuestionResultSummaryDto q1 = result.getQuestionResults().get(0);
        assertThat(q1.getQuestionId()).isEqualTo("q-mcq-1");
        assertThat(q1.isAnswered()).isTrue();
        assertThat(q1.getIsCorrect()).isTrue();
        assertThat(q1.getPointsAwarded()).isEqualTo(40.0);
        assertThat(q1.getSelectedOptionIds()).containsExactly("opt-1");

        QuestionResultSummaryDto q2 = result.getQuestionResults().get(1);
        assertThat(q2.getQuestionId()).isEqualTo("q-code-2");
        assertThat(q2.isAnswered()).isTrue();
        assertThat(q2.getIsCorrect()).isTrue();
        assertThat(q2.getPointsAwarded()).isEqualTo(60.0);
    }

    @Test
    @DisplayName("getAssessmentMetrics aggregates completion rate, pass rate, and question metrics")
    void getAssessmentMetrics_ShouldComputeAggregatedDashboardMetrics() {
        when(assessmentRepository.findById("ass-1")).thenReturn(Optional.of(assessment));
        when(assignmentRepository.findAllByAssessmentId("ass-1")).thenReturn(List.of(assignment));
        when(attemptRepository.findAllByAssessmentId("ass-1")).thenReturn(List.of(attempt));
        when(auditEventRepository.countByAssessmentIdAndEventType("ass-1", AssessmentAuditEventType.WINDOW_BLUR)).thenReturn(1L);
        when(auditEventRepository.countByAssessmentIdAndEventType("ass-1", AssessmentAuditEventType.STALE_SESSION_REPLACED)).thenReturn(0L);
        when(auditEventRepository.countByAssessmentIdAndEventType("ass-1", AssessmentAuditEventType.CODE_SUBMITTED)).thenReturn(2L);

        AssessmentMetricsResponse metrics = assessmentService.getAssessmentMetrics("ass-1");

        assertThat(metrics).isNotNull();
        assertThat(metrics.getAssessmentId()).isEqualTo("ass-1");
        assertThat(metrics.getTotalCandidatesAssigned()).isEqualTo(1);
        assertThat(metrics.getSubmittedCount()).isEqualTo(1);
        assertThat(metrics.getCompletionRate()).isEqualTo(100.0);
        assertThat(metrics.getPassRate()).isEqualTo(100.0);
        assertThat(metrics.getAverageScore()).isEqualTo(100.0);
        assertThat(metrics.getTotalIntegrityFlags()).isEqualTo(1);
        assertThat(metrics.getQuestionMetrics()).hasSize(2);
    }

    @Test
    @DisplayName("getAttemptAuditTrail returns chronological events for an attempt")
    void getAttemptAuditTrail_ShouldReturnOrderedEvents() {
        AssessmentAuditEvent ev1 = AssessmentAuditEvent.builder()
                .attemptId("att-101")
                .eventType(AssessmentAuditEventType.SESSION_STARTED)
                .timestamp(Instant.now().minusSeconds(100))
                .build();

        AssessmentAuditEvent ev2 = AssessmentAuditEvent.builder()
                .attemptId("att-101")
                .eventType(AssessmentAuditEventType.CODE_SUBMITTED)
                .timestamp(Instant.now().minusSeconds(10))
                .build();

        when(auditEventRepository.findAllByAttemptIdOrderByTimestampAsc("att-101")).thenReturn(List.of(ev1, ev2));

        List<AssessmentAuditEvent> trail = assessmentService.getAttemptAuditTrail("att-101");
        assertThat(trail).hasSize(2);
        assertThat(trail.get(0).getEventType()).isEqualTo(AssessmentAuditEventType.SESSION_STARTED);
        assertThat(trail.get(1).getEventType()).isEqualTo(AssessmentAuditEventType.CODE_SUBMITTED);
    }
}
