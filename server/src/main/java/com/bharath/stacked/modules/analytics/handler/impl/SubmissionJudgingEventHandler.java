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
public class SubmissionJudgingEventHandler implements AnalyticsEventHandler {

    private final DailyMetricUpdater metricUpdater;

    private static final Set<AnalyticsEventType> SUPPORTED = Set.of(
            AnalyticsEventType.CODE_SUBMISSION_EVALUATED,
            AnalyticsEventType.SAMPLE_RUN_EXECUTED
    );

    @Override
    public boolean supports(AnalyticsEventType eventType) {
        return SUPPORTED.contains(eventType);
    }

    @Override
    public void handle(AnalyticsEvent event) {
        LocalDate date = event.getTimestamp().atZone(ZoneOffset.UTC).toLocalDate();
        String metricKey = event.getEventType().name();

        double executionTimeMs = 0.0;
        Object timeObj = event.getAttributes().get("executionTimeMs");
        if (timeObj instanceof Number n) {
            executionTimeMs = n.doubleValue();
        }

        // Increment overall daily metric for this event type
        metricUpdater.incrementDaily(date, "SUBMISSION", metricKey, 1L, executionTimeMs);

        // Record verdict and language breakdowns for formal code submissions
        if (event.getEventType() == AnalyticsEventType.CODE_SUBMISSION_EVALUATED) {
            String verdict = String.valueOf(event.getAttributes().getOrDefault("verdict", "UNKNOWN"));
            metricUpdater.incrementDailySubCounter(date, "SUBMISSION", "SUBMISSION_BY_VERDICT", verdict, 1L);

            String languageId = String.valueOf(event.getAttributes().getOrDefault("languageId", "unknown"));
            metricUpdater.incrementDailySubCounter(date, "SUBMISSION", "SUBMISSION_BY_LANGUAGE", languageId, 1L);
        }
    }
}

