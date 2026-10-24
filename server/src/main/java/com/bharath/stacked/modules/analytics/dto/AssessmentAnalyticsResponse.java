package com.bharath.stacked.modules.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentAnalyticsResponse {

    // Lifecycle events in time range
    private long assessmentsCreated;
    private long assessmentsActivated;
    private long assessmentsCompleted;

    // Mode breakdowns
    private long manualActivations;
    private long autoActivations;
    private long manualCompletions;
    private long autoCompletions;

    // Participation funnel
    private long candidatesInvited;
    private long attemptsStarted;
    private long attemptsSubmitted;
    private long attemptsExpired;

    // Key Rates
    private double participationRate;
    private double completionRate;
    private double averageAttemptDurationMinutes;

    @Builder.Default
    private List<AnalyticsTrendPoint> trends = new ArrayList<>();
}

