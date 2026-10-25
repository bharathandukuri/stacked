import {
  Target,
  Award,
  TrendingUp,
  Code2,
  Layers,
  BarChart3,
  Loader2,
} from "lucide-react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import type { AssessmentMetricsResponse } from "@/types/assessment"

interface AssessmentMetricsTabProps {
  metricsData: AssessmentMetricsResponse | undefined
  loadingMetrics: boolean
}

export function AssessmentMetricsTab({
  metricsData,
  loadingMetrics,
}: AssessmentMetricsTabProps) {
  if (loadingMetrics) {
    return (
      <div className="flex items-center justify-center py-16 gap-2 text-muted-foreground">
        <Loader2 className="h-5 w-5 animate-spin text-primary" />
        <span>Aggregating assessment analytics & metrics...</span>
      </div>
    )
  }

  if (!metricsData) {
    return (
      <div className="text-center py-12 text-muted-foreground">
        <BarChart3 className="size-8 mx-auto mb-2 opacity-50" />
        <p className="text-sm">No metrics available yet.</p>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Performance KPIs */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="bg-card/60">
          <CardContent className="pt-6">
            <div className="flex items-center justify-between">
              <div className="space-y-1">
                <span className="text-xs text-muted-foreground font-medium">
                  Completion Rate
                </span>
                <div className="text-2xl font-bold font-mono">
                  {metricsData.completionRate.toFixed(1)}%
                </div>
                <span className="text-[11px] text-muted-foreground">
                  {metricsData.submittedCount} submitted / {metricsData.totalCandidatesAssigned} assigned
                </span>
              </div>
              <Target className="size-8 text-primary/40" />
            </div>
          </CardContent>
        </Card>

        <Card className="bg-card/60">
          <CardContent className="pt-6">
            <div className="flex items-center justify-between">
              <div className="space-y-1">
                <span className="text-xs text-muted-foreground font-medium">Pass Rate</span>
                <div className="text-2xl font-bold font-mono text-emerald-600 dark:text-emerald-400">
                  {metricsData.passRate.toFixed(1)}%
                </div>
                <span className="text-[11px] text-muted-foreground">
                  Pass threshold configured
                </span>
              </div>
              <Award className="size-8 text-emerald-500/40" />
            </div>
          </CardContent>
        </Card>

        <Card className="bg-card/60">
          <CardContent className="pt-6">
            <div className="flex items-center justify-between">
              <div className="space-y-1">
                <span className="text-xs text-muted-foreground font-medium">
                  Score Distribution
                </span>
                <div className="text-2xl font-bold font-mono">
                  {metricsData.averageScore.toFixed(1)}{" "}
                  <span className="text-xs font-normal text-muted-foreground">
                    / {metricsData.maxPossibleScore}
                  </span>
                </div>
                <span className="text-[11px] text-muted-foreground">
                  Median: {metricsData.medianScore.toFixed(1)} pts
                </span>
              </div>
              <TrendingUp className="size-8 text-sky-500/40" />
            </div>
          </CardContent>
        </Card>

        <Card className="bg-card/60">
          <CardContent className="pt-6">
            <div className="flex items-center justify-between">
              <div className="space-y-1">
                <span className="text-xs text-muted-foreground font-medium">
                  Coding Accuracy
                </span>
                <div className="text-2xl font-bold font-mono text-purple-600 dark:text-purple-400">
                  {metricsData.codingSuccessRate.toFixed(1)}%
                </div>
                <span className="text-[11px] text-muted-foreground">
                  {metricsData.totalCodeSubmissions} code submissions
                </span>
              </div>
              <Code2 className="size-8 text-purple-500/40" />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Secondary Metrics Strip */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <Card>
          <CardHeader className="pb-2">
            <CardDescription className="text-xs">Average Duration</CardDescription>
            <CardTitle className="text-xl font-bold font-mono">
              {metricsData.averageTimeSpentMinutes.toFixed(1)} minutes
            </CardTitle>
          </CardHeader>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardDescription className="text-xs">Candidate Pipeline</CardDescription>
            <CardTitle className="text-sm font-semibold flex items-center gap-2">
              <span className="text-amber-500">
                {metricsData.inProgressCount} in progress
              </span>
              <span>•</span>
              <span className="text-muted-foreground">
                {metricsData.invitedCount} invited
              </span>
            </CardTitle>
          </CardHeader>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardDescription className="text-xs">Total Integrity Flags</CardDescription>
            <CardTitle className="text-xl font-bold flex items-center gap-2">
              <span
                className={
                  metricsData.totalIntegrityFlags > 0
                    ? "text-amber-500 font-mono"
                    : "text-muted-foreground font-mono"
                }
              >
                {metricsData.totalIntegrityFlags}
              </span>
              <span className="text-xs font-normal text-muted-foreground">
                focus blur / reconnect events
              </span>
            </CardTitle>
          </CardHeader>
        </Card>
      </div>

      {/* Question-Wise Performance Breakdown Table */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Layers className="size-4 text-primary" />
            <span>Question-by-Question Difficulty & Success Rates</span>
          </CardTitle>
          <CardDescription>
            Detailed aggregate outcomes per question across all completed candidate attempts.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="rounded-md border overflow-hidden">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-12">#</TableHead>
                  <TableHead>Question Title</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead>Max Points</TableHead>
                  <TableHead>Answered</TableHead>
                  <TableHead>Correct</TableHead>
                  <TableHead>Average Score</TableHead>
                  <TableHead className="w-40 text-right">Success Rate</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {metricsData.questionMetrics?.map((qm) => (
                  <TableRow key={qm.questionId}>
                    <TableCell className="font-mono text-xs">{qm.index + 1}</TableCell>
                    <TableCell className="font-medium text-xs">{qm.title}</TableCell>
                    <TableCell>
                      <Badge variant="outline" className="text-[10px]">
                        {qm.questionType}
                      </Badge>
                    </TableCell>
                    <TableCell className="font-mono text-xs">{qm.maxPoints} pts</TableCell>
                    <TableCell className="text-xs">{qm.answeredCount}</TableCell>
                    <TableCell className="text-xs font-semibold text-emerald-600 dark:text-emerald-400">
                      {qm.correctCount}
                    </TableCell>
                    <TableCell className="font-mono text-xs">
                      {qm.averageScore.toFixed(1)} pts
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex items-center justify-end gap-2">
                        <div className="w-20 bg-muted rounded-full h-2 overflow-hidden">
                          <div
                            className="h-full bg-primary rounded-full transition-all"
                            style={{ width: `${Math.min(100, qm.successRate)}%` }}
                          />
                        </div>
                        <span className="font-mono text-xs font-bold w-12 text-right">
                          {qm.successRate.toFixed(1)}%
                        </span>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}

