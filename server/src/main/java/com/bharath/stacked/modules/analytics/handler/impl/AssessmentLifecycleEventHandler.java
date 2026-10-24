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
public class AssessmentLifecycleEventHandler implements AnalyticsEventHandler {

    private final DailyMetricUpdater metricUpdater;

    private static final Set<AnalyticsEventType> SUPPORTED = Set.of(
            AnalyticsEventType.ASSESSMENT_CREATED,
            AnalyticsEventType.ASSESSMENT_ACTIVATED,
            AnalyticsEventType.ASSESSMENT_COMPLETED,
            AnalyticsEventType.CANDIDATE_INVITED,
            AnalyticsEventType.CANDIDATE_ENROLLED
    );

    @Override
    public boolean supports(AnalyticsEventType eventType) {
        return SUPPORTED.contains(eventType);
    }

    @Override
    public void handle(AnalyticsEvent event) {
        LocalDate date = event.getTimestamp().atZone(ZoneOffset.UTC).toLocalDate();
        String metricKey = event.getEventType().name();

        metricUpdater.incrementDaily(date, "ASSESSMENT", metricKey, 1L, 0.0);

        // Distinguish manual vs automatic activation/completion
        if (event.getEventType() == AnalyticsEventType.ASSESSMENT_ACTIVATED ||
                event.getEventType() == AnalyticsEventType.ASSESSMENT_COMPLETED) {
            boolean isManual = Boolean.TRUE.equals(event.getAttributes().get("isManual"));
            String mode = isManual ? "MANUAL" : "AUTOMATIC";
            metricUpdater.incrementDailySubCounter(date, "ASSESSMENT", metricKey + "_BY_MODE", mode, 1L);
        }
    }
}

