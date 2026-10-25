import { useState, useMemo } from "react"
import axios from "axios"
import { createFileRoute, Link } from "@tanstack/react-router"
import {
  CheckCircle2,
  XCircle,
  Code2,
  CheckSquare,
  ChevronDown,
  ChevronUp,
  ArrowLeft,
  Lock,
} from "lucide-react"

import { Logo } from "@/components/logo"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Card } from "@/components/ui/card"
import { PageLoadingState } from "@/components/common/page-loading-state"
import { PageErrorState } from "@/components/common/page-error-state"
import { useCandidateResults } from "@/hooks/use-assessments"
import { ResultsHeroBanner } from "@/components/assessment-results/results-hero-banner"
import { ResultsMcqItem } from "@/components/assessment-results/results-mcq-item"
import { ResultsCodingItem } from "@/components/assessment-results/results-coding-item"
import { cn } from "@/lib/utils"
import type { QuestionResultSummary } from "@/types/assessment"

export const Route = createFileRoute("/assessments/$assessmentId/results")({
  component: AssessmentResultsPage,
})

type FilterType = "ALL" | "CORRECT" | "INCORRECT" | "UNANSWERED"

function isQCorrect(q: QuestionResultSummary): boolean {
  return (
    q.isCorrect === true ||
    (q as unknown as { correct?: boolean }).correct === true ||
    (q.pointsAwarded > 0 && q.pointsAwarded === q.maxPoints)
  )
}

function isQAnswered(q: QuestionResultSummary): boolean {
  if (q.isAnswered === true || (q as unknown as { answered?: boolean }).answered === true) {
    return true
  }
  if (isQCorrect(q)) return true
  if (q.pointsAwarded > 0) return true
  if (q.questionType === "MCQ") {
    return Boolean(q.selectedOptionIds && q.selectedOptionIds.length > 0)
  }
  if (q.submissions && q.submissions.length > 0) return true
  if (q.lastExecutionResult != null) return true
  if (q.bestPassedTestcases !== undefined && q.bestPassedTestcases !== null && q.bestPassedTestcases > 0) return true
  if (q.code && q.code.trim().length > 0 && !q.code.includes("// No code was submitted")) return true
  return false
}

export function AssessmentResultsPage() {
  const { assessmentId } = Route.useParams()
  const { data: results, isLoading, isError, error, refetch } = useCandidateResults(assessmentId)

  const [activeFilter, setActiveFilter] = useState<FilterType>("ALL")
  const [expandedQuestions, setExpandedQuestions] = useState<Record<string, boolean>>({})

  const toggleQuestion = (questionId: string) => {
    setExpandedQuestions((prev) => ({
      ...prev,
      [questionId]: !prev[questionId],
    }))
  }

  const questionList = useMemo(
    () => results?.questionResults ?? [],
    [results?.questionResults]
  )

  const correctCount = useMemo(() => {
    return questionList.filter((q) => isQCorrect(q)).length
  }, [questionList])

  const incorrectCount = useMemo(() => {
    return questionList.filter((q) => isQAnswered(q) && !isQCorrect(q)).length
  }, [questionList])

  const unansweredCount = useMemo(() => {
    return questionList.filter((q) => !isQAnswered(q)).length
  }, [questionList])

  const filteredQuestions = useMemo(() => {
    return questionList.filter((q) => {
      if (activeFilter === "CORRECT") return isQCorrect(q)
      if (activeFilter === "INCORRECT") return isQAnswered(q) && !isQCorrect(q)
      if (activeFilter === "UNANSWERED") return !isQAnswered(q)
      return true
    })
  }, [questionList, activeFilter])

  if (isLoading) {
    return <PageLoadingState message="Computing evaluation results..." />
  }

  if (isError || !results) {
    return (
      <PageErrorState
        title="Unable to Load Assessment Results"
        message={
          axios.isAxiosError(error)
            ? (error.response?.data as { message?: string } | undefined)?.message ||
              "Could not retrieve your submission results. Please try again."
            : error instanceof Error
              ? error.message
              : "Could not retrieve your submission results. Please try again."
        }
        onRetry={() => refetch()}
        returnTo="/"
        returnLabel="Back to Dashboard"
      />
    )
  }

  // If results are confidential
  if (!results.resultsVisible) {
    return (
      <div className="min-h-screen bg-background text-foreground flex flex-col font-sans">
        <header className="h-14 border-b border-border bg-card/60 backdrop-blur-md px-6 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Logo size={24} />
            <span className="text-sm font-semibold truncate max-w-sm">
              {results.title}
            </span>
          </div>
          <Link to="/">
            <Button variant="outline" size="sm" className="gap-1.5 text-xs">
              <ArrowLeft className="size-3.5" />
              <span>Back to Dashboard</span>
            </Button>
          </Link>
        </header>

        <main className="flex-1 flex items-center justify-center p-6">
          <Card className="max-w-xl w-full text-center p-8 space-y-6 shadow-sm border-border">
            <div className="size-16 rounded-full bg-primary/10 border border-primary/20 text-primary flex items-center justify-center mx-auto">
              <Lock className="size-8" />
            </div>

            <div className="space-y-2">
              <h2 className="text-xl font-bold text-foreground">
                Submission Successfully Recorded
              </h2>
              <p className="text-xs text-muted-foreground leading-relaxed max-w-md mx-auto">
                Your assessment has been securely finalized and submitted for evaluation. Detailed scores and question breakdowns are confidential and will be shared by your administrator.
              </p>
            </div>

            <div className="pt-2">
              <Link to="/">
                <Button size="sm" className="text-xs">
                  Return to Dashboard
                </Button>
              </Link>
            </div>
          </Card>
        </main>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-background text-foreground flex flex-col font-sans">
      {/* Top Navigation */}
      <header className="h-14 border-b border-border bg-card/60 backdrop-blur-md px-6 flex items-center justify-between sticky top-0 z-20">
        <div className="flex items-center gap-3">
          <Link to="/" className="flex items-center gap-2">
            <Logo size={24} />
            <span className="text-sm font-bold tracking-tight text-foreground hidden sm:inline">
              Stacked
            </span>
          </Link>
          <span className="text-muted-foreground/40 text-sm hidden sm:inline">/</span>
          <span className="text-xs sm:text-sm font-semibold truncate max-w-xs sm:max-w-md text-foreground">
            {results.title}
          </span>
          <Badge variant="outline" className="text-[10px] hidden md:inline-flex">
            Results Report
          </Badge>
        </div>

        <Link to="/">
          <Button variant="outline" size="sm" className="h-8 gap-1.5 text-xs">
            <ArrowLeft className="size-3.5" />
            <span className="hidden sm:inline">Dashboard</span>
          </Button>
        </Link>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 max-w-6xl w-full mx-auto p-4 sm:p-6 md:p-8 space-y-8">
        {/* Hero Score Banner with 4 KPI cards */}
        <ResultsHeroBanner
          results={results}
          answeredCount={correctCount + incorrectCount}
          unansweredCount={unansweredCount}
        />

        {/* Question-Wise Performance Breakdown */}
        <div className="space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div>
              <h3 className="text-lg font-bold text-foreground tracking-tight">
                Question Breakdown
              </h3>
              <p className="text-xs text-muted-foreground">
                Review your answers, code submissions, and execution test results
              </p>
            </div>

            {/* Filter Tabs */}
            <div className="flex items-center gap-1 p-1 rounded-lg border bg-muted/30 text-xs">
              <button
                type="button"
                onClick={() => setActiveFilter("ALL")}
                className={cn(
                  "px-3 py-1 rounded-md font-medium transition-colors cursor-pointer",
                  activeFilter === "ALL"
                    ? "bg-background text-foreground shadow-xs font-semibold"
                    : "text-muted-foreground hover:text-foreground"
                )}
              >
                All ({results.totalQuestions})
              </button>
              <button
                type="button"
                onClick={() => setActiveFilter("CORRECT")}
                className={cn(
                  "px-3 py-1 rounded-md font-medium transition-colors cursor-pointer",
                  activeFilter === "CORRECT"
                    ? "bg-background text-emerald-600 dark:text-emerald-400 shadow-xs font-semibold"
                    : "text-muted-foreground hover:text-foreground"
                )}
              >
                Correct ({correctCount})
              </button>
              <button
                type="button"
                onClick={() => setActiveFilter("INCORRECT")}
                className={cn(
                  "px-3 py-1 rounded-md font-medium transition-colors cursor-pointer",
                  activeFilter === "INCORRECT"
                    ? "bg-background text-destructive shadow-xs font-semibold"
                    : "text-muted-foreground hover:text-foreground"
                )}
              >
                Incorrect ({incorrectCount})
              </button>
              <button
                type="button"
                onClick={() => setActiveFilter("UNANSWERED")}
                className={cn(
                  "px-3 py-1 rounded-md font-medium transition-colors cursor-pointer",
                  activeFilter === "UNANSWERED"
                    ? "bg-background text-amber-500 shadow-xs font-semibold"
                    : "text-muted-foreground hover:text-foreground"
                )}
              >
                Unanswered ({unansweredCount})
              </button>
            </div>
          </div>

          {/* Questions List */}
          <div className="space-y-3">
            {filteredQuestions.map((q) => {
              const isExpanded = expandedQuestions[q.questionId] ?? false
              const isCorrect = isQCorrect(q)
              const isAns = isQAnswered(q)

              return (
                <div
                  key={q.questionId}
                  className="rounded-xl border border-border bg-card/50 overflow-hidden transition-all shadow-xs"
                >
                  {/* Header Row */}
                  <div
                    onClick={() => toggleQuestion(q.questionId)}
                    className="p-4 flex items-center justify-between gap-4 cursor-pointer hover:bg-muted/30 transition-colors select-none"
                  >
                    <div className="flex items-center gap-3 min-w-0">
                      <span className="flex size-7 shrink-0 items-center justify-center rounded-lg bg-muted text-xs font-bold font-mono text-muted-foreground">
                        {q.questionIndex + 1}
                      </span>
                      <div className="min-w-0">
                        <div className="flex items-center gap-2">
                          <span className="text-sm font-semibold text-foreground truncate">
                            {q.title}
                          </span>
                          <Badge variant="outline" className="text-[10px] hidden sm:inline-flex">
                            {q.questionType === "MCQ" ? (
                              <>
                                <CheckSquare className="size-3 text-sky-500 mr-1" />
                                <span>MCQ</span>
                              </>
                            ) : (
                              <>
                                <Code2 className="size-3 text-emerald-500 mr-1" />
                                <span>CODING</span>
                              </>
                            )}
                          </Badge>
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-3 shrink-0">
                      {/* Score Badge */}
                      <span className="text-xs font-mono font-semibold">
                        {q.pointsAwarded} / {q.maxPoints} pts
                      </span>

                      {/* Status Outcome Badge */}
                      {isCorrect ? (
                        <Badge className="bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border-emerald-500/30 text-[11px] gap-1">
                          <CheckCircle2 className="size-3" />
                          <span>Correct</span>
                        </Badge>
                      ) : isAns ? (
                        <Badge className="bg-destructive/15 text-destructive border-destructive/30 text-[11px] gap-1">
                          <XCircle className="size-3" />
                          <span>Incorrect</span>
                        </Badge>
                      ) : (
                        <Badge variant="outline" className="text-muted-foreground text-[11px]">
                          Unanswered
                        </Badge>
                      )}

                      {/* Expand / Collapse Icon */}
                      <Button variant="ghost" size="icon" className="size-7">
                        {isExpanded ? (
                          <ChevronUp className="size-4" />
                        ) : (
                          <ChevronDown className="size-4" />
                        )}
                      </Button>
                    </div>
                  </div>

                  {/* Expanded Detail Panel */}
                  {isExpanded && (
                    <div className="border-t border-border p-5 space-y-6 bg-card/20">
                      {q.questionType === "MCQ" ? (
                        <ResultsMcqItem question={q} />
                      ) : (
                        <ResultsCodingItem question={q} />
                      )}
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        </div>

        {/* Bottom CTA */}
        <div className="pt-6 border-t border-border/60 flex justify-center">
          <Link to="/">
            <Button variant="outline" className="gap-2 text-xs">
              <ArrowLeft className="size-3.5" />
              <span>Return to Dashboard</span>
            </Button>
          </Link>
        </div>
      </main>
    </div>
  )
}
export default AssessmentResultsPage
