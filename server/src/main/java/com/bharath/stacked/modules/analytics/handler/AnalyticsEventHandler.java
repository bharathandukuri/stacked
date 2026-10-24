package com.bharath.stacked.modules.analytics.handler;

import com.bharath.stacked.modules.analytics.enums.AnalyticsEventType;
import com.bharath.stacked.modules.analytics.model.AnalyticsEvent;

public interface AnalyticsEventHandler {

    /**
     * Determines whether this handler supports the given event type.
     */
    boolean supports(AnalyticsEventType eventType);

    /**
     * Handles the event, typically by updating pre-aggregated daily metrics or summaries.
     */
    void handle(AnalyticsEvent event);
}

