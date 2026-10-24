import { useEffect, useMemo, useState } from "react"
import { createFileRoute, useNavigate } from "@tanstack/react-router"
import {
  LayoutDashboard,
  GraduationCap,
  FileCode2,
  Clock,
  Loader2,
  AlertCircle,
  RefreshCw,
} from "lucide-react"
import { format, subDays } from "date-fns"

import { AdminSidebar } from "@/components/admin/sidebar"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Button } from "@/components/ui/button"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import {
  useIsAuthenticated,
  useIsInitialized,
} from "@/stores/auth-store"
import { initializeAuthSession } from "@/hooks/use-auth"

import {
  AnalyticsHeader,
  type DateRangeDays,
} from "@/components/admin/analytics/analytics-header"
import { KpiMetricCards } from "@/components/admin/analytics/kpi-metric-cards"
import { OverviewTab } from "@/components/admin/analytics/tabs/overview-tab"
import { AssessmentFunnelTab } from "@/components/admin/analytics/tabs/assessment-funnel-tab"
import { SubmissionsTab } from "@/components/admin/analytics/tabs/submissions-tab"
import { TimelineTab } from "@/components/admin/analytics/tabs/timeline-tab"

import {
  useAssessmentAnalyticsQuery,
  useContentAnalyticsQuery,
  useOperationalAnalyticsQuery,
  usePlatformOverviewQuery,
  useSubmissionAnalyticsQuery,
} from "@/hooks/use-analytics"

export const Route = createFileRoute("/admin/")({
  component: AdminDashboardPage,
})

export function AdminDashboardPage() {
  const navigate = useNavigate()
  const isAuthenticated = useIsAuthenticated()
  const isInitialized = useIsInitialized()

  // State: Filter Days and Active Tab
  const [days, setDays] = useState<DateRangeDays>(30)
  const [activeTab, setActiveTab] = useState("overview")
  const [lastRefreshedAt, setLastRefreshedAt] = useState<Date>(new Date())

  // Calculate ISO YYYY-MM-DD date range
  const dateRange = useMemo(() => {
    const end = new Date()
    const start = subDays(end, days)
    return {
      startDate: format(start, "yyyy-MM-dd"),
      endDate: format(end, "yyyy-MM-dd"),
    }
  }, [days])

  // Queries
  const overviewQuery = usePlatformOverviewQuery()
  const contentQuery = useContentAnalyticsQuery(dateRange)
  const assessmentsQuery = useAssessmentAnalyticsQuery(dateRange)
  const submissionsQuery = useSubmissionAnalyticsQuery(dateRange)
  const operationalQuery = useOperationalAnalyticsQuery()

  // Session verification and initialization gate
  useEffect(() => {
    let isMounted = true

    async function verifySession() {
      if (!isInitialized) {
        const hasActiveSession = await initializeAuthSession()
        if (isMounted && !hasActiveSession) {
          await navigate({ to: "/admin/login" })
        }
      } else if (!isAuthenticated) {
        await navigate({ to: "/admin/login" })
      }
    }

    verifySession()

    return () => {
      isMounted = false
    }
  }, [isInitialized, isAuthenticated, navigate])

  const handleRefresh = async () => {
    setLastRefreshedAt(new Date())
    await Promise.all([
      overviewQuery.refetch(),
      contentQuery.refetch(),
      assessmentsQuery.refetch(),
      submissionsQuery.refetch(),
      operationalQuery.refetch(),
    ])
  }

  const isRefreshing =
    overviewQuery.isFetching ||
    contentQuery.isFetching ||
    assessmentsQuery.isFetching ||
    submissionsQuery.isFetching ||
    operationalQuery.isFetching

  const hasAnyError =
    overviewQuery.isError ||
    contentQuery.isError ||
    assessmentsQuery.isError ||
    submissionsQuery.isError

  // Display clean loading state while verifying session
  if (!isInitialized) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-3 bg-background">
        <Loader2 className="size-8 animate-spin text-primary" />
        <span className="text-sm font-medium text-muted-foreground">
          Verifying administrator session...
        </span>
      </div>
    )
  }

  // If unauthenticated, redirect is in flight
  if (!isAuthenticated) {
    return null
  }

  return (
    <div className="flex h-screen w-full overflow-hidden bg-background text-foreground">
      {/* Admin Sidebar */}
      <AdminSidebar currentPath="/admin" />

      {/* Main Content Area */}
      <main className="flex-1 overflow-y-auto">
        <div className="mx-auto max-w-7xl space-y-6 p-6 sm:p-8">
          {/* Header & Date Range Controls */}
          <AnalyticsHeader
            days={days}
            onDaysChange={setDays}
            onRefresh={handleRefresh}
            isRefreshing={isRefreshing}
            lastUpdated={lastRefreshedAt}
          />

          {/* Error Banner with Retry */}
          {hasAnyError && (
            <Alert variant="destructive" className="border-destructive/40 bg-destructive/10">
              <AlertCircle className="size-4" />
              <AlertTitle className="text-sm font-semibold">
                Unable to synchronize complete telemetry
              </AlertTitle>
              <AlertDescription className="flex items-center justify-between text-xs mt-1">
                <span>
                  Some analytics streams failed to respond. Metrics might show cached or incomplete data.
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleRefresh}
                  className="h-7 gap-1 text-xs border-destructive/40 text-destructive hover:bg-destructive/10"
                >
                  <RefreshCw className="size-3" /> Retry
                </Button>
              </AlertDescription>
            </Alert>
          )}

          {/* Top KPI Metrics Row */}
          <KpiMetricCards
            overview={overviewQuery.data}
            operational={operationalQuery.data}
            isLoading={overviewQuery.isLoading || operationalQuery.isLoading}
          />

          {/* Tabbed Visualizations & Feeds */}
          <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-6">
            <TabsList className="bg-muted/60 p-1 border border-border/60">
              <TabsTrigger value="overview" className="gap-2 text-xs">
                <LayoutDashboard className="size-3.5" />
                Overview & Infrastructure
              </TabsTrigger>
              <TabsTrigger value="funnel" className="gap-2 text-xs">
                <GraduationCap className="size-3.5" />
                Assessment Funnel & Modes
              </TabsTrigger>
              <TabsTrigger value="submissions" className="gap-2 text-xs">
                <FileCode2 className="size-3.5" />
                Submissions & Judging
              </TabsTrigger>
              <TabsTrigger value="timeline" className="gap-2 text-xs">
                <Clock className="size-3.5" />
                Activity Timeline Feed
              </TabsTrigger>
            </TabsList>

            {/* TAB 1: OVERVIEW & INFRASTRUCTURE */}
            <TabsContent value="overview">
              <OverviewTab
                overview={overviewQuery.data}
                content={contentQuery.data}
                assessments={assessmentsQuery.data}
                operational={operationalQuery.data}
                isLoading={contentQuery.isLoading || assessmentsQuery.isLoading}
              />
            </TabsContent>

            {/* TAB 2: ASSESSMENT FUNNEL & MODES */}
            <TabsContent value="funnel">
              <AssessmentFunnelTab
                assessments={assessmentsQuery.data}
                isLoading={assessmentsQuery.isLoading}
              />
            </TabsContent>

            {/* TAB 3: SUBMISSIONS & JUDGING */}
            <TabsContent value="submissions">
              <SubmissionsTab
                submissions={submissionsQuery.data}
                isLoading={submissionsQuery.isLoading}
              />
            </TabsContent>

            {/* TAB 4: ACTIVITY AUDIT TIMELINE */}
            <TabsContent value="timeline">
              <TimelineTab />
            </TabsContent>
          </Tabs>
        </div>
      </main>
    </div>
  )
}
