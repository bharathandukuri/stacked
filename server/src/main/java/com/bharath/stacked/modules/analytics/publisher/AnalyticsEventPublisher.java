package com.bharath.stacked.modules.analytics.publisher;

import com.bharath.stacked.modules.analytics.model.AnalyticsEvent;

public interface AnalyticsEventPublisher {

    /**
     * Publishes a business analytics event.
     * Implementations must handle validation, deduplication, asynchronous processing,
     * and error containment so that callers in the business layer are never affected.
     *
     * @param event The typed analytics event to publish
     */
    void publish(AnalyticsEvent event);
}

