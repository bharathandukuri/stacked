import { useState, useEffect, useCallback } from "react"
import axios from "axios"
import { createFileRoute, useNavigate, Link } from "@tanstack/react-router"
import {
  Clock,
  AlertCircle,
  CheckCircle2,
  Play,
  ArrowRight,
  ShieldAlert,
  Loader2,
  Terminal,
  Lock,
  Wifi,
  Monitor,
  UserCheck,
  CalendarCheck,
  RefreshCw,
  XCircle,
  Check,
  FileText,
  Award,
  Hourglass,
} from "lucide-react"

import { Logo } from "@/components/logo"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Card, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import {
  useAssessmentReadiness,
  useCandidateAssessmentSummary,
  useStartAttempt,
} from "@/hooks/use-assessments"
import { assessmentService } from "@/services/assessment-service"
import { useIsAuthenticated, useIsInitialized } from "@/stores/auth-store"
import { initializeAuthSession } from "@/hooks/use-auth"
import { cn } from "@/lib/utils"
import type { CandidateParticipationStatus } from "@/types/assessment"

export const Route = createFileRoute("/assessments/$assessmentId/")({
  component: CandidateAssessmentLobbyPage,
})

interface CheckItem {
  id: string
  name: string
  description: string
  icon: React.ComponentType<{ className?: string }>
  status: "CHECKING" | "PASSED" | "WARNING" | "FAILED"
  detail?: string
}

function CandidateAssessmentLobbyPage() {
  const { assessmentId } = Route.useParams()
  const navigate = useNavigate()
  const isAuthenticated = useIsAuthenticated()
  const isInitialized = useIsInitialized()

  const [confirmOpen, setConfirmOpen] = useState(false)
  const [latencyMs, setLatencyMs] = useState<number | null>(null)
  const [wsReachable, setWsReachable] = useState<boolean | null>(null)
  const [screenValid, setScreenValid] = useState<boolean | null>(null)
  const [isEvaluatingChecks, setIsEvaluatingChecks] = useState(true)
  const [countdownSeconds, setCountdownSeconds] = useState<number | null>(null)

  // Auth Initialization
  useEffect(() => {
    if (!isInitialized) {
      initializeAuthSession()
    }
  }, [isInitialized])

  const {
    data: readiness,
    isLoading: loadingReadiness,
    isError: isReadinessError,
    error: readinessError,
    refetch: refetchReadiness,
  } = useAssessmentReadiness(assessmentId, {
    enabled: Boolean(isInitialized && isAuthenticated),
  })

  const { isLoading: loadingSummary } = useCandidateAssessmentSummary(assessmentId, {
    enabled: Boolean(isInitialized && isAuthenticated),
  })

  const startMutation = useStartAttempt(assessmentId)

  // Sync remaining countdown seconds for active attempts
  useEffect(() => {
    if (readiness?.hasActiveAttempt && readiness.remainingSeconds !== undefined && readiness.remainingSeconds !== null) {
      setCountdownSeconds(readiness.remainingSeconds)
    }
  }, [readiness?.hasActiveAttempt, readiness?.remainingSeconds])

  // Active countdown timer interval
  useEffect(() => {
    if (countdownSeconds === null || countdownSeconds <= 0) return
    const timer = setInterval(() => {
      setCountdownSeconds((prev) => {
        if (prev === null || prev <= 1) {
          refetchReadiness()
          return 0
        }
        return prev - 1
      })
    }, 1000)
    return () => clearInterval(timer)
  }, [countdownSeconds, refetchReadiness])

  const formatTimer = (totalSec?: number | null) => {
    if (!totalSec || totalSec <= 0) return "00:00"
    const hours = Math.floor(totalSec / 3600)
    const minutes = Math.floor((totalSec % 3600) / 60)
    const seconds = totalSec % 60
    if (hours > 0) {
      return `${hours}h ${minutes.toString().padStart(2, "0")}m ${seconds.toString().padStart(2, "0")}s`
    }
    return `${minutes.toString().padStart(2, "0")}m ${seconds.toString().padStart(2, "0")}s`
  }

  // Diagnostic Checks
  const runDiagnostics = useCallback(async () => {
    setIsEvaluatingChecks(true)

    // 1. Screen resolution check
    const width = window.innerWidth
    const height = window.innerHeight
    setScreenValid(width >= 1024 && height >= 600)

    // 2. Latency check via API client (only if authenticated)
    if (isAuthenticated) {
      const startPing = performance.now()
      try {
        await assessmentService.getReadiness(assessmentId)
        const endPing = performance.now()
        setLatencyMs(Math.round(endPing - startPing))
      } catch {
        setLatencyMs(null)
      }
    }

    // 3. Network & WebSocket reachability
    setWsReachable(typeof WebSocket !== "undefined" && navigator.onLine)
    setIsEvaluatingChecks(false)
  }, [assessmentId, isAuthenticated])

  useEffect(() => {
    if (isInitialized && isAuthenticated) {
      runDiagnostics()
    }
  }, [isInitialized, isAuthenticated, runDiagnostics])

  const handleConfirmStart = async () => {
    try {
      await startMutation.mutateAsync()
      setConfirmOpen(false)
      navigate({
        to: "/assessments/$assessmentId/take",
        params: { assessmentId },
      })
    } catch {
      // handled by mutation toast
    }
  }

  // Not yet initialized
  if (!isInitialized) {
    return (
      <div className="flex h-screen w-full items-center justify-center bg-background">
        <Loader2 className="size-8 animate-spin text-primary" />
      </div>
    )
  }

  // Unauthenticated Candidate
  if (!isAuthenticated) {
    return (
      <div className="flex min-h-screen flex-col justify-between bg-background">
        <header className="border-b border-border bg-card/60 backdrop-blur-md px-6 py-4">
          <div className="mx-auto flex max-w-5xl items-center justify-between">
            <Link to="/dashboard" className="flex items-center gap-3">
              <Logo size={28} />
              <span className="text-base font-bold tracking-tight text-foreground">Stacked</span>
            </Link>
          </div>
        </header>

        <main className="mx-auto flex w-full max-w-md flex-1 flex-col items-center justify-center p-6 text-center">
          <div className="w-full space-y-5 rounded-2xl border border-border bg-card p-8 shadow-sm">
            <div className="mx-auto flex size-12 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <Lock className="size-6" />
            </div>
            <div className="space-y-1.5">
              <h2 className="text-lg font-bold text-foreground">Sign In Required</h2>
              <p className="text-xs text-muted-foreground">
                Please sign in or enter an invitation code to access this assessment.
              </p>
            </div>
            <div className="flex flex-col gap-2 pt-2">
              <Button
                onClick={() =>
                  navigate({
                    to: "/login",
                  })
                }
                className="w-full text-xs font-semibold"
              >
                Sign In to Account
              </Button>
              <Link to="/assessments/join">
                <Button variant="outline" className="w-full text-xs">
                  Enter Invitation Code
                </Button>
              </Link>
            </div>
          </div>
        </main>

        <footer className="border-t border-border/60 py-4 text-center text-xs text-muted-foreground">
          Stacked Assessment Platform
        </footer>
      </div>
    )
  }

  const isLoading = loadingReadiness || loadingSummary

  if (isLoading) {
    return (
      <div className="flex h-screen w-full items-center justify-center bg-background">
        <div className="flex flex-col items-center gap-3 text-muted-foreground">
          <Loader2 className="size-8 animate-spin text-primary" />
          <p className="text-xs font-medium">Verifying assessment readiness...</p>
        </div>
      </div>
    )
  }

  if (isReadinessError || !readiness) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center bg-background p-6">
        <div className="max-w-md text-center space-y-4">
          <AlertCircle className="size-10 text-destructive mx-auto" />
          <h2 className="text-lg font-bold">Access Restricted</h2>
          <p className="text-xs text-muted-foreground">
            {axios.isAxiosError(readinessError)
              ? (readinessError.response?.data as { message?: string } | undefined)?.message ||
                "You are not assigned to this assessment or it is currently closed."
              : readinessError instanceof Error
                ? readinessError.message
                : "You are not assigned to this assessment or it is currently closed."}
          </p>
          <div className="pt-2">
            <Link to="/dashboard">
              <Button variant="outline" size="sm" className="text-xs">Return to Dashboard</Button>
            </Link>
          </div>
        </div>
      </div>
    )
  }

  const getStatusBadge = (status: CandidateParticipationStatus) => {
    switch (status) {
      case "INVITED":
        return <Badge variant="secondary" className="text-xs">Ready to Start</Badge>
      case "IN_PROGRESS":
        return (
          <Badge className="bg-amber-500/15 text-amber-600 dark:text-amber-400 border-amber-500/30 text-xs animate-pulse">
            In Progress
          </Badge>
        )
      case "SUBMITTED":
        return (
          <Badge className="bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border-emerald-500/30 text-xs">
            Completed
          </Badge>
        )
      case "EXPIRED":
        return <Badge variant="destructive" className="text-xs">Expired</Badge>
      default:
        return <Badge variant="outline" className="text-xs">{status}</Badge>
    }
  }

  // Diagnostic Checklist
  const checks: CheckItem[] = [
    {
      id: "auth",
      name: "Candidate Profile",
      description: "Candidate account authenticated.",
      icon: UserCheck,
      status: readiness.authenticated ? "PASSED" : "FAILED",
      detail: readiness.candidateEmail || undefined,
    },
    {
      id: "network",
      name: "Server Latency",
      description: "Roundtrip connection to execution clusters.",
      icon: Wifi,
      status:
        latencyMs === null
          ? isEvaluatingChecks
            ? "CHECKING"
            : "FAILED"
          : latencyMs < 500
            ? "PASSED"
            : "WARNING",
      detail: latencyMs !== null ? `${latencyMs} ms` : "Checking...",
    },
    {
      id: "screen",
      name: "Viewport & Display",
      description: "Full desktop resolution verified.",
      icon: Monitor,
      status: screenValid === null ? "CHECKING" : screenValid ? "PASSED" : "WARNING",
      detail: `${window.innerWidth}×${window.innerHeight}px`,
    },
    {
      id: "ws",
      name: "Real-time Connectivity",
      description: "WebSocket connection for continuous sync.",
      icon: Terminal,
      status: wsReachable === null ? "CHECKING" : wsReachable ? "PASSED" : "FAILED",
      detail: wsReachable ? "Connected" : "Checking...",
    },
    {
      id: "schedule",
      name: "Eligibility & Window",
      description: "Assessment window and attempt validation.",
      icon: CalendarCheck,
      status:
        readiness.canStart || readiness.hasActiveAttempt || readiness.participationStatus === "SUBMITTED" || readiness.participationStatus === "EXPIRED"
          ? "PASSED"
          : "FAILED",
      detail:
        readiness.hasActiveAttempt
          ? `Attempt in progress`
          : readiness.canStart
            ? `Attempt ${readiness.attemptsUsed + 1}${readiness.maxAttempts ? ` of ${readiness.maxAttempts}` : ""}`
            : readiness.cannotStartReason || readiness.participationStatus,
    },
  ]

  const allPassed = checks.every((c) => c.status === "PASSED" || c.status === "WARNING")
  const canStartAssessment = (readiness.canStart || readiness.hasActiveAttempt) && allPassed
  const isConcluded = readiness.participationStatus === "SUBMITTED" || readiness.participationStatus === "EXPIRED"

  return (
    <div className="min-h-screen w-full bg-background flex flex-col justify-between font-sans">
      {/* Top Header */}
      <header className="border-b border-border bg-card/60 backdrop-blur-md px-6 py-4 sticky top-0 z-20">
        <div className="mx-auto flex max-w-5xl items-center justify-between">
          <Link to="/dashboard" className="flex items-center gap-2.5">
            <Logo size={26} />
            <span className="text-sm font-bold tracking-tight text-foreground">Stacked</span>
          </Link>
          <div className="flex items-center gap-3">
            {readiness.candidateEmail && (
              <span className="text-xs text-muted-foreground hidden sm:inline-block font-mono">
                {readiness.candidateEmail}
              </span>
            )}
            <Link to="/dashboard">
              <Button variant="ghost" size="sm" className="text-xs text-muted-foreground hover:text-foreground">
                Exit to Dashboard
              </Button>
            </Link>
          </div>
        </div>
      </header>

      {/* Main Container */}
      <main className="mx-auto w-full max-w-5xl flex-1 px-4 py-8 space-y-6">
        {/* Title & Metadata Hero Header */}
        <div className="space-y-3 pb-2 border-b border-border/60">
          <div className="flex flex-wrap items-center gap-2">
            {getStatusBadge(readiness.participationStatus)}
            <Badge variant="outline" className="text-xs gap-1">
              <FileText className="size-3 text-primary" />
              <span>{readiness.totalQuestions} Questions</span>
            </Badge>
            <Badge variant="outline" className="text-xs gap-1">
              <Award className="size-3 text-emerald-500" />
              <span>{readiness.totalScore} Points</span>
            </Badge>
            <Badge variant="outline" className="text-xs gap-1">
              <Clock className="size-3 text-amber-500" />
              <span>{readiness.durationMinutes} Minutes</span>
            </Badge>
            {readiness.maxAttempts ? (
              <Badge variant="outline" className="text-xs">
                Max {readiness.maxAttempts} Attempt{readiness.maxAttempts > 1 ? "s" : ""}
              </Badge>
            ) : null}
          </div>

          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-foreground">
            {readiness.title}
          </h1>

          {readiness.description && (
            <p className="text-xs sm:text-sm text-muted-foreground leading-relaxed max-w-3xl">
              {readiness.description}
            </p>
          )}
        </div>

        {/* State Banner 1: Active In-Progress Session (Candidate returned before deadline) */}
        {readiness.hasActiveAttempt && (
          <Card className="border-amber-500/50 bg-amber-500/10 shadow-sm animate-in fade-in duration-200">
            <CardHeader className="pb-3">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div className="space-y-1">
                  <CardTitle className="text-base font-bold text-amber-600 dark:text-amber-400 flex items-center gap-2">
                    <Clock className="size-5 animate-pulse" />
                    <span>Active Assessment Session in Progress</span>
                  </CardTitle>
                  <CardDescription className="text-xs text-foreground/80">
                    You have an existing attempt running. Your previous answers and code solutions are automatically saved.
                  </CardDescription>
                </div>
                {countdownSeconds !== null && (
                  <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-amber-500/20 border border-amber-500/30 text-amber-600 dark:text-amber-300 font-mono text-sm font-bold shrink-0">
                    <Hourglass className="size-4 animate-spin text-amber-500" />
                    <span>{formatTimer(countdownSeconds)} remaining</span>
                  </div>
                )}
              </div>
            </CardHeader>
            <CardFooter className="pt-0">
              <Link
                to="/assessments/$assessmentId/take"
                params={{ assessmentId }}
                className="w-full sm:w-auto"
              >
                <Button size="sm" className="w-full sm:w-auto gap-2 bg-amber-600 hover:bg-amber-700 text-white font-semibold text-xs shadow-sm">
                  <span>Resume Assessment Now</span>
                  <ArrowRight className="size-3.5" />
                </Button>
              </Link>
            </CardFooter>
          </Card>
        )}


        {/* Manual Administrator Intervention Banner */}
        {readiness.manualInterventionMessage && (
          <Card className="border-primary/40 bg-primary/5">
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-semibold text-primary flex items-center gap-2">
                <AlertCircle className="size-4" />
                <span>Notice from Administrator</span>
              </CardTitle>
              <CardDescription className="text-xs text-foreground/90">
                {readiness.manualInterventionMessage}
              </CardDescription>
            </CardHeader>
          </Card>
        )}

        {isConcluded ? (
          <Card className="border-border bg-card shadow-sm p-6 sm:p-8 space-y-6 text-center max-w-2xl mx-auto">
            <div className="size-16 rounded-full bg-muted/60 flex items-center justify-center mx-auto">
              {readiness.participationStatus === "SUBMITTED" ? (
                <CheckCircle2 className="size-8 text-emerald-500" />
              ) : (
                <Clock className="size-8 text-amber-500" />
              )}
            </div>

            <div className="space-y-2">
              <h2 className="text-xl font-bold text-foreground">
                {readiness.participationStatus === "SUBMITTED"
                  ? "Assessment Successfully Submitted"
                  : "Assessment Time Limit Concluded"}
              </h2>
              <p className="text-xs text-muted-foreground max-w-md mx-auto leading-relaxed">
                {readiness.participationStatus === "SUBMITTED"
                  ? "Your assessment has been officially submitted and evaluated. You can review your score and detailed question breakdown below."
                  : "The allotted duration for this assessment has ended. All MCQ responses and code evaluations completed prior to the deadline were automatically saved and evaluated."}
              </p>
            </div>

            {/* Assessment Quick Stats */}
            <div className="flex flex-wrap items-center justify-center gap-4 py-3 border-y border-border/60 text-xs text-muted-foreground">
              <div className="flex items-center gap-1.5">
                <span>Questions:</span>
                <strong className="text-foreground font-mono">{readiness.totalQuestions}</strong>
              </div>
              <span>•</span>
              <div className="flex items-center gap-1.5">
                <span>Total Points:</span>
                <strong className="text-foreground font-mono">{readiness.totalScore} pts</strong>
              </div>
              <span>•</span>
              <div className="flex items-center gap-1.5">
                <span>Allotted Time:</span>
                <strong className="text-foreground font-mono">{readiness.durationMinutes} min</strong>
              </div>
              <span>•</span>
              <div className="flex items-center gap-1.5">
                <span>Attempts:</span>
                <strong className="text-foreground font-mono">{readiness.attemptsUsed} / {readiness.maxAttempts || 1}</strong>
              </div>
            </div>

            <div className="pt-2 flex flex-col sm:flex-row items-center justify-center gap-3">
              <Link to="/assessments/$assessmentId/results" params={{ assessmentId }} className="w-full sm:w-auto">
                <Button size="default" className="w-full sm:w-auto gap-2 font-semibold text-xs px-6">
                  <span>View Assessment Results</span>
                  <ArrowRight className="size-4" />
                </Button>
              </Link>
              <Link to="/dashboard" className="w-full sm:w-auto">
                <Button variant="outline" size="default" className="w-full sm:w-auto text-xs px-4">
                  Return to Dashboard
                </Button>
              </Link>
            </div>
          </Card>
        ) : (
        /* Two-Column Grid: Left Diagnostics, Right Rules & Begin */
        <div className="grid grid-cols-1 md:grid-cols-5 gap-6">
          {/* Left: System Diagnostics (3 cols) */}
          <div className="md:col-span-3 space-y-4">
            <div className="rounded-xl border border-border bg-card p-5 space-y-4 shadow-xs">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-sm font-semibold text-foreground">Pre-Test System Diagnostics</h3>
                  <p className="text-xs text-muted-foreground">Verification of browser environment and server connectivity</p>
                </div>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => {
                    refetchReadiness()
                    runDiagnostics()
                  }}
                  disabled={isEvaluatingChecks}
                  className="h-8 text-xs gap-1.5 text-muted-foreground hover:text-foreground"
                >
                  <RefreshCw className={cn("size-3", isEvaluatingChecks ? "animate-spin" : "")} />
                  <span>Re-check</span>
                </Button>
              </div>

              <div className="divide-y divide-border rounded-lg border border-border/60 overflow-hidden">
                {checks.map((check) => {
                  const Icon = check.icon
                  return (
                    <div
                      key={check.id}
                      className="flex items-center justify-between p-3 gap-3 bg-card hover:bg-muted/30 transition-colors"
                    >
                      <div className="flex items-center gap-3 min-w-0">
                        <div className="p-1.5 rounded-md bg-muted text-muted-foreground shrink-0">
                          <Icon className="size-4" />
                        </div>
                        <div className="min-w-0">
                          <div className="text-xs font-semibold text-foreground truncate">
                            {check.name}
                          </div>
                          <p className="text-[11px] text-muted-foreground truncate">
                            {check.detail || check.description}
                          </p>
                        </div>
                      </div>

                      <div className="shrink-0">
                        {check.status === "CHECKING" && (
                          <Badge variant="outline" className="gap-1 text-[11px] py-0">
                            <Loader2 className="size-2.5 animate-spin" />
                            <span>Checking</span>
                          </Badge>
                        )}
                        {check.status === "PASSED" && (
                          <Badge className="bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20 gap-1 text-[11px] py-0">
                            <Check className="size-2.5" />
                            <span>Ready</span>
                          </Badge>
                        )}
                        {check.status === "WARNING" && (
                          <Badge className="bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/20 gap-1 text-[11px] py-0">
                            <AlertCircle className="size-2.5" />
                            <span>Warning</span>
                          </Badge>
                        )}
                        {check.status === "FAILED" && (
                          <Badge variant="destructive" className="gap-1 text-[11px] py-0">
                            <XCircle className="size-2.5" />
                            <span>Failed</span>
                          </Badge>
                        )}
                      </div>
                    </div>
                  )
                })}
              </div>
            </div>
          </div>

          {/* Right: Guidelines & Start Action Card (2 cols) */}
          <div className="md:col-span-2 space-y-4">
            <div className="rounded-xl border border-border bg-card p-5 space-y-4 shadow-xs">
              <h3 className="text-sm font-semibold text-foreground">Assessment Rules</h3>
              <div className="space-y-3 text-xs text-muted-foreground">
                <div className="flex items-start gap-2.5">
                  <Clock className="size-4 text-primary shrink-0 mt-0.5" />
                  <div>
                    <p className="font-semibold text-foreground">Strict Countdown</p>
                    <p className="text-[11px] leading-relaxed">
                      Timer runs continuously from the moment you begin. Closing the tab will not pause the timer.
                    </p>
                  </div>
                </div>

                <div className="flex items-start gap-2.5">
                  <Terminal className="size-4 text-emerald-500 shrink-0 mt-0.5" />
                  <div>
                    <p className="font-semibold text-foreground">Real-time Auto-save</p>
                    <p className="text-[11px] leading-relaxed">
                      Every code revision and option selection is saved automatically. If disconnected, you can safely resume.
                    </p>
                  </div>
                </div>

                <div className="flex items-start gap-2.5">
                  <ShieldAlert className="size-4 text-amber-500 shrink-0 mt-0.5" />
                  <div>
                    <p className="font-semibold text-foreground">Single Tab Session</p>
                    <p className="text-[11px] leading-relaxed">
                      Only one active browser tab is permitted. Opening elsewhere will deactivate the previous session.
                    </p>
                  </div>
                </div>

                <div className="flex items-start gap-2.5">
                  <Award className="size-4 text-purple-500 shrink-0 mt-0.5" />
                  <div>
                    <p className="font-semibold text-foreground">Best Score Retained</p>
                    <p className="text-[11px] leading-relaxed">
                      For coding challenges, your highest scoring submission is preserved even if subsequent runs fail.
                    </p>
                  </div>
                </div>
              </div>

              {/* Action Button Row */}
              <div className="border-t border-border pt-4">
                {readiness.participationStatus === "INVITED" && (
                  <div className="space-y-2">
                    {readiness.canStart ? (
                      <Button
                        size="sm"
                        disabled={!canStartAssessment || startMutation.isPending}
                        onClick={() => setConfirmOpen(true)}
                        className="w-full gap-2 font-semibold text-xs shadow-sm h-9"
                      >
                        <Play className="size-3.5 fill-current" />
                        <span>Begin Assessment</span>
                      </Button>
                    ) : (
                      <div className="p-3 rounded-lg border border-destructive/30 bg-destructive/10 text-destructive text-xs">
                        <p className="font-semibold flex items-center gap-1.5">
                          <AlertCircle className="size-3.5 shrink-0" />
                          <span>Cannot Start</span>
                        </p>
                        <p className="text-[11px] mt-0.5">
                          {readiness.cannotStartReason || "Assessment is currently closed."}
                        </p>
                      </div>
                    )}
                  </div>
                )}

                {readiness.hasActiveAttempt && (
                  <Link
                    to="/assessments/$assessmentId/take"
                    params={{ assessmentId }}
                    className="block w-full"
                  >
                    <Button
                      size="sm"
                      className="w-full gap-2 bg-amber-600 hover:bg-amber-700 text-white font-semibold text-xs shadow-sm h-9"
                    >
                      <Play className="size-3.5 fill-current" />
                      <span>Resume Active Attempt</span>
                    </Button>
                  </Link>
                )}

              </div>
            </div>
          </div>
        </div>
        )}
      </main>

      {/* Confirmation Dialog */}
      <Dialog open={confirmOpen} onOpenChange={setConfirmOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-base">
              <Play className="size-4 text-primary fill-primary" />
              <span>Begin Assessment</span>
            </DialogTitle>
            <DialogDescription className="text-xs">
              Your <strong>{readiness.durationMinutes}-minute</strong> countdown timer begins immediately and cannot be paused.
            </DialogDescription>
          </DialogHeader>

          <div className="py-2 space-y-2 text-xs text-muted-foreground border-y my-1">
            <div className="flex items-center justify-between">
              <span>Total Questions:</span>
              <strong className="text-foreground">{readiness.totalQuestions}</strong>
            </div>
            <div className="flex items-center justify-between">
              <span>Maximum Points:</span>
              <strong className="text-foreground">{readiness.totalScore} pts</strong>
            </div>
            <div className="flex items-center justify-between">
              <span>Allocated Time:</span>
              <strong className="text-foreground">{readiness.durationMinutes} minutes</strong>
            </div>
          </div>

          <DialogFooter className="gap-2 sm:gap-0 pt-2">
            <Button variant="outline" size="sm" onClick={() => setConfirmOpen(false)} className="text-xs">
              Cancel
            </Button>
            <Button
              size="sm"
              onClick={handleConfirmStart}
              disabled={startMutation.isPending}
              className="gap-2 text-xs font-semibold"
            >
              {startMutation.isPending && <Loader2 className="size-3.5 animate-spin" />}
              <span>Start Now</span>
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Footer */}
      <footer className="border-t border-border/60 py-3 text-center text-[11px] text-muted-foreground">
        Stacked Assessment Platform
      </footer>
    </div>
  )
}
export default CandidateAssessmentLobbyPage
