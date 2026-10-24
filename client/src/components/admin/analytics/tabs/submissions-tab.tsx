import { useMemo } from "react"
import {
  Code2,
  PieChart as PieIcon,
  CheckCircle2,
  Timer,
  Terminal,
} from "lucide-react"
import {
  PieChart,
  Pie,
  Cell,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { Badge } from "@/components/ui/badge"
import { AnalyticsEmptyState } from "../analytics-empty-state"
import type { SubmissionAnalyticsResponse } from "@/types/analytics"

interface SubmissionsTabProps {
  submissions?: SubmissionAnalyticsResponse
  isLoading: boolean
}

const VERDICT_COLORS: Record<string, string> = {
  ACCEPTED: "#10b981", // Emerald
  WRONG_ANSWER: "#f43f5e", // Rose
  TIME_LIMIT_EXCEEDED: "#f59e0b", // Amber
  COMPILATION_ERROR: "#8b5cf6", // Violet
  RUNTIME_ERROR: "#3b82f6", // Blue
  MEMORY_LIMIT_EXCEEDED: "#06b6d4", // Cyan
  INTERNAL_ERROR: "#ef4444", // Red
}
const DEFAULT_COLOR = "#64748b"

export function SubmissionsTab({
  submissions,
  isLoading,
}: SubmissionsTabProps) {
  // Format verdict distribution for Donut / Pie chart
  const verdictChartData = useMemo(() => {
    if (!submissions?.verdictDistribution) return []
    const total = Object.values(submissions.verdictDistribution).reduce((a, b) => a + b, 0)
    if (total === 0) return []

    return Object.entries(submissions.verdictDistribution)
      .map(([verdict, count]) => {
        const percentage = Math.round((count / total) * 1000) / 10
        return {
          name: verdict.replace(/_/g, " "),
          rawKey: verdict,
          value: count,
          percentage,
          color: VERDICT_COLORS[verdict] || DEFAULT_COLOR,
        }
      })
      .sort((a, b) => b.value - a.value)
  }, [submissions?.verdictDistribution])

  // Format language distribution for Bar chart
  const languageChartData = useMemo(() => {
    if (!submissions?.languageDistribution) return []
    return Object.entries(submissions.languageDistribution)
      .map(([lang, count]) => ({
        language: lang.toUpperCase(),
        submissions: count,
      }))
      .sort((a, b) => b.submissions - a.submissions)
  }, [submissions?.languageDistribution])

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="grid gap-4 sm:grid-cols-3">
          <Skeleton className="h-24 rounded-xl" />
          <Skeleton className="h-24 rounded-xl" />
          <Skeleton className="h-24 rounded-xl" />
        </div>
        <div className="grid gap-6 md:grid-cols-2">
          <Skeleton className="h-[320px] rounded-xl" />
          <Skeleton className="h-[320px] rounded-xl" />
        </div>
      </div>
    )
  }

  if (!submissions) {
    return <AnalyticsEmptyState />
  }

  return (
    <div className="space-y-6">
      {/* KPI Cards: Pass Rate, Latency, Interactive Runs */}
      <div className="grid gap-4 sm:grid-cols-3">
        {/* Pass Rate */}
        <Card className="border-border/80">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
              Acceptance Rate
            </CardTitle>
            <div className="rounded-md bg-emerald-500/10 p-1.5 text-emerald-500">
              <CheckCircle2 className="size-4" />
            </div>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold tracking-tight text-emerald-600 dark:text-emerald-400">
              {submissions.passRate}%
            </div>
            <p className="text-[11px] text-muted-foreground mt-1">
              {submissions.acceptedSubmissions} accepted of {submissions.totalSubmissions} evaluated
            </p>
          </CardContent>
        </Card>

        {/* Avg Execution Latency */}
        <Card className="border-border/80">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
              Avg Execution Latency
            </CardTitle>
            <div className="rounded-md bg-amber-500/10 p-1.5 text-amber-500">
              <Timer className="size-4" />
            </div>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold tracking-tight text-foreground">
              {submissions.avgExecutionTimeMs > 0
                ? `${submissions.avgExecutionTimeMs} ms`
                : "—"}
            </div>
            <p className="text-[11px] text-muted-foreground mt-1">
              Average sandbox execution duration
            </p>
          </CardContent>
        </Card>

        {/* Interactive Testing Volume */}
        <Card className="border-border/80">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
              Interactive Test Runs
            </CardTitle>
            <div className="rounded-md bg-indigo-500/10 p-1.5 text-indigo-500">
              <Terminal className="size-4" />
            </div>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold tracking-tight text-foreground">
              {submissions.totalInteractiveRuns.toLocaleString()}
            </div>
            <p className="text-[11px] text-muted-foreground mt-1">
              Candidate sample & custom input runs
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Grid: Verdict Donut Chart & Language Usage */}
      <div className="grid gap-6 lg:grid-cols-2">
        {/* Verdict Distribution Donut */}
        <Card className="border-border/80">
          <CardHeader>
            <div className="flex items-center justify-between">
              <div>
                <CardTitle className="text-base font-semibold flex items-center gap-2">
                  <PieIcon className="size-4 text-primary" />
                  Submission Verdict Distribution
                </CardTitle>
                <CardDescription className="text-xs">
                  Proportion of pass and fail verdicts across evaluated submissions
                </CardDescription>
              </div>
              <Badge variant="outline" className="border-border text-xs">
                {submissions.totalSubmissions} Total
              </Badge>
            </div>
          </CardHeader>
          <CardContent>
            {verdictChartData.length === 0 ? (
              <AnalyticsEmptyState
                title="No submission verdicts"
                description="Verdicts will plot here once candidates submit code solutions to assessment problems."
                className="py-12 border border-dashed border-border/70"
              />
            ) : (
              <div className="flex flex-col sm:flex-row items-center justify-center gap-6">
                <div className="h-[250px] w-[250px] shrink-0">
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={verdictChartData}
                        dataKey="value"
                        nameKey="name"
                        cx="50%"
                        cy="50%"
                        innerRadius={65}
                        outerRadius={95}
                        paddingAngle={3}
                      >
                        {verdictChartData.map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={entry.color} />
                        ))}
                      </Pie>
                      <Tooltip
                        content={({ active, payload }) => {
                          if (active && payload && payload.length) {
                            const data = payload[0].payload
                            return (
                              <div className="rounded-lg border border-border bg-background p-2.5 shadow-md text-xs">
                                <p className="font-semibold text-foreground">{data.name}</p>
                                <p className="text-muted-foreground mt-0.5">
                                  {data.value} submissions ({data.percentage}%)
                                </p>
                              </div>
                            )
                          }
                          return null
                        }}
                      />
                    </PieChart>
                  </ResponsiveContainer>
                </div>

                {/* Verdict Legends List */}
                <div className="flex flex-col gap-2 w-full max-w-xs text-xs">
                  {verdictChartData.map((item) => (
                    <div
                      key={item.rawKey}
                      className="flex items-center justify-between rounded-md border border-border/50 px-2.5 py-1.5"
                    >
                      <div className="flex items-center gap-2">
                        <span
                          className="size-2.5 rounded-full shrink-0"
                          style={{ backgroundColor: item.color }}
                        />
                        <span className="font-medium text-foreground">{item.name}</span>
                      </div>
                      <span className="text-muted-foreground font-mono">
                        {item.value} ({item.percentage}%)
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Language Breakdown */}
        <Card className="border-border/80">
          <CardHeader>
            <div className="flex items-center justify-between">
              <div>
                <CardTitle className="text-base font-semibold flex items-center gap-2">
                  <Code2 className="size-4 text-primary" />
                  Programming Language Adoption
                </CardTitle>
                <CardDescription className="text-xs">
                  Total submission volume broken down by compiler / runtime environment
                </CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            {languageChartData.length === 0 ? (
              <AnalyticsEmptyState
                title="No language data"
                description="Language distributions will appear when candidate code executions occur."
                className="py-12 border border-dashed border-border/70"
              />
            ) : (
              <div className="h-[280px] w-full">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart
                    data={languageChartData}
                    layout="vertical"
                    margin={{ top: 10, right: 20, left: 20, bottom: 0 }}
                  >
                    <CartesianGrid strokeDasharray="3 3" className="stroke-border/50" horizontal={false} />
                    <XAxis
                      type="number"
                      stroke="#888888"
                      fontSize={11}
                      tickLine={false}
                      axisLine={false}
                      allowDecimals={false}
                    />
                    <YAxis
                      dataKey="language"
                      type="category"
                      stroke="#888888"
                      fontSize={11}
                      tickLine={false}
                      axisLine={false}
                      width={80}
                    />
                    <Tooltip
                      content={({ active, payload, label }) => {
                        if (active && payload && payload.length) {
                          return (
                            <div className="rounded-lg border border-border bg-background p-2.5 shadow-md text-xs">
                              <p className="font-semibold text-foreground">{label}</p>
                              <p className="text-primary mt-0.5">
                                {payload[0]?.value ?? 0} Submissions
                              </p>
                            </div>
                          )
                        }
                        return null
                      }}
                    />
                    <Bar
                      dataKey="submissions"
                      fill="#6366f1"
                      radius={[0, 4, 4, 0]}
                    />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
