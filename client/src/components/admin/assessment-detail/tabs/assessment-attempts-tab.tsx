import { useState, useMemo, useEffect } from "react"
import {
  Eye,
  Activity,
  ShieldAlert,
  ShieldCheck,
  Radio,
  Clock,
  Search,
  AlertTriangle,
  ArrowRight,
} from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { TablePagination } from "@/components/common/table-pagination"
import { TableEmptyState } from "@/components/common/table-empty-state"
import { PageLoadingState } from "@/components/common/page-loading-state"
import { cn } from "@/lib/utils"
import type { AttemptReport, AttemptStatus } from "@/types/assessment"

interface AssessmentAttemptsTabProps {
  attempts: AttemptReport[]
  loadingAttempts: boolean
  totalScore: number
  page: number
  totalPages: number
  totalElements: number
  pageSize: number
  onPageChange: (newPage: number) => void
  onPageSizeChange: (newSize: number) => void
  onInspectAttempt: (attemptId: string) => void
  onAuditAttempt: (attemptId: string) => void
}

type FilterOption = "ALL" | "IN_PROGRESS" | "FLAGGED" | "SUBMITTED" | "EXPIRED"

export function AssessmentAttemptsTab({
  attempts,
  loadingAttempts,
  totalScore,
  page,
  totalPages,
  totalElements,
  pageSize,
  onPageChange,
  onPageSizeChange,
  onInspectAttempt,
  onAuditAttempt,
}: AssessmentAttemptsTabProps) {
  const [filterMode, setFilterMode] = useState<FilterOption>("ALL")
  const [searchQuery, setSearchQuery] = useState("")
  const [currentTime, setCurrentTime] = useState(() => Date.now())

  useEffect(() => {
    const timer = setInterval(() => setCurrentTime(Date.now()), 5000)
    return () => clearInterval(timer)
  }, [])

  // Live active sessions
  const liveActiveAttempts = useMemo(
    () => attempts.filter((a) => a.status === "IN_PROGRESS"),
    [attempts]
  )

  // Flagged suspicious attempts
  const flaggedAttempts = useMemo(
    () => attempts.filter((a) => a.integrityFlagsCount > 0),
    [attempts]
  )

  // Filtered attempts for table display
  const displayedAttempts = useMemo(() => {
    return attempts.filter((att) => {
      // Status & Flag Filter
      if (filterMode === "IN_PROGRESS" && att.status !== "IN_PROGRESS") return false
      if (filterMode === "FLAGGED" && att.integrityFlagsCount === 0) return false
      if (filterMode === "SUBMITTED" && att.status !== "SUBMITTED") return false
      if (filterMode === "EXPIRED" && att.status !== "EXPIRED") return false

      // Search Query
      if (searchQuery.trim()) {
        const query = searchQuery.toLowerCase().trim()
        const email = att.candidateEmail?.toLowerCase() || ""
        const id = att.id.toLowerCase()
        return email.includes(query) || id.includes(query)
      }

      return true
    })
  }, [attempts, filterMode, searchQuery])

  const getAttemptBadge = (status: AttemptStatus) => {
    switch (status) {
      case "IN_PROGRESS":
        return (
          <Badge className="bg-amber-500/15 text-amber-600 dark:text-amber-400 border-amber-500/30 gap-1 text-[11px] font-semibold">
            <span className="size-1.5 rounded-full bg-amber-500 animate-pulse" />
            <span>Taking Now</span>
          </Badge>
        )
      case "SUBMITTED":
        return (
          <Badge className="bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border-emerald-500/30 text-[11px] font-medium">
            Submitted
          </Badge>
        )
      case "EXPIRED":
        return (
          <Badge variant="destructive" className="text-[11px]">
            Expired
          </Badge>
        )
      case "CANCELLED":
        return (
          <Badge variant="outline" className="text-[11px]">
            Cancelled
          </Badge>
        )
      default:
        return (
          <Badge variant="outline" className="text-[11px]">
            {status}
          </Badge>
        )
    }
  }

  const formatRemainingMinutes = (deadlineStr: string, currentMs: number) => {
    const diffMs = new Date(deadlineStr).getTime() - currentMs
    if (diffMs <= 0) return "Concluding..."
    const mins = Math.floor(diffMs / 60000)
    const secs = Math.floor((diffMs % 60000) / 1000)
    return `${mins}m ${secs}s left`
  }

  return (
    <div className="space-y-6">
      {/* 1. LIVE CANDIDATE PRESENCE TICKER */}
      <div className="rounded-xl border border-primary/25 bg-gradient-to-r from-primary/10 via-background to-card p-4 shadow-xs">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-border/60">
          <div className="flex items-center gap-2.5">
            <div className="flex size-8 items-center justify-center rounded-lg bg-primary/20 text-primary">
              <Radio className="size-4 animate-pulse" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-sm font-bold text-foreground">
                  Live Operations Center
                </h3>
                <span className="inline-flex items-center px-1.5 py-0.2 rounded-full text-[10px] font-mono font-bold bg-emerald-500/20 text-emerald-600 dark:text-emerald-400 border border-emerald-500/30">
                  {liveActiveAttempts.length} ACTIVE NOW
                </span>
              </div>
              <p className="text-[11px] text-muted-foreground">
                Real-time heartbeat monitoring, proactive tab-blur telemetry, and socket integrity tracking.
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <span className="text-xs text-muted-foreground">
              Total Recorded Attempts: <strong className="text-foreground">{totalElements}</strong>
            </span>
          </div>
        </div>

        {/* Live Active Cards Grid */}
        {liveActiveAttempts.length === 0 ? (
          <div className="pt-3 text-xs text-muted-foreground flex items-center gap-2">
            <Clock className="size-3.5 text-muted-foreground" />
            <span>No candidates are actively taking the assessment right now.</span>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 pt-3">
            {liveActiveAttempts.map((active) => (
              <div
                key={active.id}
                className="rounded-lg border border-border/80 bg-background/80 p-3 shadow-2xs hover:border-primary/40 transition-colors flex flex-col justify-between gap-2"
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="min-w-0">
                    <p className="font-mono text-xs font-semibold text-foreground truncate">
                      {active.candidateEmail}
                    </p>
                    <p className="text-[10px] text-muted-foreground">
                      Attempt #{active.attemptNumber} • Started{" "}
                      {new Date(active.startedAt).toLocaleTimeString([], {
                        hour: "2-digit",
                        minute: "2-digit",
                      })}
                    </p>
                  </div>
                  <Badge className="bg-amber-500/15 text-amber-600 dark:text-amber-400 border-amber-500/30 text-[10px] px-1.5 py-0 font-mono shrink-0">
                    {formatRemainingMinutes(active.effectiveDeadline, currentTime)}
                  </Badge>
                </div>

                <div className="flex items-center justify-between pt-1 border-t border-border/50 text-[11px]">
                  <div>
                    {active.integrityFlagsCount > 0 ? (
                      <span className="inline-flex items-center gap-1 font-semibold text-rose-500 dark:text-rose-400">
                        <ShieldAlert className="size-3" />
                        {active.integrityFlagsCount} flag(s)
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 text-emerald-600 dark:text-emerald-400">
                        <ShieldCheck className="size-3" />
                        Clean Session
                      </span>
                    )}
                  </div>
                  <div className="flex items-center gap-1.5">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => onInspectAttempt(active.id)}
                      className="h-6 text-[10px] px-2"
                    >
                      Inspect
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => onAuditAttempt(active.id)}
                      className="h-6 text-[10px] px-2 gap-1"
                    >
                      <Activity className="size-2.5" />
                      <span>Audit</span>
                    </Button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* 2. SUSPICIOUS INTEGRITY ALERTS HIGHLIGHT BANNER */}
      {flaggedAttempts.length > 0 && (
        <div className="rounded-xl border border-rose-500/30 bg-rose-500/10 p-4 text-xs space-y-2">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-rose-600 dark:text-rose-400 font-bold">
              <AlertTriangle className="size-4 shrink-0" />
              <span>
                Integrity Alerts Detected on {flaggedAttempts.length} Candidate Session(s)
              </span>
            </div>
            <Badge variant="destructive" className="text-[10px] uppercase font-mono">
              Action Required
            </Badge>
          </div>
          <p className="text-muted-foreground text-[11px] leading-relaxed">
            Integrity flags are triggered automatically during candidate audit when candidates lose browser window focus (<code>WINDOW_BLUR</code> tab-switching) or open concurrent multi-tab sessions (<code>STALE_SESSION_REPLACED</code>). Click <strong>Audit</strong> to review their chronological telemetry ledger.
          </p>
          <div className="flex flex-wrap items-center gap-2 pt-1">
            {flaggedAttempts.slice(0, 5).map((f) => (
              <Button
                key={f.id}
                variant="outline"
                size="sm"
                onClick={() => onAuditAttempt(f.id)}
                className="h-7 text-xs border-rose-500/30 bg-background/80 hover:bg-background text-rose-600 dark:text-rose-400 gap-1.5"
              >
                <ShieldAlert className="size-3" />
                <span>
                  {f.candidateEmail} ({f.integrityFlagsCount} flags)
                </span>
                <ArrowRight className="size-2.5 text-muted-foreground" />
              </Button>
            ))}
          </div>
        </div>
      )}

      {/* 3. FILTER TABS & SEARCH BAR */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 rounded-lg border bg-card p-3 shadow-2xs">
        <div className="flex flex-wrap items-center gap-1.5">
          <Button
            variant={filterMode === "ALL" ? "default" : "outline"}
            size="sm"
            onClick={() => setFilterMode("ALL")}
            className="h-8 text-xs"
          >
            All ({attempts.length})
          </Button>

          <Button
            variant={filterMode === "IN_PROGRESS" ? "default" : "outline"}
            size="sm"
            onClick={() => setFilterMode("IN_PROGRESS")}
            className="h-8 text-xs gap-1.5"
          >
            <span className="size-1.5 rounded-full bg-emerald-500 animate-pulse" />
            <span>Taking Now ({liveActiveAttempts.length})</span>
          </Button>

          <Button
            variant={filterMode === "FLAGGED" ? "destructive" : "outline"}
            size="sm"
            onClick={() => setFilterMode("FLAGGED")}
            className={cn(
              "h-8 text-xs gap-1.5",
              filterMode !== "FLAGGED" && flaggedAttempts.length > 0 && "text-rose-500 border-rose-500/30"
            )}
          >
            <ShieldAlert className="size-3" />
            <span>Flagged ({flaggedAttempts.length})</span>
          </Button>

          <Button
            variant={filterMode === "SUBMITTED" ? "default" : "outline"}
            size="sm"
            onClick={() => setFilterMode("SUBMITTED")}
            className="h-8 text-xs"
          >
            Submitted ({attempts.filter((a) => a.status === "SUBMITTED").length})
          </Button>

          <Button
            variant={filterMode === "EXPIRED" ? "default" : "outline"}
            size="sm"
            onClick={() => setFilterMode("EXPIRED")}
            className="h-8 text-xs"
          >
            Expired ({attempts.filter((a) => a.status === "EXPIRED").length})
          </Button>
        </div>

        <div className="relative w-full sm:w-60">
          <Search className="absolute top-1/2 left-2.5 size-3.5 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder="Search candidate email..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="h-8 pl-8 text-xs"
          />
        </div>
      </div>

      {/* 4. ATTEMPTS ROSTER TABLE */}
      <div className="rounded-xl border bg-card overflow-hidden shadow-xs">
        <Table>
          <TableHeader>
            <TableRow className="bg-muted/40 hover:bg-muted/40">
              <TableHead className="w-[240px]">Candidate Email</TableHead>
              <TableHead className="w-[100px]">Attempt #</TableHead>
              <TableHead className="w-[130px]">Status</TableHead>
              <TableHead className="w-[140px]">Started At</TableHead>
              <TableHead className="w-[160px]">Deadline / Ended</TableHead>
              <TableHead className="w-[140px]">Score Verdict</TableHead>
              <TableHead className="w-[150px]">Integrity Audit</TableHead>
              <TableHead className="w-[150px] text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {loadingAttempts ? (
              <TableRow>
                <TableCell colSpan={8} className="h-40 text-center">
                  <PageLoadingState message="Streaming attempt telemetry & results..." />
                </TableCell>
              </TableRow>
            ) : displayedAttempts.length === 0 ? (
              <TableRow>
                <TableCell colSpan={8} className="h-40 p-0 text-center">
                  <TableEmptyState
                    title="No Sessions Match Filter"
                    description={
                      searchQuery || filterMode !== "ALL"
                        ? "No candidate attempts match your search or status selection."
                        : "No candidate attempts recorded yet."
                    }
                  />
                </TableCell>
              </TableRow>
            ) : (
              displayedAttempts.map((att) => (
                <TableRow key={att.id} className="transition-colors hover:bg-muted/30">
                  <TableCell className="font-mono text-xs font-semibold text-foreground">
                    {att.candidateEmail}
                  </TableCell>
                  <TableCell>
                    <Badge variant="outline" className="text-[10px] font-mono">
                      #{att.attemptNumber}
                    </Badge>
                  </TableCell>
                  <TableCell>{getAttemptBadge(att.status)}</TableCell>
                  <TableCell className="text-xs text-muted-foreground font-mono">
                    {new Date(att.startedAt).toLocaleTimeString([], {
                      hour: "2-digit",
                      minute: "2-digit",
                      second: "2-digit",
                    })}
                  </TableCell>
                  <TableCell className="text-xs text-muted-foreground font-mono">
                    {att.status === "IN_PROGRESS" ? (
                      <span className="text-amber-600 dark:text-amber-400 font-medium">
                        {formatRemainingMinutes(att.effectiveDeadline, currentTime)}
                      </span>
                    ) : att.submittedAt ? (
                      new Date(att.submittedAt).toLocaleTimeString([], {
                        hour: "2-digit",
                        minute: "2-digit",
                        second: "2-digit",
                      })
                    ) : (
                      new Date(att.effectiveDeadline).toLocaleTimeString([], {
                        hour: "2-digit",
                        minute: "2-digit",
                      })
                    )}
                  </TableCell>
                  <TableCell>
                    {att.score !== undefined && att.score !== null ? (
                      <div className="flex items-center gap-1.5">
                        <span className="font-bold text-xs font-mono">
                          {att.score} / {att.maxPossibleScore ?? totalScore}
                        </span>
                        {att.passed !== undefined && (
                          <Badge
                            className={cn(
                              "text-[9px] px-1.5 py-0 font-semibold",
                              att.passed
                                ? "bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border-emerald-500/30"
                                : "bg-destructive/15 text-destructive border-destructive/30"
                            )}
                          >
                            {att.passed ? "PASSED" : "FAILED"}
                          </Badge>
                        )}
                      </div>
                    ) : (
                      <span className="text-muted-foreground text-xs font-mono">In evaluation...</span>
                    )}
                  </TableCell>
                  <TableCell>
                    {att.integrityFlagsCount > 0 ? (
                      <Badge variant="destructive" className="gap-1 text-[11px] font-semibold cursor-pointer" onClick={() => onAuditAttempt(att.id)}>
                        <ShieldAlert className="size-3" />
                        <span>{att.integrityFlagsCount} flag(s)</span>
                      </Badge>
                    ) : (
                      <Badge
                        variant="secondary"
                        className="gap-1 text-[10px] text-muted-foreground"
                      >
                        <ShieldCheck className="size-2.5 text-emerald-500" />
                        <span>Clean</span>
                      </Badge>
                    )}
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex items-center justify-end gap-1.5">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => onInspectAttempt(att.id)}
                        className="h-7 text-xs gap-1 font-medium"
                        title="Inspect Code, Submissions & MCQ Answers"
                      >
                        <Eye className="size-3" />
                        <span>Inspect</span>
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => onAuditAttempt(att.id)}
                        className="h-7 text-xs gap-1 text-muted-foreground hover:text-foreground"
                        title="View Detailed Telemetry & Presence Events"
                      >
                        <Activity className="size-3" />
                        <span>Audit</span>
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>

        {totalPages > 1 && (
          <TablePagination
            page={page}
            totalPages={totalPages}
            totalElements={totalElements}
            pageSize={pageSize}
            onPageChange={onPageChange}
            onPageSizeChange={onPageSizeChange}
          />
        )}
      </div>
    </div>
  )
}
