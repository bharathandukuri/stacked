package com.bharath.stacked.common.cache;

import com.bharath.stacked.modules.analytics.enums.AnalyticsEventType;
import com.bharath.stacked.modules.analytics.model.AnalyticsEvent;
import com.bharath.stacked.modules.question_bank.model.QuestionBank;
import com.bharath.stacked.modules.question_bank.repository.QuestionBankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheInvalidationService {

    private final StackedCacheService cacheService;
    private final QuestionBankRepository questionBankRepository;

    /**
     * Invalidate all platform analytics overview and default breakdown caches.
     */
    public void invalidateAnalytics() {
        cacheService.evict(List.of(
                CacheKeys.analyticsOverview(),
                CacheKeys.analyticsContentDefault(),
                CacheKeys.analyticsAssessmentsDefault(),
                CacheKeys.analyticsSubmissionsDefault()
        ));
    }

    /**
     * Invalidate coding problem details, default catalog page, analytics, and affected question banks.
     */
    public void invalidateCodingProblem(String id, String slug) {
        List<String> keys = new ArrayList<>();
        keys.add(CacheKeys.codingProblemDefaultPage());
        keys.add(CacheKeys.analyticsOverview());
        keys.add(CacheKeys.analyticsContentDefault());

        if (id != null && !id.isBlank()) {
            keys.add(CacheKeys.codingProblemDetail(id));
            invalidateReferencingQuestionBanks(id, keys);
        }
        if (slug != null && !slug.isBlank()) {
            keys.add(CacheKeys.codingProblemDetail(slug));
        }

        cacheService.evict(keys);
    }

    /**
     * Invalidate MCQ details, default catalog page, stats, topics, analytics, and affected question banks.
     */
    public void invalidateMcq(String id) {
        List<String> keys = new ArrayList<>();
        keys.add(CacheKeys.mcqDefaultPage());
        keys.add(CacheKeys.mcqStats());
        keys.add(CacheKeys.mcqTopics());
        keys.add(CacheKeys.analyticsOverview());
        keys.add(CacheKeys.analyticsContentDefault());

        if (id != null && !id.isBlank()) {
            keys.add(CacheKeys.mcqDetail(id));
            invalidateReferencingQuestionBanks(id, keys);
        }

        cacheService.evict(keys);
    }

    /**
     * Invalidate MCQ topics list and MCQ statistics.
     */
    public void invalidateMcqTopics() {
        cacheService.evict(List.of(
                CacheKeys.mcqTopics(),
                CacheKeys.mcqStats()
        ));
    }

    /**
     * Invalidate Question Bank details, default catalog page, stats, categories, preview, and analytics.
     */
    public void invalidateQuestionBank(String id) {
        List<String> keys = new ArrayList<>();
        keys.add(CacheKeys.questionBankDefaultPage());
        keys.add(CacheKeys.questionBankCategories());
        keys.add(CacheKeys.questionBankStats());
        keys.add(CacheKeys.analyticsOverview());
        keys.add(CacheKeys.analyticsContentDefault());

        if (id != null && !id.isBlank()) {
            keys.add(CacheKeys.questionBankDetail(id));
            keys.add(CacheKeys.assessmentBankPreview(id));
        }

        cacheService.evict(keys);
    }

    /**
     * Invalidate Assessment details, default catalog page, metrics, and analytics.
     */
    public void invalidateAssessment(String id, String slug) {
        List<String> keys = new ArrayList<>();
        keys.add(CacheKeys.assessmentDefaultPage());
        keys.add(CacheKeys.analyticsOverview());
        keys.add(CacheKeys.analyticsAssessmentsDefault());

        if (id != null && !id.isBlank()) {
            keys.add(CacheKeys.assessmentDetail(id));
            keys.add(CacheKeys.assessmentMetrics(id));
        }
        if (slug != null && !slug.isBlank()) {
            keys.add(CacheKeys.assessmentDetail(slug));
            keys.add(CacheKeys.assessmentMetrics(slug));
        }

        cacheService.evict(keys);
    }

    /**
     * Invalidate Assessment statistical metrics and platform overview when an attempt completes.
     */
    public void invalidateAssessmentMetrics(String assessmentId) {
        if (assessmentId == null || assessmentId.isBlank()) {
            return;
        }
        cacheService.evict(List.of(
                CacheKeys.assessmentMetrics(assessmentId),
                CacheKeys.analyticsOverview(),
                CacheKeys.analyticsAssessmentsDefault()
        ));
    }

    /**
     * Invalidate default submission analytics when code execution verdicts are published.
     */
    public void invalidateSubmissionAnalytics() {
        cacheService.evict(List.of(
                CacheKeys.analyticsSubmissionsDefault()
        ));
    }

    /**
     * Event listener reacting automatically to platform domain events (e.g. background sweeps,
     * schedulers, asynchronous lifecycles).
     */
    @EventListener
    public void handleAnalyticsEvent(AnalyticsEvent event) {
        if (event == null || event.getEventType() == null) {
            return;
        }

        AnalyticsEventType type = event.getEventType();
        String entityId = event.getEntityId();
        String assessmentId = event.getAssessmentId();

        switch (type) {
            case CODING_PROBLEM_CREATED, CODING_PROBLEM_UPDATED, CODING_PROBLEM_PUBLISHED,
                 CODING_PROBLEM_UNPUBLISHED, CODING_PROBLEM_DELETED -> {
                String slug = event.getAttributes() != null ? (String) event.getAttributes().get("slug") : null;
                invalidateCodingProblem(entityId, slug);
            }
            case MCQ_CREATED, MCQ_UPDATED, MCQ_PUBLISHED, MCQ_UNPUBLISHED, MCQ_DELETED ->
                invalidateMcq(entityId);
            case QUESTION_BANK_CREATED, QUESTION_BANK_UPDATED, QUESTION_BANK_PUBLISHED,
                 QUESTION_BANK_UNPUBLISHED, QUESTION_BANK_DELETED ->
                invalidateQuestionBank(entityId);
            case ASSESSMENT_CREATED, ASSESSMENT_ACTIVATED, ASSESSMENT_COMPLETED, ASSESSMENT_DELETED -> {
                String slug = event.getAttributes() != null ? (String) event.getAttributes().get("slug") : null;
                invalidateAssessment(entityId, slug);
            }
            case ATTEMPT_SUBMITTED, ATTEMPT_EXPIRED ->
                invalidateAssessmentMetrics(assessmentId != null ? assessmentId : entityId);
            case CODE_SUBMISSION_EVALUATED ->
                invalidateSubmissionAnalytics();
            default -> {
                // Non-invalidating telemetry events (e.g. ATTEMPT_STARTED, CANDIDATE_ENROLLED)
            }
        }
    }

    private void invalidateReferencingQuestionBanks(String questionId, List<String> keys) {
        try {
            List<QuestionBank> banks = questionBankRepository.findAllByItemsQuestionId(questionId);
            for (QuestionBank bank : banks) {
                keys.add(CacheKeys.questionBankDetail(bank.getId()));
                keys.add(CacheKeys.assessmentBankPreview(bank.getId()));
            }
        } catch (Exception ex) {
            log.warn("Failed to find referencing question banks for question [{}]: {}", questionId, ex.getMessage());
        }
    }
}
