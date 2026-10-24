import { useQuery } from "@tanstack/react-query"
import { analyticsService } from "@/services/analytics-service"
import type {
  AnalyticsDateRangeParams,
  TimelineFilterParams,
} from "@/types/analytics"

export const analyticsQueryKeys = {
  all: ["analytics"] as const,
  overview: () => [...analyticsQueryKeys.all, "overview"] as const,
  content: (params?: AnalyticsDateRangeParams) =>
    [...analyticsQueryKeys.all, "content", params] as const,
  assessments: (params?: AnalyticsDateRangeParams) =>
    [...analyticsQueryKeys.all, "assessments", params] as const,
  submissions: (params?: AnalyticsDateRangeParams) =>
    [...analyticsQueryKeys.all, "submissions", params] as const,
  operational: () => [...analyticsQueryKeys.all, "operational"] as const,
  timeline: (params?: TimelineFilterParams) =>
    [...analyticsQueryKeys.all, "timeline", params] as const,
}

export function usePlatformOverviewQuery() {
  return useQuery({
    queryKey: analyticsQueryKeys.overview(),
    queryFn: () => analyticsService.getOverview(),
    staleTime: 30 * 1000, // 30 seconds
  })
}

export function useContentAnalyticsQuery(params?: AnalyticsDateRangeParams) {
  return useQuery({
    queryKey: analyticsQueryKeys.content(params),
    queryFn: () => analyticsService.getContentAnalytics(params),
    staleTime: 60 * 1000,
  })
}

export function useAssessmentAnalyticsQuery(params?: AnalyticsDateRangeParams) {
  return useQuery({
    queryKey: analyticsQueryKeys.assessments(params),
    queryFn: () => analyticsService.getAssessmentAnalytics(params),
    staleTime: 60 * 1000,
  })
}

export function useSubmissionAnalyticsQuery(params?: AnalyticsDateRangeParams) {
  return useQuery({
    queryKey: analyticsQueryKeys.submissions(params),
    queryFn: () => analyticsService.getSubmissionAnalytics(params),
    staleTime: 60 * 1000,
  })
}

export function useOperationalAnalyticsQuery() {
  return useQuery({
    queryKey: analyticsQueryKeys.operational(),
    queryFn: () => analyticsService.getOperationalAnalytics(),
    refetchInterval: 10 * 1000, // poll operational metrics every 10s for real-time monitoring
    staleTime: 5 * 1000,
  })
}

export function useActivityTimelineQuery(params?: TimelineFilterParams) {
  return useQuery({
    queryKey: analyticsQueryKeys.timeline(params),
    queryFn: () => analyticsService.getActivityTimeline(params),
    staleTime: 30 * 1000,
  })
}

