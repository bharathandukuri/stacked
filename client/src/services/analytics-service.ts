import API from "@/lib/api-client"
import type { ApiResponse, PageResponse } from "@/types/api"
import type {
  ActivityTimelineItemResponse,
  AnalyticsDateRangeParams,
  AssessmentAnalyticsResponse,
  ContentAnalyticsResponse,
  OperationalAnalyticsResponse,
  PlatformOverviewResponse,
  SubmissionAnalyticsResponse,
  TimelineFilterParams,
} from "@/types/analytics"

export const analyticsService = {
  /**
   * Authoritative real-time platform overview counters and operational rates
   */
  async getOverview(): Promise<PlatformOverviewResponse> {
    const res = await API.get<ApiResponse<PlatformOverviewResponse>>(
      "/admin/analytics/overview"
    )
    return res.data.data
  },

  /**
   * Content repository breakdown (coding, mcqs, question banks) and creation trends
   */
  async getContentAnalytics(
    params?: AnalyticsDateRangeParams
  ): Promise<ContentAnalyticsResponse> {
    const res = await API.get<ApiResponse<ContentAnalyticsResponse>>(
      "/admin/analytics/content",
      { params }
    )
    return res.data.data
  },

  /**
   * Assessment lifecycle, candidate participation funnel, and completion metrics
   */
  async getAssessmentAnalytics(
    params?: AnalyticsDateRangeParams
  ): Promise<AssessmentAnalyticsResponse> {
    const res = await API.get<ApiResponse<AssessmentAnalyticsResponse>>(
      "/admin/analytics/assessments",
      { params }
    )
    return res.data.data
  },

  /**
   * Code submission metrics, verdicts breakdown, languages, and judge latency
   */
  async getSubmissionAnalytics(
    params?: AnalyticsDateRangeParams
  ): Promise<SubmissionAnalyticsResponse> {
    const res = await API.get<ApiResponse<SubmissionAnalyticsResponse>>(
      "/admin/analytics/submissions",
      { params }
    )
    return res.data.data
  },

  /**
   * Live operational metrics: queue depth, worker threads, capacity, total events
   */
  async getOperationalAnalytics(): Promise<OperationalAnalyticsResponse> {
    const res = await API.get<ApiResponse<OperationalAnalyticsResponse>>(
      "/admin/analytics/operational"
    )
    return res.data.data
  },

  /**
   * Paginated activity audit feed
   */
  async getActivityTimeline(
    params?: TimelineFilterParams
  ): Promise<PageResponse<ActivityTimelineItemResponse>> {
    const res = await API.get<
      ApiResponse<PageResponse<ActivityTimelineItemResponse>>
    >("/admin/analytics/timeline", { params })
    return res.data.data
  },
}

