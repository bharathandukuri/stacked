import { useState, useMemo } from "react"
import axios from "axios"
import { createFileRoute, Link } from "@tanstack/react-router"
import {
  CheckCircle2,
  XCircle,
  Clock,
  Award,
  BarChart3,
  Code2,
  CheckSquare,
  ChevronDown,
  ChevronUp,
  AlertCircle,
  ShieldCheck,
  ArrowLeft,
  Loader2,
  FileCode,
} from "lucide-react"

import { Logo } from "@/components/logo"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Card } from "@/components/ui/card"
import { MonacoEditor } from "@/components/monaco-editor"
import { MarkdownPreview } from "@/components/markdown-preview/markdown-preview"
import { useCandidateResults } from "@/hooks/use-assessments"
import { resolveMonacoLanguage } from "@/lib/monaco-languages"
import { cn } from "@/lib/utils"

export const Route = createFileRoute("/assessments/$assessmentId/results")({
  component: AssessmentResultsPage,
})

type FilterType = "ALL" | "CORRECT" | "INCORRECT" | "UNANSWERED"

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

  // Format seconds into readable string (e.g. "34m 12s" or "1h 10m 22s")
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

  const questionList = useMemo(
    () => results?.questionResults ?? [],
    [results?.questionResults]
  )

  const correctCount = useMemo(() => {
    return questionList.filter((q) => q.isCorrect === true).length
  }, [questionList])

  const incorrectCount = useMemo(() => {
    return questionList.filter((q) => q.isAnswered && q.isCorrect === false).length
  }, [questionList])

  const unansweredCount = useMemo(() => {
    return questionList.filter((q) => !q.isAnswered).length
  }, [questionList])

  const filteredQuestions = useMemo(() => {
    return questionList.filter((q) => {
      if (activeFilter === "CORRECT") return q.isCorrect === true
      if (activeFilter === "INCORRECT") return q.isAnswered && q.isCorrect === false
      if (activeFilter === "UNANSWERED") return !q.isAnswered
      return true
    })
  }, [questionList, activeFilter])

  if (isLoading) {
    return (
      <div className="flex h-screen w-full items-center justify-center bg-background">
        <div className="flex flex-col items-center gap-3 text-muted-foreground">
          <Loader2 className="size-8 animate-spin text-primary" />
          <p className="text-sm font-medium">Computing evaluation results...</p>
        </div>
      </div>
    )
  }

  if (isError || !results) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center bg-background p-6">
        <div className="max-w-md text-center space-y-4">
          <AlertCircle className="size-12 text-destructive mx-auto" />
          <h2 className="text-2xl font-bold">Unable to Load Assessment Results</h2>
          <p className="text-sm text-muted-foreground">
            {axios.isAxiosError(error)
              ? (error.response?.data as { message?: string } | undefined)?.message ||
                "Could not retrieve your submission results. Please try again."
              : error instanceof Error
                ? error.message
                : "Could not retrieve your submission results. Please try again."}
          </p>
          <div className="flex justify-center gap-3 pt-2">
            <Button onClick={() => refetch()} variant="outline">
              Try Again
            </Button>
            <Link to="/">
              <Button>Dashboard</Button>
            </Link>
          </div>
        </div>
      </div>
    )
  }

  // If results are confidential
  if (!results.resultsVisible) {
    return (
      <div className="min-h-screen bg-background text-foreground flex flex-col">
        {/* Navigation Bar */}
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
            <div className="size-16 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-500 flex items-center justify-center mx-auto">
              <CheckCircle2 className="size-8" />
            </div>

            <div className="space-y-2">
              <h2 className="text-2xl font-bold tracking-tight">Assessment Submitted</h2>
              <p className="text-sm text-muted-foreground leading-relaxed">
                Your responses have been successfully recorded and submitted for evaluation. Detailed scores and question breakdowns are confidential and will be shared by the recruitment team.
              </p>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 p-4 rounded-xl border bg-muted/20 text-xs">
              <div>
                <span className="text-muted-foreground">Attempt</span>
                <p className="font-bold text-foreground mt-0.5">#{results.attemptNumber}</p>
              </div>
              <div>
                <span className="text-muted-foreground">Time Taken</span>
                <p className="font-bold text-foreground font-mono mt-0.5">
                  {formatTimeSpent(results.timeSpentSeconds)}
                </p>
              </div>
              <div className="col-span-2 sm:col-span-1">
                <span className="text-muted-foreground">Questions Answered</span>
                <p className="font-bold text-foreground mt-0.5">
                  {results.answeredCount} / {results.totalQuestions}
                </p>
              </div>
            </div>

            <div className="pt-2 flex justify-center gap-3">
              <Link to="/">
                <Button className="px-6">Return to Dashboard</Button>
              </Link>
            </div>
          </Card>
        </main>
      </div>
    )
  }

  // Full post-submission results
  return (
    <div className="min-h-screen bg-background text-foreground flex flex-col">
      {/* Top Header */}
      <header className="h-14 border-b border-border bg-card/60 backdrop-blur-md px-4 sm:px-6 flex items-center justify-between shrink-0 sticky top-0 z-20">
        <div className="flex items-center gap-3 min-w-0">
          <Logo size={24} />
          <div className="h-4 w-px bg-border hidden sm:block" />
          <span className="text-sm font-semibold truncate max-w-xs sm:max-w-md">
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
        {/* Hero Score Banner */}
        <div className="rounded-2xl border border-border bg-card/60 p-6 sm:p-8 shadow-sm space-y-6">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <div>
              <div className="flex items-center gap-2 mb-1.5">
                <Badge
                  className={cn(
                    "text-xs font-semibold px-2.5 py-0.5",
                    results.passed
                      ? "bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border-emerald-500/30"
                      : results.passingScore !== undefined
                        ? "bg-destructive/15 text-destructive border-destructive/30"
                        : "bg-primary/10 text-primary border-primary/20"
                  )}
                >
                  {results.passed !== undefined
                    ? results.passed
                      ? "PASSED"
                      : "DID NOT PASS"
                    : "COMPLETED"}
                </Badge>
                <span className="text-xs text-muted-foreground">
                  Attempt #{results.attemptNumber}
                </span>
              </div>
              <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight">
                Assessment Results
              </h1>
              {results.description && (
                <p className="text-xs sm:text-sm text-muted-foreground mt-1 max-w-2xl leading-relaxed">
                  {results.description}
                </p>
              )}
            </div>

            {/* Score Display Card */}
            <div className="flex flex-col items-center sm:items-end p-4 rounded-xl border bg-muted/20 min-w-[160px]">
              <span className="text-xs text-muted-foreground font-medium">Final Score</span>
              <div className="text-3xl sm:text-4xl font-extrabold text-foreground tracking-tight">
                {results.score}{" "}
                <span className="text-lg font-normal text-muted-foreground">
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
                {results.answeredCount}{" "}
                <span className="text-xs font-normal text-muted-foreground">
                  / {results.totalQuestions}
                </span>
              </div>
              <div className="text-[11px] text-emerald-600 dark:text-emerald-400">
                {results.unansweredCount} unanswered
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

        {/* Question-Wise Performance Breakdown */}
        <div className="space-y-4">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
            <div>
              <h2 className="text-lg font-bold tracking-tight text-foreground">
                Question Breakdown
              </h2>
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
              return (
                <div
                  key={q.questionId}
                  className="rounded-xl border border-border bg-card/50 overflow-hidden transition-all shadow-xs"
                >
                  {/* Question Summary Header */}
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
                              <CheckSquare className="size-3 text-sky-500 mr-1" />
                            ) : (
                              <Code2 className="size-3 text-purple-500 mr-1" />
                            )}
                            {q.questionType}
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
                      {q.isCorrect === true ? (
                        <Badge className="bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border-emerald-500/30 text-[11px] gap-1">
                          <CheckCircle2 className="size-3" />
                          <span>Correct</span>
                        </Badge>
                      ) : q.isAnswered ? (
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
                      {/* MCQ Outcome View */}
                      {q.questionType === "MCQ" && (
                        <div className="space-y-4">
                          {q.mcqStatement && (
                            <div className="prose dark:prose-invert max-w-none text-sm text-foreground/90">
                              <MarkdownPreview content={q.mcqStatement} />
                            </div>
                          )}

                          <div className="space-y-2 pt-2 border-t">
                            <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                              Options & Answers
                            </span>
                            <div className="grid grid-cols-1 gap-2 pt-1">
                              {q.options?.map((opt, optIndex) => {
                                const isSelected = q.selectedOptionIds?.includes(opt.id)
                                const isCorrect = opt.isCorrect
                                const optionLetter = String.fromCharCode(65 + optIndex)

                                return (
                                  <div
                                    key={opt.id}
                                    className={cn(
                                      "p-3.5 rounded-lg border text-sm flex items-start gap-3 transition-colors",
                                      isCorrect
                                        ? "border-emerald-500/40 bg-emerald-500/10 text-foreground"
                                        : isSelected
                                          ? "border-destructive/40 bg-destructive/10 text-foreground"
                                          : "border-border/70 bg-card/40 text-muted-foreground"
                                    )}
                                  >
                                    <div
                                      className={cn(
                                        "flex size-5 shrink-0 items-center justify-center rounded-md border text-xs font-bold mt-0.5",
                                        isCorrect
                                          ? "border-emerald-500 bg-emerald-500 text-white"
                                          : isSelected
                                            ? "border-destructive bg-destructive text-white"
                                            : "border-border text-muted-foreground"
                                      )}
                                    >
                                      {isCorrect ? (
                                        <CheckCircle2 className="size-3.5 stroke-[2.5]" />
                                      ) : isSelected ? (
                                        <XCircle className="size-3.5 stroke-[2.5]" />
                                      ) : (
                                        optionLetter
                                      )}
                                    </div>

                                    <div className="flex-1 min-w-0 prose dark:prose-invert max-w-none text-xs leading-relaxed">
                                      <MarkdownPreview content={opt.content} />
                                    </div>

                                    <div className="shrink-0 flex items-center gap-1.5">
                                      {isSelected && (
                                        <Badge variant="outline" className="text-[10px]">
                                          Your Choice
                                        </Badge>
                                      )}
                                      {isCorrect && (
                                        <Badge className="bg-emerald-600 text-white text-[10px]">
                                          Correct Answer
                                        </Badge>
                                      )}
                                    </div>
                                  </div>
                                )
                              })}
                            </div>
                          </div>
                        </div>
                      )}

                      {/* CODING Outcome View */}
                      {q.questionType === "CODING" && (
                        <div className="space-y-5">
                          {q.codingStatement && (
                            <div className="prose dark:prose-invert max-w-none text-sm text-foreground/90">
                              <MarkdownPreview content={q.codingStatement} />
                            </div>
                          )}

                          {/* Candidate Code Submission */}
                          <div className="space-y-2">
                            <div className="flex items-center justify-between">
                              <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider flex items-center gap-1.5">
                                <FileCode className="size-3.5" />
                                <span>Submitted Solution ({q.languageId || "code"})</span>
                              </span>
                            </div>
                            <div className="h-64 rounded-lg overflow-hidden border border-border">
                              <MonacoEditor
                                language={resolveMonacoLanguage(q.languageId)}
                                value={q.code || "// No code was submitted for this question"}
                                height="100%"
                                options={{
                                  readOnly: true,
                                  minimap: { enabled: false },
                                  fontSize: 12,
                                  lineNumbers: "on",
                                  scrollBeyondLastLine: false,
                                  automaticLayout: true,
                                }}
                              />
                            </div>
                          </div>

                          {/* Execution Results */}
                          {q.lastExecutionResult && (
                            <div className="space-y-3 pt-2 border-t border-border">
                              <div className="flex items-center justify-between">
                                <div className="flex items-center gap-2">
                                  <Badge
                                    className={cn(
                                      "text-xs font-semibold",
                                      q.lastExecutionResult.overallStatus === "SUCCESS"
                                        ? "bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border-emerald-500/30"
                                        : "bg-destructive/15 text-destructive border-destructive/30"
                                    )}
                                  >
                                    {q.lastExecutionResult.overallStatus}
                                  </Badge>
                                  <span className="text-xs text-muted-foreground">
                                    Passed {q.lastExecutionResult.passedTestcases} /{" "}
                                    {q.lastExecutionResult.totalTestcases} test cases
                                  </span>
                                </div>
                                <span className="text-xs font-mono text-muted-foreground">
                                  {q.lastExecutionResult.totalExecutionTimeMs} ms
                                </span>
                              </div>

                              {/* Testcase Result Cards */}
                              <div className="space-y-2">
                                {q.lastExecutionResult.results?.map((res, tcIdx) => (
                                  <div
                                    key={tcIdx}
                                    className="p-3 rounded-lg border bg-muted/20 text-xs space-y-2"
                                  >
                                    <div className="flex items-center justify-between">
                                      <span className="font-semibold text-foreground">
                                        Test Case #{tcIdx + 1}
                                      </span>
                                      <Badge
                                        variant={res.passed ? "default" : "destructive"}
                                        className="text-[10px]"
                                      >
                                        {res.status}
                                      </Badge>
                                    </div>

                                    {res.actualOutput && (
                                      <div>
                                        <span className="text-muted-foreground text-[11px]">Output:</span>
                                        <pre className="font-mono p-2 bg-background border rounded text-[11px] mt-0.5 whitespace-pre-wrap">
                                          {res.actualOutput}
                                        </pre>
                                      </div>
                                    )}

                                    {res.expectedOutput && (
                                      <div>
                                        <span className="text-muted-foreground text-[11px]">Expected:</span>
                                        <pre className="font-mono p-2 bg-background border rounded text-[11px] mt-0.5 whitespace-pre-wrap">
                                          {res.expectedOutput}
                                        </pre>
                                      </div>
                                    )}

                                    {res.stderr && (
                                      <div>
                                        <span className="text-destructive font-semibold text-[11px]">
                                          Stderr / Error:
                                        </span>
                                        <pre className="font-mono p-2 bg-destructive/10 text-destructive border border-destructive/30 rounded text-[11px] mt-0.5 whitespace-pre-wrap">
                                          {res.stderr}
                                        </pre>
                                      </div>
                                    )}
                                  </div>
                                ))}
                              </div>
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        </div>

        {/* Back Link */}
        <div className="flex justify-center pt-4">
          <Link to="/">
            <Button variant="outline" className="gap-2">
              <ArrowLeft className="size-4" />
              <span>Return to Dashboard</span>
            </Button>
          </Link>
        </div>
      </main>
    </div>
  )
}
