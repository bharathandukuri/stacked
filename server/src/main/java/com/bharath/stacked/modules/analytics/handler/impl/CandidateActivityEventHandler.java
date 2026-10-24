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
public class CandidateActivityEventHandler implements AnalyticsEventHandler {

    private final DailyMetricUpdater metricUpdater;

    private static final Set<AnalyticsEventType> SUPPORTED = Set.of(
            AnalyticsEventType.ATTEMPT_STARTED,
            AnalyticsEventType.ANSWER_SAVED,
            AnalyticsEventType.ATTEMPT_SUBMITTED,
            AnalyticsEventType.ATTEMPT_EXPIRED
    );

    @Override
    public boolean supports(AnalyticsEventType eventType) {
        return SUPPORTED.contains(eventType);
    }

    @Override
    public void handle(AnalyticsEvent event) {
        LocalDate date = event.getTimestamp().atZone(ZoneOffset.UTC).toLocalDate();
        String metricKey = event.getEventType().name();

        double numericDelta = 0.0;
        if (event.getEventType() == AnalyticsEventType.ATTEMPT_SUBMITTED) {
            Object durationObj = event.getAttributes().get("durationSeconds");
            if (durationObj instanceof Number n) {
                numericDelta = n.doubleValue();
            }
        }

        metricUpdater.incrementDaily(date, "ATTEMPT", metricKey, 1L, numericDelta);
    }
}

