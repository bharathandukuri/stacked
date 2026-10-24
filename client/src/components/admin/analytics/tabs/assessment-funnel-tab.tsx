import { useMemo } from "react"
import {
  Users,
  PlayCircle,
  CheckCircle2,
  AlertTriangle,
  Clock,
  SlidersHorizontal,
} from "lucide-react"
import {
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
import type { AssessmentAnalyticsResponse } from "@/types/analytics"

interface AssessmentFunnelTabProps {
  assessments?: AssessmentAnalyticsResponse
  isLoading: boolean
}

export function AssessmentFunnelTab({
  assessments,
  isLoading,
}: AssessmentFunnelTabProps) {
  // Mode breakdown comparison data (manual vs automatic)
  const modeData = useMemo(() => {
    if (!assessments) return []
    return [
      {
        action: "Activations",
        Manual: assessments.manualActivations,
        Automatic: assessments.autoActivations,
        total: assessments.manualActivations + assessments.autoActivations,
      },
      {
        action: "Completions",
        Manual: assessments.manualCompletions,
        Automatic: assessments.autoCompletions,
        total: assessments.manualCompletions + assessments.autoCompletions,
      },
    ]
  }, [assessments])

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-[220px] rounded-xl" />
        <div className="grid gap-6 md:grid-cols-2">
          <Skeleton className="h-[300px] rounded-xl" />
          <Skeleton className="h-[300px] rounded-xl" />
        </div>
      </div>
    )
  }

  if (!assessments) {
    return <AnalyticsEmptyState />
  }

  const invited = assessments.candidatesInvited
  const started = assessments.attemptsStarted
  const submitted = assessments.attemptsSubmitted
  const expired = assessments.attemptsExpired

  const startedRatio = invited > 0 ? Math.min(100, Math.round((started / invited) * 100)) : 0
  const submittedRatio = started > 0 ? Math.min(100, Math.round((submitted / started) * 100)) : 0
  const expiredRatio = started > 0 ? Math.min(100, Math.round((expired / started) * 100)) : 0

  return (
    <div className="space-y-6">
      {/* Funnel Visual Progression Cards */}
      <Card className="border-border/80">
        <CardHeader className="pb-3">
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="text-base font-semibold flex items-center gap-2">
                <Users className="size-4 text-primary" />
                Candidate Assessment Participation Funnel
              </CardTitle>
              <CardDescription className="text-xs">
                End-to-end conversion from candidate invitation to final submission
              </CardDescription>
            </div>
            <div className="flex gap-2">
              <Badge variant="outline" className="border-border text-xs">
                {assessments.participationRate}% Participation
              </Badge>
              <Badge variant="outline" className="border-emerald-500/30 text-emerald-500 text-xs">
                {assessments.completionRate}% Completion
              </Badge>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {invited === 0 && started === 0 ? (
            <AnalyticsEmptyState
              title="No assessment attempts in range"
              description="Funnel conversions will visualize here once candidates are enrolled and start attempts."
              className="py-12 border border-dashed border-border/70"
            />
          ) : (
            <div className="grid gap-4 sm:grid-cols-4">
              {/* Stage 1: Enrolled */}
              <div className="rounded-xl border border-border bg-muted/20 p-4 space-y-2 relative overflow-hidden">
                <div className="flex items-center justify-between text-xs text-muted-foreground">
                  <span className="font-semibold uppercase tracking-wider text-[10px]">Stage 1</span>
                  <Users className="size-3.5 text-blue-500" />
                </div>
                <div className="text-2xl font-bold tracking-tight text-foreground">
                  {invited.toLocaleString()}
                </div>
                <p className="text-xs font-medium text-foreground">Candidates Invited</p>
                <div className="text-[11px] text-muted-foreground">100% of candidate pool</div>
                <Progress value={100} className="h-1.5" />
              </div>

              {/* Stage 2: Started */}
              <div className="rounded-xl border border-border bg-muted/20 p-4 space-y-2 relative overflow-hidden">
                <div className="flex items-center justify-between text-xs text-muted-foreground">
                  <span className="font-semibold uppercase tracking-wider text-[10px]">Stage 2</span>
                  <PlayCircle className="size-3.5 text-indigo-500" />
                </div>
                <div className="text-2xl font-bold tracking-tight text-foreground">
                  {started.toLocaleString()}
                </div>
                <p className="text-xs font-medium text-foreground">Attempts Started</p>
                <div className="text-[11px] text-muted-foreground">
                  {startedRatio}% start conversion
                </div>
                <Progress value={startedRatio} className="h-1.5" />
              </div>

              {/* Stage 3: Submitted */}
              <div className="rounded-xl border border-border bg-emerald-500/5 border-emerald-500/30 p-4 space-y-2 relative overflow-hidden">
                <div className="flex items-center justify-between text-xs text-emerald-600 dark:text-emerald-400">
                  <span className="font-semibold uppercase tracking-wider text-[10px]">Stage 3</span>
                  <CheckCircle2 className="size-3.5 text-emerald-500" />
                </div>
                <div className="text-2xl font-bold tracking-tight text-emerald-600 dark:text-emerald-400">
                  {submitted.toLocaleString()}
                </div>
                <p className="text-xs font-medium text-foreground">Final Submissions</p>
                <div className="text-[11px] text-muted-foreground">
                  {submittedRatio}% of started attempts
                </div>
                <Progress value={submittedRatio} className="h-1.5" />
              </div>

              {/* Stage 4: Expired */}
              <div className="rounded-xl border border-border bg-muted/20 p-4 space-y-2 relative overflow-hidden">
                <div className="flex items-center justify-between text-xs text-muted-foreground">
                  <span className="font-semibold uppercase tracking-wider text-[10px]">Incomplete</span>
                  <AlertTriangle className="size-3.5 text-amber-500" />
                </div>
                <div className="text-2xl font-bold tracking-tight text-amber-500">
                  {expired.toLocaleString()}
                </div>
                <p className="text-xs font-medium text-foreground">Expired Unfinalized</p>
                <div className="text-[11px] text-muted-foreground">
                  {expiredRatio}% timeout drop-off
                </div>
                <Progress value={expiredRatio} className="h-1.5" />
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Grid: Lifecycle Management Modes & Duration */}
      <div className="grid gap-6 lg:grid-cols-2">
        {/* Chart: Manual Admin Overrides vs Automatic System Schedules */}
        <Card className="border-border/80">
          <CardHeader>
            <div className="flex items-center justify-between">
              <div>
                <CardTitle className="text-base font-semibold flex items-center gap-2">
                  <SlidersHorizontal className="size-4 text-primary" />
                  Lifecycle Control: Manual vs Automated
                </CardTitle>
                <CardDescription className="text-xs">
                  Frequency of manual administrator actions versus scheduled automated execution
                </CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            {modeData.every((m) => m.total === 0) ? (
              <AnalyticsEmptyState
                title="No lifecycle transitions recorded"
                description="Activations and completions triggered manually or automatically will be tracked here."
                className="py-12 border border-dashed border-border/70"
              />
            ) : (
              <div className="h-[260px] w-full">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={modeData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" className="stroke-border/50" vertical={false} />
                    <XAxis
                      dataKey="action"
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
                              <div className="flex items-center gap-2 text-primary">
                                <span className="size-2 rounded-full bg-primary" />
                                <span>Manual Override: {payload[0]?.value ?? 0}</span>
                              </div>
                              <div className="flex items-center gap-2 text-muted-foreground mt-0.5">
                                <span className="size-2 rounded-full bg-muted-foreground" />
                                <span>Scheduled Auto: {payload[1]?.value ?? 0}</span>
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
                          {value === "Manual" ? "Manual Admin Action" : "Automatic Schedule"}
                        </span>
                      )}
                    />
                    <Bar dataKey="Manual" fill="#8b5cf6" radius={[4, 4, 0, 0]} />
                    <Bar dataKey="Automatic" fill="#64748b" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Efficiency & Duration Stats */}
        <Card className="border-border/80">
          <CardHeader>
            <div className="flex items-center justify-between">
              <div>
                <CardTitle className="text-base font-semibold flex items-center gap-2">
                  <Clock className="size-4 text-amber-500" />
                  Assessment Duration & Velocity
                </CardTitle>
                <CardDescription className="text-xs">
                  Pacing and execution duration metrics for completed attempts
                </CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="rounded-lg border border-border/70 p-4 bg-muted/20 flex items-center justify-between">
              <div>
                <p className="text-xs text-muted-foreground">Average Completion Time</p>
                <p className="text-2xl font-bold tracking-tight text-foreground mt-0.5">
                  {assessments.averageAttemptDurationMinutes > 0
                    ? `${assessments.averageAttemptDurationMinutes} minutes`
                    : "—"}
                </p>
                <p className="text-[11px] text-muted-foreground mt-1">
                  Average time candidates spend inside the exam room before submission
                </p>
              </div>
              <div className="rounded-full bg-amber-500/10 p-3 text-amber-500">
                <Clock className="size-6" />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="rounded-lg border border-border/70 p-3 bg-muted/10">
                <span className="text-[11px] text-muted-foreground">Created in Window</span>
                <p className="text-lg font-bold text-foreground mt-0.5">
                  {assessments.assessmentsCreated}
                </p>
              </div>
              <div className="rounded-lg border border-border/70 p-3 bg-muted/10">
                <span className="text-[11px] text-muted-foreground">Finalized in Window</span>
                <p className="text-lg font-bold text-foreground mt-0.5">
                  {assessments.assessmentsCompleted}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
