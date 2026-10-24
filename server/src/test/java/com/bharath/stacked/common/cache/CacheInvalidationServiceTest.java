package com.bharath.stacked.common.cache;

import com.bharath.stacked.modules.analytics.enums.AnalyticsEventType;
import com.bharath.stacked.modules.analytics.model.AnalyticsEvent;
import com.bharath.stacked.modules.question_bank.model.QuestionBank;
import com.bharath.stacked.modules.question_bank.repository.QuestionBankRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheInvalidationServiceTest {

    @Mock
    private StackedCacheService cacheService;

    @Mock
    private QuestionBankRepository questionBankRepository;

    @Captor
    private ArgumentCaptor<Collection<String>> keysCaptor;

    private CacheInvalidationService invalidationService;

    @BeforeEach
    void setUp() {
        invalidationService = new CacheInvalidationService(cacheService, questionBankRepository);
    }

    @Test
    @DisplayName("Invalidating coding problem evicts default page, details, analytics, and affected question banks")
    void invalidateCodingProblemWithQuestionBanks() {
        QuestionBank bank = QuestionBank.builder().id("qb-99").build();
        when(questionBankRepository.findAllByItemsQuestionId("prob-1")).thenReturn(List.of(bank));

        invalidationService.invalidateCodingProblem("prob-1", "two-sum");

        verify(cacheService).evict(keysCaptor.capture());
        Collection<String> evicted = keysCaptor.getValue();

        assertThat(evicted).contains(
                CacheKeys.codingProblemDefaultPage(),
                CacheKeys.codingProblemDetail("prob-1"),
                CacheKeys.codingProblemDetail("two-sum"),
                CacheKeys.analyticsOverview(),
                CacheKeys.analyticsContentDefault(),
                CacheKeys.questionBankDetail("qb-99"),
                CacheKeys.assessmentBankPreview("qb-99")
        );
    }

    @Test
    @DisplayName("Invalidating MCQ evicts default page, detail, stats, topics, analytics, and affected question banks")
    void invalidateMcq() {
        QuestionBank bank = QuestionBank.builder().id("qb-100").build();
        when(questionBankRepository.findAllByItemsQuestionId("mcq-1")).thenReturn(List.of(bank));

        invalidationService.invalidateMcq("mcq-1");

        verify(cacheService).evict(keysCaptor.capture());
        Collection<String> evicted = keysCaptor.getValue();

        assertThat(evicted).contains(
                CacheKeys.mcqDefaultPage(),
                CacheKeys.mcqDetail("mcq-1"),
                CacheKeys.mcqStats(),
                CacheKeys.mcqTopics(),
                CacheKeys.analyticsOverview(),
                CacheKeys.analyticsContentDefault(),
                CacheKeys.questionBankDetail("qb-100"),
                CacheKeys.assessmentBankPreview("qb-100")
        );
    }

    @Test
    @DisplayName("Invalidating Question Bank evicts default page, detail, categories, stats, and preview")
    void invalidateQuestionBank() {
        invalidationService.invalidateQuestionBank("qb-200");

        verify(cacheService).evict(keysCaptor.capture());
        Collection<String> evicted = keysCaptor.getValue();

        assertThat(evicted).contains(
                CacheKeys.questionBankDefaultPage(),
                CacheKeys.questionBankDetail("qb-200"),
                CacheKeys.questionBankCategories(),
                CacheKeys.questionBankStats(),
                CacheKeys.assessmentBankPreview("qb-200"),
                CacheKeys.analyticsOverview(),
                CacheKeys.analyticsContentDefault()
        );
    }

    @Test
    @DisplayName("Invalidating Assessment evicts default page, details, metrics, and analytics")
    void invalidateAssessment() {
        invalidationService.invalidateAssessment("asm-1", "spring-quiz");

        verify(cacheService).evict(keysCaptor.capture());
        Collection<String> evicted = keysCaptor.getValue();

        assertThat(evicted).contains(
                CacheKeys.assessmentDefaultPage(),
                CacheKeys.assessmentDetail("asm-1"),
                CacheKeys.assessmentDetail("spring-quiz"),
                CacheKeys.assessmentMetrics("asm-1"),
                CacheKeys.assessmentMetrics("spring-quiz"),
                CacheKeys.analyticsOverview(),
                CacheKeys.analyticsAssessmentsDefault()
        );
    }

    @Test
    @DisplayName("Invalidating attempt metrics evicts assessment metrics and analytics")
    void invalidateAttemptMetrics() {
        invalidationService.invalidateAssessmentMetrics("asm-500");

        verify(cacheService).evict(keysCaptor.capture());
        Collection<String> evicted = keysCaptor.getValue();

        assertThat(evicted).contains(
                CacheKeys.assessmentMetrics("asm-500"),
                CacheKeys.analyticsOverview(),
                CacheKeys.analyticsAssessmentsDefault()
        );
    }

    @Test
    @DisplayName("EventListener handles AnalyticsEvent and triggers corresponding invalidations")
    void handleAnalyticsEventTriggersEviction() {
        AnalyticsEvent event = AnalyticsEvent.builder()
                .eventType(AnalyticsEventType.ASSESSMENT_ACTIVATED)
                .entityId("asm-300")
                .attributes(Map.of("slug", "active-exam"))
                .build();

        invalidationService.handleAnalyticsEvent(event);

        verify(cacheService).evict(keysCaptor.capture());
        assertThat(keysCaptor.getValue()).contains(
                CacheKeys.assessmentDefaultPage(),
                CacheKeys.assessmentDetail("asm-300"),
                CacheKeys.assessmentDetail("active-exam")
        );
    }
}

