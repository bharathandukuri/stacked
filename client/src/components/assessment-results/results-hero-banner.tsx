import { BarChart3, Clock, Award, ShieldCheck } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { cn } from "@/lib/utils"
import type { CandidateAssessmentResultResponse } from "@/types/assessment"

interface ResultsHeroBannerProps {
  results: CandidateAssessmentResultResponse
  answeredCount?: number
  unansweredCount?: number
}

export function ResultsHeroBanner({
  results,
  answeredCount,
  unansweredCount,
}: ResultsHeroBannerProps) {
  const formatTimeSpent = (totalSec?: number) => {
    if (totalSec === undefined || totalSec === null) return "--"
    const hours = Math.floor(totalSec / 3600)
    const minutes = Math.floor((totalSec % 3600) / 60)
    const seconds = totalSec % 60

    if (hours > 0) {
      return `${hours}h ${minutes}m ${seconds}s`
    }
    return `${minutes}m ${seconds}s`
  }

  const hasPassingScore =
    results.passingScore !== undefined && results.passingScore !== null
  const isPassed = results.passed === true

  return (
    <div className="rounded-2xl border border-border bg-card/60 p-6 sm:p-8 shadow-sm space-y-6">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 mb-1.5">
            <Badge
              className={cn(
                "text-xs font-semibold px-2.5 py-0.5",
                hasPassingScore && results.passed != null
                  ? isPassed
                    ? "bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border-emerald-500/30"
                    : "bg-destructive/15 text-destructive border-destructive/30"
                  : "bg-primary/10 text-primary border-primary/20"
              )}
            >
              {hasPassingScore && results.passed != null
                ? isPassed
                  ? "PASSED"
                  : "DID NOT PASS"
                : "COMPLETED"}
            </Badge>
            <span className="text-xs text-muted-foreground">
              Attempt #{results.attemptNumber}
            </span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-foreground">
            Assessment Results
          </h1>
          {results.description && (
            <p className="text-xs sm:text-sm text-muted-foreground mt-1 max-w-2xl leading-relaxed">
              {results.description}
            </p>
          )}
        </div>

        {/* Big Score Box */}
        <div className="flex flex-col items-start sm:items-end p-4 rounded-xl border border-border bg-background/50 min-w-[140px]">
          <span className="text-[11px] font-semibold text-muted-foreground uppercase tracking-wider">
            Final Score
          </span>
          <div className="flex items-baseline gap-1 mt-0.5">
            <span className="text-3xl font-black font-mono text-foreground">
              {results.score}
            </span>
            <span className="text-sm font-semibold text-muted-foreground font-mono">
              / {results.maxPossibleScore}
            </span>
          </div>
          <span className="text-xs font-mono font-semibold text-primary mt-0.5">
            {results.percentage.toFixed(1)}% Score
          </span>
        </div>
      </div>

      {/* 4 Metric Summary Strip */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3 pt-2">
        <div className="p-4 rounded-xl border bg-card/40 space-y-1">
          <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <BarChart3 className="size-4 text-primary" />
            <span>Accuracy</span>
          </div>
          <div className="text-xl font-bold font-mono text-foreground">
            {results.accuracyPercentage.toFixed(1)}%
          </div>
          <div className="text-[11px] text-muted-foreground">
            On attempted questions
          </div>
        </div>

        <div className="p-4 rounded-xl border bg-card/40 space-y-1">
          <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <Clock className="size-4 text-amber-500" />
            <span>Time Spent</span>
          </div>
          <div className="text-xl font-bold font-mono text-foreground">
            {formatTimeSpent(results.timeSpentSeconds)}
          </div>
          <div className="text-[11px] text-muted-foreground">
            Total duration
          </div>
        </div>

        <div className="p-4 rounded-xl border bg-card/40 space-y-1">
          <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <Award className="size-4 text-emerald-500" />
            <span>Questions Answered</span>
          </div>
          <div className="text-xl font-bold text-foreground">
            {answeredCount ?? results.answeredCount}{" "}
            <span className="text-xs font-normal text-muted-foreground">
              / {results.totalQuestions}
            </span>
          </div>
          <div className="text-[11px] text-emerald-600 dark:text-emerald-400">
            {unansweredCount ?? results.unansweredCount} unanswered
          </div>
        </div>

        <div className="p-4 rounded-xl border bg-card/40 space-y-1">
          <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <ShieldCheck className="size-4 text-purple-500" />
            <span>Question Mix</span>
          </div>
          <div className="text-xl font-bold text-foreground">
            {results.codingCount}{" "}
            <span className="text-xs font-normal text-muted-foreground">Code</span> +{" "}
            {results.mcqCount}{" "}
            <span className="text-xs font-normal text-muted-foreground">MCQ</span>
          </div>
          <div className="text-[11px] text-muted-foreground">
            {results.markedForReviewCount} flagged during test
          </div>
        </div>
      </div>
    </div>
  )
}
