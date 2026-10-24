package com.bharath.stacked.modules.analytics.service;

import com.bharath.stacked.common.api.PageResponse;
import com.bharath.stacked.modules.analytics.dto.*;
import com.bharath.stacked.modules.analytics.enums.AnalyticsEventType;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface AdminAnalyticsService {

    /**
     * Real-time authoritative platform summary counters and health indicators.
     */
    PlatformOverviewResponse getPlatformOverview();

    /**
     * Content creation and publishing trends across coding, MCQ, and question banks.
     */
    ContentAnalyticsResponse getContentAnalytics(LocalDate startDate, LocalDate endDate);

    /**
     * Assessment lifecycle, candidate participation funnel, and completion rates.
     */
    AssessmentAnalyticsResponse getAssessmentAnalytics(LocalDate startDate, LocalDate endDate);

    /**
     * Submission volume, pass rates, verdict breakdown, and language distribution.
     */
    SubmissionAnalyticsResponse getSubmissionAnalytics(LocalDate startDate, LocalDate endDate);

    /**
     * Live execution queue metrics, worker capacity, and event throughput.
     */
    OperationalAnalyticsResponse getOperationalAnalytics();

    /**
     * Paginated audit and business activity feed.
     */
    PageResponse<ActivityTimelineItemResponse> getActivityTimeline(AnalyticsEventType eventType, Pageable pageable);
}

