package com.bharath.stacked.modules.analytics.controller;

import com.bharath.stacked.common.api.ApiResponse;
import com.bharath.stacked.common.api.PageResponse;
import com.bharath.stacked.modules.analytics.dto.*;
import com.bharath.stacked.modules.analytics.enums.AnalyticsEventType;
import com.bharath.stacked.modules.analytics.service.AdminAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/admin/analytics")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final AdminAnalyticsService analyticsService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<PlatformOverviewResponse>> getOverview() {
        PlatformOverviewResponse response = analyticsService.getPlatformOverview();
        return ApiResponse.success(response, "Platform overview analytics retrieved successfully");
    }

    @GetMapping("/content")
    public ResponseEntity<ApiResponse<ContentAnalyticsResponse>> getContentAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        ContentAnalyticsResponse response = analyticsService.getContentAnalytics(startDate, endDate);
        return ApiResponse.success(response, "Content analytics retrieved successfully");
    }

    @GetMapping("/assessments")
    public ResponseEntity<ApiResponse<AssessmentAnalyticsResponse>> getAssessmentAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        AssessmentAnalyticsResponse response = analyticsService.getAssessmentAnalytics(startDate, endDate);
        return ApiResponse.success(response, "Assessment analytics retrieved successfully");
    }

    @GetMapping("/submissions")
    public ResponseEntity<ApiResponse<SubmissionAnalyticsResponse>> getSubmissionAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        SubmissionAnalyticsResponse response = analyticsService.getSubmissionAnalytics(startDate, endDate);
        return ApiResponse.success(response, "Submission analytics retrieved successfully");
    }

    @GetMapping("/operational")
    public ResponseEntity<ApiResponse<OperationalAnalyticsResponse>> getOperationalAnalytics() {
        OperationalAnalyticsResponse response = analyticsService.getOperationalAnalytics();
        return ApiResponse.success(response, "Operational analytics retrieved successfully");
    }

    @GetMapping("/timeline")
    public ResponseEntity<ApiResponse<PageResponse<ActivityTimelineItemResponse>>> getActivityTimeline(
            @RequestParam(required = false) AnalyticsEventType eventType,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<ActivityTimelineItemResponse> response = analyticsService.getActivityTimeline(eventType, pageable);
        return ApiResponse.success(response, "Activity timeline retrieved successfully");
    }
}

