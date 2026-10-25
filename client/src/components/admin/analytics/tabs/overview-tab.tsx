import { useMemo } from "react"
import {
  AreaChart,
  Area,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from "recharts"
import {
  Activity,
  Layers,
  Cpu,
} from "lucide-react"
import { format, parseISO } from "date-fns"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { Progress } from "@/components/ui/progress"
import { Badge } from "@/components/ui/badge"
import { AnalyticsEmptyState } from "../analytics-empty-state"
import type {
  AssessmentAnalyticsResponse,
  ContentAnalyticsResponse,
  OperationalAnalyticsResponse,
  PlatformOverviewResponse,
} from "@/types/analytics"

interface OverviewTabProps {
  overview?: PlatformOverviewResponse
  content?: ContentAnalyticsResponse
  assessments?: AssessmentAnalyticsResponse
  operational?: OperationalAnalyticsResponse
  isLoading: boolean
}

export function OverviewTab({
  overview,
  content,
  assessments,
  operational,
  isLoading,
}: OverviewTabProps) {
  // Aggregate daily attempt trends (started vs submitted)
  const assessmentTrends = assessments?.trends
  const attemptTrendData = useMemo(() => {
    if (!assessmentTrends || assessmentTrends.length === 0) return []

    const map = new Map<string, { date: string; started: number; submitted: number }>()

    assessmentTrends.forEach((t) => {
      const existing = map.get(t.date) || { date: t.date, started: 0, submitted: 0 }
      if (t.metricKey === "ATTEMPT_STARTED") {
        existing.started += t.count
      } else if (t.metricKey === "ATTEMPT_SUBMITTED") {
        existing.submitted += t.count
      }
      map.set(t.date, existing)
    })

    return Array.from(map.values())
      .sort((a, b) => a.date.localeCompare(b.date))
      .map((d) => {
        let label = d.date
        try {
          label = format(parseISO(d.date), "MMM d")
        } catch {
          // fallback
        }
        return {
          ...d,
          formattedDate: label,
        }
      })
  }, [assessmentTrends])

  // Content published vs draft breakdown
  const contentBalanceData = useMemo(() => {
    if (!content) return []
    return [
      {
        name: "Coding Problems",
        published: content.codingProblemsPublished,
        draft: content.codingProblemsDraft,
        total: content.codingProblemsPublished + content.codingProblemsDraft,
      },
      {
        name: "MCQ Questions",
        published: content.mcqsPublished,
        draft: content.mcqsDraft,
        total: content.mcqsPublished + content.mcqsDraft,
      },
      {
        name: "Question Banks",
        published: content.questionBanksPublished,
        draft: content.questionBanksDraft,
        total: content.questionBanksPublished + content.questionBanksDraft,
      },
    ]
  }, [content])

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="grid gap-6 md:grid-cols-2">
          <Skeleton className="h-[360px] rounded-xl" />
          <Skeleton className="h-[360px] rounded-xl" />
        </div>
        <Skeleton className="h-[180px] rounded-xl" />
      </div>
    )
  }

  const workerPercent = operational
    ? Math.min(100, Math.round((operational.activeExecutionWorkers / 4) * 100))
    : 0
  const queuePercent = operational
    ? Math.min(100, Math.round((operational.currentQueueDepth / operational.maxQueueCapacity) * 100))
    : 0

  return (
    <div className="space-y-6">
      {/* Visual Analytics Grid: 2 Primary Charts */}
      <div className="grid gap-6 lg:grid-cols-2">
        {/* Chart 1: Assessment Activity Trends */}
        <Card className="border-border/80">
          <CardHeader>
            <div className="flex items-center justify-between">
              <div>
                <CardTitle className="text-base font-semibold flex items-center gap-2">
                  <Activity className="size-4 text-primary" />
                  Candidate Attempt Activity
                </CardTitle>
                <CardDescription className="text-xs">
                  Daily trends of attempts started vs successfully finalized
                </CardDescription>
              </div>
              <Badge variant="outline" className="text-xs border-border">
                {attemptTrendData.length} active day(s)
              </Badge>
            </div>
          </CardHeader>
          <CardContent>
            {attemptTrendData.length === 0 ? (
              <AnalyticsEmptyState
                title="No attempt trends in range"
                description="When candidates begin or submit assessments, daily trends will plot here."
                className="py-12 border border-dashed border-border/70"
              />
            ) : (
              <div className="h-[280px] w-full">
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={attemptTrendData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                    <defs>
                      <linearGradient id="startedGrad" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.4} />
                        <stop offset="95%" stopColor="#3b82f6" stopOpacity={0.0} />
                      </linearGradient>
                      <linearGradient id="submittedGrad" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#10b981" stopOpacity={0.5} />
                        <stop offset="95%" stopColor="#10b981" stopOpacity={0.0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" className="stroke-border/50" vertical={false} />
                    <XAxis
                      dataKey="formattedDate"
                      stroke="#888888"
                      fontSize={11}
                      tickLine={false}
                      axisLine={false}
                    />
                    <YAxis
                      stroke="#888888"
                      fontSize={11}
                      tickLine={false}
                      axisLine={false}
                      allowDecimals={false}
                    />
                    <Tooltip
                      content={({ active, payload, label }) => {
                        if (active && payload && payload.length) {
                          return (
                            <div className="rounded-lg border border-border bg-background p-2.5 shadow-md text-xs">
                              <p className="font-semibold mb-1 text-foreground">{label}</p>
                              <div className="flex items-center gap-2 text-blue-500">
                                <span className="size-2 rounded-full bg-blue-500" />
                                <span>Started: {payload[0]?.value ?? 0}</span>
                              </div>
                              <div className="flex items-center gap-2 text-emerald-500 mt-0.5">
                                <span className="size-2 rounded-full bg-emerald-500" />
                                <span>Submitted: {payload[1]?.value ?? 0}</span>
                              </div>
                            </div>
                          )
                        }
                        return null
                      }}
                    />
                    <Legend
                      verticalAlign="top"
                      height={36}
                      formatter={(value) => (
                        <span className="text-xs font-medium text-muted-foreground mr-3">
                          {value === "started" ? "Attempts Started" : "Attempts Submitted"}
                        </span>
                      )}
                    />
                    <Area
                      type="monotone"
                      dataKey="started"
                      stroke="#3b82f6"
                      strokeWidth={2}
                      fillOpacity={1}
                      fill="url(#startedGrad)"
                    />
                    <Area
                      type="monotone"
                      dataKey="submitted"
                      stroke="#10b981"
                      strokeWidth={2}
                      fillOpacity={1}
                      fill="url(#submittedGrad)"
                    />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Chart 2: Content Repository Balance */}
        <Card className="border-border/80">
          <CardHeader>
            <div className="flex items-center justify-between">
              <div>
                <CardTitle className="text-base font-semibold flex items-center gap-2">
                  <Layers className="size-4 text-primary" />
                  Content Publication Balance
                </CardTitle>
                <CardDescription className="text-xs">
                  Inventory of published ready-to-test content versus drafts
                </CardDescription>
              </div>
              <Badge variant="outline" className="text-xs border-border">
                {(overview?.totalCodingProblems ?? 0) +
                  (overview?.totalMcqs ?? 0) +
                  (overview?.totalQuestionBanks ?? 0)}{" "}
                total items
              </Badge>
            </div>
          </CardHeader>
          <CardContent>
            {contentBalanceData.every((c) => c.total === 0) ? (
              <AnalyticsEmptyState
                title="No content items available"
                description="Create coding problems, MCQs, or question banks to view curriculum balance."
                className="py-12 border border-dashed border-border/70"
              />
            ) : (
              <div className="h-[280px] w-full">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={contentBalanceData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" className="stroke-border/50" vertical={false} />
                    <XAxis
                      dataKey="name"
                      stroke="#888888"
                      fontSize={11}
                      tickLine={false}
                      axisLine={false}
                    />
                    <YAxis
                      stroke="#888888"
                      fontSize={11}
                      tickLine={false}
                      axisLine={false}
                      allowDecimals={false}
                    />
                    <Tooltip
                      content={({ active, payload, label }) => {
                        if (active && payload && payload.length) {
                          return (
                            <div className="rounded-lg border border-border bg-background p-2.5 shadow-md text-xs">
                              <p className="font-semibold mb-1 text-foreground">{label}</p>
                              <div className="flex items-center gap-2 text-emerald-500">
                                <span className="size-2 rounded-full bg-emerald-500" />
                                <span>Published: {payload[0]?.value ?? 0}</span>
                              </div>
                              <div className="flex items-center gap-2 text-amber-500 mt-0.5">
                                <span className="size-2 rounded-full bg-amber-500" />
                                <span>Draft: {payload[1]?.value ?? 0}</span>
                              </div>
                            </div>
                          )
                        }
                        return null
                      }}
                    />
                    <Legend
                      verticalAlign="top"
                      height={36}
                      formatter={(value) => (
                        <span className="text-xs font-medium text-muted-foreground mr-3">
                          {value === "published" ? "Published (Live)" : "Draft"}
                        </span>
                      )}
                    />
                    <Bar dataKey="published" fill="#10b981" radius={[4, 4, 0, 0]} />
                    <Bar dataKey="draft" fill="#f59e0b" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Infrastructure & Live Telemetry Panel */}
      <Card className="border-border/80">
        <CardHeader className="pb-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Cpu className="size-4 text-indigo-500" />
              <CardTitle className="text-base font-semibold">
                Execution Engine & Infrastructure Health
              </CardTitle>
            </div>
            <Badge
              variant="outline"
              className="border-emerald-500/30 bg-emerald-500/10 text-emerald-500 text-xs"
            >
              System Online
            </Badge>
          </div>
          <CardDescription className="text-xs">
            Live execution worker concurrency, queue buffering capacity, and audit event volume
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-6 sm:grid-cols-3">
            {/* Worker Capacity */}
            <div className="rounded-lg border border-border/70 p-4 space-y-2 bg-muted/20">
              <div className="flex items-center justify-between text-xs">
                <span className="font-medium text-foreground">Worker Concurrency</span>
                <span className="font-semibold text-primary">
                  {operational?.activeExecutionWorkers ?? 0} / 4 Threads
                </span>
              </div>
              <Progress value={workerPercent} className="h-2" />
              <p className="text-[11px] text-muted-foreground">
                Dedicated Isolate & Docker sandbox judging threads
              </p>
            </div>

            {/* Waiting Queue */}
            <div className="rounded-lg border border-border/70 p-4 space-y-2 bg-muted/20">
              <div className="flex items-center justify-between text-xs">
                <span className="font-medium text-foreground">Queue Buffer Depth</span>
                <span className="font-semibold text-amber-500">
                  {operational?.currentQueueDepth ?? 0} / {operational?.maxQueueCapacity ?? 50}
                </span>
              </div>
              <Progress value={queuePercent} className="h-2" />
              <p className="text-[11px] text-muted-foreground">
                Submissions awaiting execution dispatch
              </p>
            </div>

            {/* Total Events Ingested */}
            <div className="rounded-lg border border-border/70 p-4 space-y-2 bg-muted/20">
              <div className="flex items-center justify-between text-xs">
                <span className="font-medium text-foreground">Analytics Ingestion</span>
                <Badge variant="outline" className="text-[10px] border-border">
                  Indexed
                </Badge>
              </div>
              <div className="text-xl font-bold tracking-tight text-foreground">
                {(operational?.totalEventsLogged ?? 0).toLocaleString()} Events
              </div>
              <p className="text-[11px] text-muted-foreground">
                Idempotent audit logs retained for 90 days
              </p>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
