import {
  GraduationCap,
  Users,
  CheckCircle2,
  Clock,
  Cpu,
} from "lucide-react"
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { Badge } from "@/components/ui/badge"
import type {
  OperationalAnalyticsResponse,
  PlatformOverviewResponse,
} from "@/types/analytics"

interface KpiMetricCardsProps {
  overview?: PlatformOverviewResponse
  operational?: OperationalAnalyticsResponse
  isLoading: boolean
}

export function KpiMetricCards({
  overview,
  operational,
  isLoading,
}: KpiMetricCardsProps) {
  if (isLoading || !overview) {
    return (
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
        {Array.from({ length: 5 }).map((_, i) => (
          <Card key={i} className="border-border/80">
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <Skeleton className="h-4 w-24" />
              <Skeleton className="size-4 rounded-full" />
            </CardHeader>
            <CardContent className="space-y-2">
              <Skeleton className="h-7 w-20" />
              <Skeleton className="h-3 w-32" />
            </CardContent>
          </Card>
        ))}
      </div>
    )
  }

  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
      {/* 1. Active Assessments */}
      <Card className="border-border/80 hover:border-primary/40 transition-colors">
        <CardHeader className="flex flex-row items-center justify-between pb-2">
          <CardTitle className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
            Active Assessments
          </CardTitle>
          <div className="rounded-md bg-primary/10 p-1.5 text-primary">
            <GraduationCap className="size-4" />
          </div>
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-bold tracking-tight text-foreground">
            {overview.activeAssessments}
          </div>
          <p className="text-[11px] text-muted-foreground mt-1 truncate">
            {overview.completedAssessments} completed • {overview.scheduledAssessments} scheduled
          </p>
        </CardContent>
      </Card>

      {/* 2. Total Candidates */}
      <Card className="border-border/80 hover:border-blue-500/40 transition-colors">
        <CardHeader className="flex flex-row items-center justify-between pb-2">
          <CardTitle className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
            Total Candidates
          </CardTitle>
          <div className="rounded-md bg-blue-500/10 p-1.5 text-blue-500">
            <Users className="size-4" />
          </div>
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-bold tracking-tight text-foreground">
            {overview.totalCandidates.toLocaleString()}
          </div>
          <p className="text-[11px] text-muted-foreground mt-1 truncate">
            {overview.totalEnrollments.toLocaleString()} total enrollments
          </p>
        </CardContent>
      </Card>

      {/* 3. Overall Completion Rate */}
      <Card className="border-border/80 hover:border-emerald-500/40 transition-colors">
        <CardHeader className="flex flex-row items-center justify-between pb-2">
          <CardTitle className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
            Completion Rate
          </CardTitle>
          <div className="rounded-md bg-emerald-500/10 p-1.5 text-emerald-500">
            <CheckCircle2 className="size-4" />
          </div>
        </CardHeader>
        <CardContent>
          <div className="flex items-baseline gap-2">
            <span className="text-2xl font-bold tracking-tight text-foreground">
              {overview.overallCompletionRate}%
            </span>
          </div>
          <p className="text-[11px] text-muted-foreground mt-1 truncate">
            {overview.totalAttemptsSubmitted.toLocaleString()} of {overview.totalAttemptsStarted.toLocaleString()} started
          </p>
        </CardContent>
      </Card>

      {/* 4. Avg Attempt Duration */}
      <Card className="border-border/80 hover:border-amber-500/40 transition-colors">
        <CardHeader className="flex flex-row items-center justify-between pb-2">
          <CardTitle className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
            Avg Duration
          </CardTitle>
          <div className="rounded-md bg-amber-500/10 p-1.5 text-amber-500">
            <Clock className="size-4" />
          </div>
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-bold tracking-tight text-foreground">
            {overview.avgAttemptDurationMinutes > 0
              ? `${overview.avgAttemptDurationMinutes}m`
              : "—"}
          </div>
          <p className="text-[11px] text-muted-foreground mt-1 truncate">
            Finalized attempt time spent
          </p>
        </CardContent>
      </Card>

      {/* 5. Live Execution Infrastructure */}
      <Card className="border-border/80 hover:border-indigo-500/40 transition-colors">
        <CardHeader className="flex flex-row items-center justify-between pb-2">
          <CardTitle className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
            Execution Queue
          </CardTitle>
          <div className="rounded-md bg-indigo-500/10 p-1.5 text-indigo-500">
            <Cpu className="size-4" />
          </div>
        </CardHeader>
        <CardContent>
          <div className="flex items-center gap-2">
            <span className="text-2xl font-bold tracking-tight text-foreground">
              {operational?.currentQueueDepth ?? 0}
            </span>
            <span className="text-xs text-muted-foreground">
              / {operational?.maxQueueCapacity ?? 50} depth
            </span>
            <Badge
              variant="outline"
              className="ml-auto text-[10px] border-emerald-500/30 text-emerald-500"
            >
              {operational?.systemStatus ?? "HEALTHY"}
            </Badge>
          </div>
          <p className="text-[11px] text-muted-foreground mt-1 truncate">
            {operational?.activeExecutionWorkers ?? 0} active workers • {operational?.totalEventsLogged ?? 0} events
          </p>
        </CardContent>
      </Card>
    </div>
  )
}
