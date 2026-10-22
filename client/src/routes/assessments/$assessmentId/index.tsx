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

  useEffect(() => {
    if (readiness?.participationStatus === "SUBMITTED") {
      navigate({
        to: "/assessments/$assessmentId/results",
        params: { assessmentId },
        replace: true,
      })
    }
  }, [readiness?.participationStatus, assessmentId, navigate])

  const handleConfirmStart = async () => {
    try {
      await startMutation.mutateAsync()
      setConfirmOpen(false)
      navigate({
        to: "/assessments/$assessmentId/take",
        params: { assessmentId },
      })
    } catch {
      // handled by toast
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

  if (readiness?.participationStatus === "SUBMITTED") {
    return (
      <div className="flex h-screen w-full items-center justify-center bg-background">
        <div className="flex flex-col items-center gap-3 text-muted-foreground">
          <Loader2 className="size-8 animate-spin text-emerald-500" />
          <p className="text-xs font-medium">Assessment completed. Redirecting to your results...</p>
          <Link to="/assessments/$assessmentId/results" params={{ assessmentId }}>
            <Button variant="outline" size="sm" className="mt-2 text-xs">
              Go to Results Now
            </Button>
          </Link>
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
      name: "Identity Verification",
      description: "Candidate profile authenticated.",
      icon: UserCheck,
      status: readiness.authenticated ? "PASSED" : "FAILED",
      detail: readiness.candidateEmail || undefined,
    },
    {
      id: "network",
      name: "Network & Latency",
      description: "Roundtrip connection to execution servers.",
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
      name: "Display & Viewport",
      description: "Desktop viewport verification.",
      icon: Monitor,
      status: screenValid === null ? "CHECKING" : screenValid ? "PASSED" : "WARNING",
      detail: `${window.innerWidth}×${window.innerHeight}px`,
    },
    {
      id: "ws",
      name: "Real-time Presence",
      description: "Live connection for session tracking.",
      icon: Terminal,
      status: wsReachable === null ? "CHECKING" : wsReachable ? "PASSED" : "FAILED",
      detail: wsReachable ? "Active" : "Checking...",
    },
    {
      id: "schedule",
      name: "Eligibility & Window",
      description: "Assessment window and attempt limits.",
      icon: CalendarCheck,
      status:
        readiness.canStart || readiness.hasActiveAttempt
          ? "PASSED"
          : "FAILED",
      detail:
        readiness.canStart || readiness.hasActiveAttempt
          ? `Attempt ${readiness.attemptsUsed + 1}${readiness.maxAttempts ? ` of ${readiness.maxAttempts}` : ""}`
          : readiness.cannotStartReason || "Closed",
    },
  ]

  const allPassed = checks.every((c) => c.status === "PASSED" || c.status === "WARNING")
  const canStartAssessment = (readiness.canStart || readiness.hasActiveAttempt) && allPassed

  return (
    <div className="min-h-screen w-full bg-background flex flex-col justify-between font-sans">
      {/* Top Header */}
      <header className="border-b border-border bg-card/60 backdrop-blur-md px-6 py-4">
        <div className="mx-auto flex max-w-4xl items-center justify-between">
          <Link to="/dashboard" className="flex items-center gap-2.5">
            <Logo size={26} />
            <span className="text-sm font-bold tracking-tight text-foreground">Stacked</span>
          </Link>
          <Link to="/dashboard">
            <Button variant="ghost" size="sm" className="text-xs text-muted-foreground">
              Exit Lobby
            </Button>
          </Link>
        </div>
      </header>

      {/* Main Container */}
      <main className="mx-auto w-full max-w-4xl flex-1 px-4 py-8 space-y-6">
        {/* Title & Metadata Header */}
        <div className="space-y-2">
          <div className="flex flex-wrap items-center gap-2">
            {getStatusBadge(readiness.participationStatus)}
            <Badge variant="outline" className="text-xs">
              {readiness.totalQuestions} Questions • {readiness.totalScore} Points
            </Badge>
            <Badge variant="outline" className="text-xs">
              {readiness.durationMinutes} Minutes
            </Badge>
            {readiness.maxAttempts ? (
              <Badge variant="outline" className="text-xs">
                Max {readiness.maxAttempts} Attempt{readiness.maxAttempts > 1 ? "s" : ""}
              </Badge>
            ) : null}
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">
            {readiness.title}
          </h1>
          {readiness.description && (
            <p className="text-xs text-muted-foreground leading-relaxed max-w-2xl">
              {readiness.description}
            </p>
          )}
        </div>

        {/* Active Attempt Banner */}
        {readiness.hasActiveAttempt && (
          <Card className="border-amber-500/40 bg-amber-500/5">
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-semibold text-amber-600 dark:text-amber-400 flex items-center gap-2">
                <Clock className="size-4" />
                <span>Active Attempt in Progress</span>
              </CardTitle>
              <CardDescription className="text-xs">
                You have a running attempt. Resume to continue your answers.
              </CardDescription>
            </CardHeader>
            <CardFooter>
              <Link
                to="/assessments/$assessmentId/take"
                params={{ assessmentId }}
              >
                <Button size="sm" className="gap-2 bg-amber-600 hover:bg-amber-700 text-white font-medium text-xs">
                  <span>Resume Assessment</span>
                  <ArrowRight className="size-3.5" />
                </Button>
              </Link>
            </CardFooter>
          </Card>
        )}

        {/* Diagnostic Readiness */}
        <div className="rounded-xl border border-border bg-card p-5 space-y-4 shadow-sm">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-sm font-semibold text-foreground">System Checks</h3>
              <p className="text-xs text-muted-foreground">Pre-test environment verification</p>
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

        {/* Essential Rules */}
        <div className="rounded-xl border border-border bg-card p-5 space-y-3 shadow-sm">
          <h3 className="text-sm font-semibold text-foreground">Guidelines</h3>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs text-muted-foreground">
            <div className="flex items-start gap-2">
              <Clock className="size-4 text-primary shrink-0 mt-0.5" />
              <div>
                <p className="font-semibold text-foreground">Timer Continues</p>
                <p className="text-[11px]">Countdown starts immediately upon confirmation.</p>
              </div>
            </div>
            <div className="flex items-start gap-2">
              <ShieldAlert className="size-4 text-amber-500 shrink-0 mt-0.5" />
              <div>
                <p className="font-semibold text-foreground">Single Tab</p>
                <p className="text-[11px]">Multiple tabs will invalidate the session.</p>
              </div>
            </div>
            <div className="flex items-start gap-2">
              <Terminal className="size-4 text-emerald-500 shrink-0 mt-0.5" />
              <div>
                <p className="font-semibold text-foreground">Auto Saved</p>
                <p className="text-[11px]">Answers and code runs are persisted continuously.</p>
              </div>
            </div>
          </div>

          {/* Action Row */}
          {readiness.participationStatus === "INVITED" && (
            <div className="border-t border-border pt-4 flex flex-col sm:flex-row items-center justify-between gap-3">
              <div className="text-xs">
                {readiness.canStart ? (
                  <span className="text-emerald-600 dark:text-emerald-400 font-medium flex items-center gap-1.5">
                    <CheckCircle2 className="size-3.5" />
                    Eligibility verified. You may begin.
                  </span>
                ) : (
                  <span className="text-destructive font-medium flex items-center gap-1.5">
                    <AlertCircle className="size-3.5" />
                    {readiness.cannotStartReason || "Assessment is currently closed."}
                  </span>
                )}
              </div>

              <Button
                size="sm"
                disabled={!canStartAssessment || startMutation.isPending}
                onClick={() => setConfirmOpen(true)}
                className="w-full sm:w-auto gap-2 px-6 font-semibold text-xs shadow-sm"
              >
                <Play className="size-3.5 fill-current" />
                <span>Begin Assessment</span>
              </Button>
            </div>
          )}
        </div>
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

          <div className="py-2 space-y-1.5 text-xs text-muted-foreground">
            <p>• {readiness.totalQuestions} Questions ({readiness.totalScore} Points)</p>
            <p>• Single active browser session allowed</p>
          </div>

          <DialogFooter className="gap-2 sm:gap-0">
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
