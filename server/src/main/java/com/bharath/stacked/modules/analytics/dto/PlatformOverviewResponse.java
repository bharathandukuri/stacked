package com.bharath.stacked.modules.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformOverviewResponse {

    // Content counts
    private long totalCodingProblems;
    private long totalMcqs;
    private long totalQuestionBanks;

    // Assessment lifecycle counts
    private long totalAssessments;
    private long activeAssessments;
    private long completedAssessments;
    private long scheduledAssessments;

    // Candidate participation
    private long totalCandidates;
    private long totalEnrollments;
    private long totalAttemptsStarted;
    private long totalAttemptsSubmitted;

    // Derived operational rates
    private double overallCompletionRate;
    private double avgAttemptDurationMinutes;
}

