package com.bharath.stacked.modules.analytics.handler.impl;

import com.bharath.stacked.modules.analytics.enums.AnalyticsEventType;
import com.bharath.stacked.modules.analytics.handler.AnalyticsEventHandler;
import com.bharath.stacked.modules.analytics.model.AnalyticsEvent;
import com.bharath.stacked.modules.analytics.service.DailyMetricUpdater;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentLifecycleEventHandler implements AnalyticsEventHandler {

    private final DailyMetricUpdater metricUpdater;

    private static final Set<AnalyticsEventType> SUPPORTED = Set.of(
            AnalyticsEventType.CODING_PROBLEM_CREATED,
            AnalyticsEventType.CODING_PROBLEM_UPDATED,
            AnalyticsEventType.CODING_PROBLEM_PUBLISHED,
            AnalyticsEventType.CODING_PROBLEM_UNPUBLISHED,
            AnalyticsEventType.CODING_PROBLEM_DELETED,
            AnalyticsEventType.MCQ_CREATED,
            AnalyticsEventType.MCQ_UPDATED,
            AnalyticsEventType.MCQ_PUBLISHED,
            AnalyticsEventType.MCQ_UNPUBLISHED,
            AnalyticsEventType.MCQ_DELETED,
            AnalyticsEventType.QUESTION_BANK_CREATED,
            AnalyticsEventType.QUESTION_BANK_UPDATED,
            AnalyticsEventType.QUESTION_BANK_PUBLISHED,
            AnalyticsEventType.QUESTION_BANK_UNPUBLISHED,
            AnalyticsEventType.QUESTION_BANK_DELETED
    );

    @Override
    public boolean supports(AnalyticsEventType eventType) {
        return SUPPORTED.contains(eventType);
    }

    @Override
    public void handle(AnalyticsEvent event) {
        LocalDate date = event.getTimestamp().atZone(ZoneOffset.UTC).toLocalDate();
        String metricKey = event.getEventType().name();
        metricUpdater.incrementDaily(date, "CONTENT", metricKey, 1L, 0.0);
    }
}

