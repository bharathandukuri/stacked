package com.bharath.stacked.modules.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionAnalyticsResponse {

    private long totalSubmissions;
    private long totalInteractiveRuns;

    private long acceptedSubmissions;
    private long rejectedSubmissions;
    private double passRate;

    private double avgExecutionTimeMs;

    @Builder.Default
    private Map<String, Long> verdictDistribution = new HashMap<>();

    @Builder.Default
    private Map<String, Long> languageDistribution = new HashMap<>();

    @Builder.Default
    private List<AnalyticsTrendPoint> trends = new ArrayList<>();
}

