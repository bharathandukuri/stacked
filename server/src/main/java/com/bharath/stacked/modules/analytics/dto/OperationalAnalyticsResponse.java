package com.bharath.stacked.modules.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationalAnalyticsResponse {
    private int activeExecutionWorkers;
    private int currentQueueDepth;
    private int maxQueueCapacity;
    private long totalEventsLogged;
    private String systemStatus;
}

