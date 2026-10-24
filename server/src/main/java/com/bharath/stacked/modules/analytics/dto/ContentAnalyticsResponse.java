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
public class ContentAnalyticsResponse {

    private long codingProblemsPublished;
    private long codingProblemsDraft;

    private long mcqsPublished;
    private long mcqsDraft;

    private long questionBanksPublished;
    private long questionBanksDraft;

    @Builder.Default
    private List<AnalyticsTrendPoint> creationTrends = new ArrayList<>();
}

