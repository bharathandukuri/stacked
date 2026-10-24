package com.bharath.stacked.modules.analytics.service.impl;

import com.bharath.stacked.common.api.PageResponse;
import com.bharath.stacked.common.cache.CacheKeys;
import com.bharath.stacked.common.cache.StackedCacheService;
import com.bharath.stacked.config.cache.CacheProperties;
import com.bharath.stacked.exception.BadRequestException;
import com.bharath.stacked.modules.analytics.config.AnalyticsProperties;
import com.bharath.stacked.modules.analytics.dto.*;
import com.bharath.stacked.modules.analytics.enums.AnalyticsEventType;
import com.bharath.stacked.modules.analytics.model.AnalyticsDailyMetric;
import com.bharath.stacked.modules.analytics.model.AnalyticsEvent;
import com.bharath.stacked.modules.analytics.repository.AnalyticsDailyMetricRepository;
import com.bharath.stacked.modules.analytics.repository.AnalyticsEventRepository;
import com.bharath.stacked.modules.analytics.service.AdminAnalyticsService;
import com.bharath.stacked.modules.assessment.enums.AssessmentStatus;
import com.bharath.stacked.modules.assessment.enums.AttemptStatus;
import com.bharath.stacked.modules.assessment.model.AssessmentAttempt;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private final AnalyticsProperties analyticsProperties;
    private final AnalyticsEventRepository eventRepository;
    private final AnalyticsDailyMetricRepository dailyMetricRepository;

    private final CodingProblemRepository codingProblemRepository;
    private final McqQuestionRepository mcqQuestionRepository;
    private final QuestionBankRepository questionBankRepository;

    private final AssessmentRepository assessmentRepository;
    private final AssessmentAssignmentRepository assignmentRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final UserRepository userRepository;

    private final Optional<ExecutionQueueService> executionQueueService;
    private final Optional<StackedCacheService> cacheService;
    private final Optional<CacheProperties> cacheProperties;

    public AdminAnalyticsServiceImpl(AnalyticsProperties analyticsProperties,
                                     AnalyticsEventRepository eventRepository,
                                     AnalyticsDailyMetricRepository dailyMetricRepository,
                                     CodingProblemRepository codingProblemRepository,
                                     McqQuestionRepository mcqQuestionRepository,
                                     QuestionBankRepository questionBankRepository,
                                     AssessmentRepository assessmentRepository,
                                     AssessmentAssignmentRepository assignmentRepository,
                                     AssessmentAttemptRepository attemptRepository,
                                     UserRepository userRepository,
                                     Optional<ExecutionQueueService> executionQueueService) {
        this(analyticsProperties, eventRepository, dailyMetricRepository, codingProblemRepository,
                mcqQuestionRepository, questionBankRepository, assessmentRepository,
                assignmentRepository, attemptRepository, userRepository, executionQueueService,
                Optional.empty(), Optional.empty());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AdminAnalyticsServiceImpl(AnalyticsProperties analyticsProperties,
                                     AnalyticsEventRepository eventRepository,
                                     AnalyticsDailyMetricRepository dailyMetricRepository,
                                     CodingProblemRepository codingProblemRepository,
                                     McqQuestionRepository mcqQuestionRepository,
                                     QuestionBankRepository questionBankRepository,
                                     AssessmentRepository assessmentRepository,
                                     AssessmentAssignmentRepository assignmentRepository,
                                     AssessmentAttemptRepository attemptRepository,
                                     UserRepository userRepository,
                                     Optional<ExecutionQueueService> executionQueueService,
                                     Optional<StackedCacheService> cacheService,
                                     Optional<CacheProperties> cacheProperties) {
        this.analyticsProperties = analyticsProperties;
        this.eventRepository = eventRepository;
        this.dailyMetricRepository = dailyMetricRepository;
        this.codingProblemRepository = codingProblemRepository;
        this.mcqQuestionRepository = mcqQuestionRepository;
        this.questionBankRepository = questionBankRepository;
        this.assessmentRepository = assessmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.attemptRepository = attemptRepository;
        this.userRepository = userRepository;
        this.executionQueueService = executionQueueService;
        this.cacheService = cacheService;
        this.cacheProperties = cacheProperties;
    }

    @Override
    public PlatformOverviewResponse getPlatformOverview() {
        if (cacheService != null && cacheService.isPresent() && cacheProperties != null && cacheProperties.isPresent()) {
            return cacheService.get().getOrCompute(
                    CacheKeys.analyticsOverview(),
                    PlatformOverviewResponse.class,
                    cacheProperties.get().analyticsTtl(),
                    this::computePlatformOverview
            );
        }
        return computePlatformOverview();
    }

    private PlatformOverviewResponse computePlatformOverview() {
        long totalCodingProblems = codingProblemRepository.count();
        long totalMcqs = mcqQuestionRepository.count();
        long totalQuestionBanks = questionBankRepository.count();

        long totalAssessments = assessmentRepository.count();
        long activeAssessments = assessmentRepository.countByStatus(AssessmentStatus.ACTIVE);
        long completedAssessments = assessmentRepository.countByStatus(AssessmentStatus.COMPLETED);
        long scheduledAssessments = assessmentRepository.countByStatus(AssessmentStatus.NOT_STARTED);

        long totalCandidates = userRepository.countByRole(Role.USER);
        long totalEnrollments = assignmentRepository.count();

        long totalAttemptsStarted = attemptRepository.count();
        long totalAttemptsSubmitted = attemptRepository.countByStatus(AttemptStatus.SUBMITTED);

        double overallCompletionRate = totalAttemptsStarted > 0
                ? Math.round(((double) totalAttemptsSubmitted / totalAttemptsStarted) * 1000.0) / 10.0
                : 0.0;

        // Compute average attempt duration across recently finished attempts
        List<AssessmentAttempt> finished = attemptRepository.findAllByStatus(AttemptStatus.SUBMITTED);
        double avgDurationMinutes = 0.0;
        if (!finished.isEmpty()) {
            long totalSeconds = 0;
            int count = 0;
            for (AssessmentAttempt att : finished) {
                if (att.getStartedAt() != null && att.getSubmittedAt() != null) {
                    totalSeconds += Math.max(0, Duration.between(att.getStartedAt(), att.getSubmittedAt()).toSeconds());
                    count++;
                }
            }
            if (count > 0) {
                avgDurationMinutes = Math.round((totalSeconds / (double) count / 60.0) * 10.0) / 10.0;
            }
        }

        return PlatformOverviewResponse.builder()
                .totalCodingProblems(totalCodingProblems)
                .totalMcqs(totalMcqs)
                .totalQuestionBanks(totalQuestionBanks)
                .totalAssessments(totalAssessments)
                .activeAssessments(activeAssessments)
                .completedAssessments(completedAssessments)
                .scheduledAssessments(scheduledAssessments)
                .totalCandidates(totalCandidates)
                .totalEnrollments(totalEnrollments)
                .totalAttemptsStarted(totalAttemptsStarted)
                .totalAttemptsSubmitted(totalAttemptsSubmitted)
                .overallCompletionRate(overallCompletionRate)
                .avgAttemptDurationMinutes(avgDurationMinutes)
                .build();
    }

    @Override
    public ContentAnalyticsResponse getContentAnalytics(LocalDate startDate, LocalDate endDate) {
        if (isDefaultDateRange(startDate, endDate) && cacheService != null && cacheService.isPresent() && cacheProperties != null && cacheProperties.isPresent()) {
            return cacheService.get().getOrCompute(
                    CacheKeys.analyticsContentDefault(),
                    ContentAnalyticsResponse.class,
                    cacheProperties.get().analyticsTtl(),
                    () -> computeContentAnalytics(startDate, endDate)
            );
        }
        return computeContentAnalytics(startDate, endDate);
    }

    private ContentAnalyticsResponse computeContentAnalytics(LocalDate startDate, LocalDate endDate) {
        LocalDate[] range = validateDateRange(startDate, endDate);
        LocalDate start = range[0];
        LocalDate end = range[1];

        long cpPublished = codingProblemRepository.countByStatus(CodingProblemStatus.PUBLISHED);
        long cpDraft = codingProblemRepository.countByStatus(CodingProblemStatus.DRAFT);

        long mcqPublished = mcqQuestionRepository.countByStatus(McqStatus.PUBLISHED);
        long mcqDraft = mcqQuestionRepository.countByStatus(McqStatus.DRAFT);

        long qbPublished = questionBankRepository.countByStatus(QuestionBankStatus.PUBLISHED);
        long qbDraft = questionBankRepository.countByStatus(QuestionBankStatus.DRAFT);

        List<AnalyticsDailyMetric> contentMetrics = dailyMetricRepository.findAllByCategoryAndDateBetweenOrderByDateAsc("CONTENT", start, end);
        List<AnalyticsTrendPoint> trends = new ArrayList<>();
        for (AnalyticsDailyMetric dm : contentMetrics) {
            trends.add(AnalyticsTrendPoint.builder()
                    .date(dm.getDate())
                    .metricKey(dm.getMetricKey())
                    .count(dm.getCount())
                    .value(dm.getNumericSum())
                    .build());
        }

        return ContentAnalyticsResponse.builder()
                .codingProblemsPublished(cpPublished)
                .codingProblemsDraft(cpDraft)
                .mcqsPublished(mcqPublished)
                .mcqsDraft(mcqDraft)
                .questionBanksPublished(qbPublished)
                .questionBanksDraft(qbDraft)
                .creationTrends(trends)
                .build();
    }

    @Override
    public AssessmentAnalyticsResponse getAssessmentAnalytics(LocalDate startDate, LocalDate endDate) {
        if (isDefaultDateRange(startDate, endDate) && cacheService != null && cacheService.isPresent() && cacheProperties != null && cacheProperties.isPresent()) {
            return cacheService.get().getOrCompute(
                    CacheKeys.analyticsAssessmentsDefault(),
                    AssessmentAnalyticsResponse.class,
                    cacheProperties.get().analyticsTtl(),
                    () -> computeAssessmentAnalytics(startDate, endDate)
            );
        }
        return computeAssessmentAnalytics(startDate, endDate);
    }

    private AssessmentAnalyticsResponse computeAssessmentAnalytics(LocalDate startDate, LocalDate endDate) {
        LocalDate[] range = validateDateRange(startDate, endDate);
        LocalDate start = range[0];
        LocalDate end = range[1];

        List<AnalyticsDailyMetric> assessmentMetrics = dailyMetricRepository.findAllByCategoryAndDateBetweenOrderByDateAsc("ASSESSMENT", start, end);
        List<AnalyticsDailyMetric> attemptMetrics = dailyMetricRepository.findAllByCategoryAndDateBetweenOrderByDateAsc("ATTEMPT", start, end);

        long created = 0;
        long activated = 0;
        long completed = 0;
        long manualActivations = 0;
        long autoActivations = 0;
        long manualCompletions = 0;
        long autoCompletions = 0;
        long invited = 0;
        long enrolled = 0;

        for (AnalyticsDailyMetric dm : assessmentMetrics) {
            if ("ASSESSMENT_CREATED".equals(dm.getMetricKey())) created += dm.getCount();
            else if ("ASSESSMENT_ACTIVATED".equals(dm.getMetricKey())) activated += dm.getCount();
            else if ("ASSESSMENT_COMPLETED".equals(dm.getMetricKey())) completed += dm.getCount();
            else if ("ASSESSMENT_ACTIVATED_BY_MODE".equals(dm.getMetricKey())) {
                manualActivations += dm.getSubCounters().getOrDefault("MANUAL", 0L);
                autoActivations += dm.getSubCounters().getOrDefault("AUTOMATIC", 0L);
            } else if ("ASSESSMENT_COMPLETED_BY_MODE".equals(dm.getMetricKey())) {
                manualCompletions += dm.getSubCounters().getOrDefault("MANUAL", 0L);
                autoCompletions += dm.getSubCounters().getOrDefault("AUTOMATIC", 0L);
            } else if ("CANDIDATE_INVITED".equals(dm.getMetricKey())) invited += dm.getCount();
            else if ("CANDIDATE_ENROLLED".equals(dm.getMetricKey())) enrolled += dm.getCount();
        }

        long started = 0;
        long submitted = 0;
        long expired = 0;
        double totalDurationSec = 0.0;

        for (AnalyticsDailyMetric dm : attemptMetrics) {
            if ("ATTEMPT_STARTED".equals(dm.getMetricKey())) started += dm.getCount();
            else if ("ATTEMPT_SUBMITTED".equals(dm.getMetricKey())) {
                submitted += dm.getCount();
                totalDurationSec += dm.getNumericSum();
            } else if ("ATTEMPT_EXPIRED".equals(dm.getMetricKey())) expired += dm.getCount();
        }

        double participationRate = (invited + enrolled) > 0
                ? Math.round(((double) started / (invited + enrolled)) * 1000.0) / 10.0
                : 0.0;

        double completionRate = started > 0
                ? Math.round(((double) submitted / started) * 1000.0) / 10.0
                : 0.0;

        double avgDurationMinutes = submitted > 0
                ? Math.round((totalDurationSec / submitted / 60.0) * 10.0) / 10.0
                : 0.0;

        List<AnalyticsTrendPoint> trends = new ArrayList<>();
        for (AnalyticsDailyMetric dm : assessmentMetrics) {
            trends.add(AnalyticsTrendPoint.builder()
                    .date(dm.getDate())
                    .metricKey(dm.getMetricKey())
                    .count(dm.getCount())
                    .value(dm.getNumericSum())
                    .build());
        }
        for (AnalyticsDailyMetric dm : attemptMetrics) {
            trends.add(AnalyticsTrendPoint.builder()
                    .date(dm.getDate())
                    .metricKey(dm.getMetricKey())
                    .count(dm.getCount())
                    .value(dm.getNumericSum())
                    .build());
        }

        return AssessmentAnalyticsResponse.builder()
                .assessmentsCreated(created)
                .assessmentsActivated(activated)
                .assessmentsCompleted(completed)
                .manualActivations(manualActivations)
                .autoActivations(autoActivations)
                .manualCompletions(manualCompletions)
                .autoCompletions(autoCompletions)
                .candidatesInvited(invited + enrolled)
                .attemptsStarted(started)
                .attemptsSubmitted(submitted)
                .attemptsExpired(expired)
                .participationRate(participationRate)
                .completionRate(completionRate)
                .averageAttemptDurationMinutes(avgDurationMinutes)
                .trends(trends)
                .build();
    }

    @Override
    public SubmissionAnalyticsResponse getSubmissionAnalytics(LocalDate startDate, LocalDate endDate) {
        if (isDefaultDateRange(startDate, endDate) && cacheService != null && cacheService.isPresent() && cacheProperties != null && cacheProperties.isPresent()) {
            return cacheService.get().getOrCompute(
                    CacheKeys.analyticsSubmissionsDefault(),
                    SubmissionAnalyticsResponse.class,
                    cacheProperties.get().analyticsTtl(),
                    () -> computeSubmissionAnalytics(startDate, endDate)
            );
        }
        return computeSubmissionAnalytics(startDate, endDate);
    }

    private SubmissionAnalyticsResponse computeSubmissionAnalytics(LocalDate startDate, LocalDate endDate) {
        LocalDate[] range = validateDateRange(startDate, endDate);
        LocalDate start = range[0];
        LocalDate end = range[1];

        List<AnalyticsDailyMetric> metrics = dailyMetricRepository.findAllByCategoryAndDateBetweenOrderByDateAsc("SUBMISSION", start, end);

        long totalSubmissions = 0;
        long totalInteractive = 0;
        double totalExecutionTime = 0.0;
        Map<String, Long> verdicts = new HashMap<>();
        Map<String, Long> languages = new HashMap<>();

        for (AnalyticsDailyMetric dm : metrics) {
            if ("CODE_SUBMISSION_EVALUATED".equals(dm.getMetricKey())) {
                totalSubmissions += dm.getCount();
                totalExecutionTime += dm.getNumericSum();
            } else if ("SAMPLE_RUN_EXECUTED".equals(dm.getMetricKey())) {
                totalInteractive += dm.getCount();
            } else if ("SUBMISSION_BY_VERDICT".equals(dm.getMetricKey())) {
                dm.getSubCounters().forEach((k, v) -> verdicts.merge(k, v, Long::sum));
            } else if ("SUBMISSION_BY_LANGUAGE".equals(dm.getMetricKey())) {
                dm.getSubCounters().forEach((k, v) -> languages.merge(k, v, Long::sum));
            }
        }

        long accepted = verdicts.getOrDefault("ACCEPTED", 0L);
        long rejected = Math.max(0, totalSubmissions - accepted);
        double passRate = totalSubmissions > 0
                ? Math.round(((double) accepted / totalSubmissions) * 1000.0) / 10.0
                : 0.0;
        double avgExecutionTimeMs = totalSubmissions > 0
                ? Math.round((totalExecutionTime / totalSubmissions) * 10.0) / 10.0
                : 0.0;

        List<AnalyticsTrendPoint> trends = new ArrayList<>();
        for (AnalyticsDailyMetric dm : metrics) {
            trends.add(AnalyticsTrendPoint.builder()
                    .date(dm.getDate())
                    .metricKey(dm.getMetricKey())
                    .count(dm.getCount())
                    .value(dm.getNumericSum())
                    .build());
        }

        return SubmissionAnalyticsResponse.builder()
                .totalSubmissions(totalSubmissions)
                .totalInteractiveRuns(totalInteractive)
                .acceptedSubmissions(accepted)
                .rejectedSubmissions(rejected)
                .passRate(passRate)
                .avgExecutionTimeMs(avgExecutionTimeMs)
                .verdictDistribution(verdicts)
                .languageDistribution(languages)
                .trends(trends)
                .build();
    }

    @Override
    public OperationalAnalyticsResponse getOperationalAnalytics() {
        int activeWorkers = executionQueueService.map(ExecutionQueueService::getActiveWorkerCount).orElse(0);
        int queueDepth = executionQueueService.map(ExecutionQueueService::getWaitingQueueSize).orElse(0);
        int maxCapacity = 50;

        long totalEvents = eventRepository.count();

        return OperationalAnalyticsResponse.builder()
                .activeExecutionWorkers(activeWorkers)
                .currentQueueDepth(queueDepth)
                .maxQueueCapacity(maxCapacity)
                .totalEventsLogged(totalEvents)
                .systemStatus("HEALTHY")
                .build();
    }

    @Override
    public PageResponse<ActivityTimelineItemResponse> getActivityTimeline(AnalyticsEventType eventType, Pageable pageable) {
        Page<AnalyticsEvent> page = eventType != null
                ? eventRepository.findAllByEventTypeOrderByTimestampDesc(eventType, pageable)
                : eventRepository.findAllByOrderByTimestampDesc(pageable);

        List<ActivityTimelineItemResponse> items = page.getContent().stream()
                .map(ActivityTimelineItemResponse::fromEvent)
                .toList();

        return PageResponse.of(items, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    private LocalDate[] validateDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDate end = endDate != null ? endDate : LocalDate.now(ZoneOffset.UTC);
        LocalDate start = startDate != null ? startDate : end.minusDays(30);

        if (start.isAfter(end)) {
            throw new BadRequestException("Start date [" + start + "] cannot be after end date [" + end + "].");
        }

        long daysBetween = ChronoUnit.DAYS.between(start, end);
        if (daysBetween > analyticsProperties.maxQueryDays()) {
            throw new BadRequestException("Date range [" + daysBetween + " days] exceeds maximum allowed query window of " +
                    analyticsProperties.maxQueryDays() + " days.");
        }

        return new LocalDate[]{start, end};
    }

    private boolean isDefaultDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return true;
        }
        LocalDate end = LocalDate.now(ZoneOffset.UTC);
        LocalDate start = end.minusDays(30);
        return start.equals(startDate) && end.equals(endDate);
    }
}

