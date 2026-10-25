import { useEffect, useState, useDeferredValue } from "react"
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router"
import {
  GraduationCap,
  Plus,
  Search,
  RotateCcw,
  Loader2,
  MoreVertical,
  Trash2,
  CheckCircle2,
  Eye,
  Clock,
  Users,
  KeyRound,
  Copy,
  Check,
  Share2,
  Play,
  Calendar,
  AlertCircle,
  CheckSquare,
  Code2,
} from "lucide-react"

import { AdminSidebar } from "@/components/admin/sidebar"
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
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select"
import { ConfirmActionDialog } from "@/components/common/confirm-action-dialog"
import { toast } from "@/components/ui/toast"
import {
  useIsAuthenticated,
  useIsInitialized,
} from "@/stores/auth-store"
import { initializeAuthSession } from "@/hooks/use-auth"
import {
  useAssessments,
  useDeleteAssessment,
  useActivateAssessment,
  useCompleteAssessment,
} from "@/hooks/use-assessments"
import type { AssessmentStatus, AssessmentSummaryResponse } from "@/types/assessment"

export const Route = createFileRoute("/admin/assessments/")({
  component: AssessmentsPage,
})

export function AssessmentsPage() {
  const navigate = useNavigate()
  const isAuthenticated = useIsAuthenticated()
  const isInitialized = useIsInitialized()

  // Filters & Pagination
  const [searchInput, setSearchInput] = useState("")
  const deferredSearch = useDeferredValue(searchInput)
  const [statusFilter, setStatusFilter] = useState<string>("ALL")
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)

  // Copy states
  const [copiedId, setCopiedId] = useState<string | null>(null)

  // Action Dialog Targets
  const [deleteTarget, setDeleteTarget] = useState<AssessmentSummaryResponse | null>(null)
  const [completeTarget, setCompleteTarget] = useState<AssessmentSummaryResponse | null>(null)
  const [activateTarget, setActivateTarget] = useState<AssessmentSummaryResponse | null>(null)

  // Auth gate
  useEffect(() => {
    let isMounted = true

    async function verifySession() {
      if (!isInitialized) {
        const hasSession = await initializeAuthSession()
        if (isMounted && !hasSession) {
          navigate({ to: "/admin/login", replace: true })
        }
      } else if (!isAuthenticated) {
        navigate({ to: "/admin/login", replace: true })
      }
    }

    verifySession()

    return () => {
      isMounted = false
    }
  }, [isAuthenticated, isInitialized, navigate])

  // Data Queries
  const parsedStatus =
    statusFilter === "ALL" ? undefined : (statusFilter as AssessmentStatus)

  const { data, isLoading, isError, error, refetch } = useAssessments({
    search: deferredSearch || undefined,
    status: parsedStatus,
    page,
    size: pageSize,
  })

  const deleteMutation = useDeleteAssessment()
  const completeMutation = useCompleteAssessment()
  const activateMutation = useActivateAssessment()

  const assessments = data?.items ?? []
  const totalElements = data?.totalElements ?? 0
  const totalPages = data?.totalPages ?? 0
  const isFirst = page === 0
  const isLast = page >= totalPages - 1 || totalPages === 0

  const hasActiveFilters =
    searchInput.trim().length > 0 || statusFilter !== "ALL"

  const handleResetFilters = () => {
    setSearchInput("")
    setStatusFilter("ALL")
    setPage(0)
  }

  const handleCopyCode = (code: string, id: string) => {
    navigator.clipboard.writeText(code)
    setCopiedId(`code-${id}`)
    setTimeout(() => setCopiedId(null), 2000)
    toast.add({
      title: "Code Copied",
      description: `Invitation code ${code} copied to clipboard!`,
      type: "success",
    })
  }

  const handleCopyJoinLink = (code: string, id: string) => {
    const url = `${window.location.origin}/assessments/join?code=${encodeURIComponent(code)}`
    navigator.clipboard.writeText(url)
    setCopiedId(`link-${id}`)
    setTimeout(() => setCopiedId(null), 2000)
    toast.add({
      title: "Join Link Copied",
      description: "Candidate invitation link copied to clipboard!",
      type: "success",
    })
  }

  const getStatusBadge = (status: AssessmentStatus) => {
    switch (status) {
      case "ACTIVE":
        return (
          <Badge className="bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20 text-xs gap-1 font-medium">
            <span className="size-1.5 rounded-full bg-emerald-500 animate-pulse" />
            <span>Active</span>
          </Badge>
        )
      case "COMPLETED":
        return (
          <Badge className="bg-purple-500/10 text-purple-600 dark:text-purple-400 border-purple-500/20 text-xs font-medium">
            Completed
          </Badge>
        )
      case "NOT_STARTED":
      default:
        return (
          <Badge variant="secondary" className="text-xs font-normal">
            Scheduled
          </Badge>
        )
    }
  }

  const activeCount = assessments.filter((a) => a.status === "ACTIVE").length
  const completedCount = assessments.filter((a) => a.status === "COMPLETED").length
  const totalAssignedCandidates = assessments.reduce(
    (acc, a) => acc + (a.assignedCandidateCount || 0),
    0
  )

  if (!isInitialized) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-3 bg-background">
        <Loader2 className="size-8 animate-spin text-primary" />
        <span className="text-sm font-medium text-muted-foreground">
          Verifying administrator session...
        </span>
      </div>
    )
  }

  if (!isAuthenticated) {
    return null
  }

  return (
    <div className="flex h-screen w-full overflow-hidden bg-background text-foreground">
      {/* Sidebar */}
      <AdminSidebar currentPath="/admin/assessments" />

      {/* Main Content Area */}
      <main className="flex-1 min-w-0 overflow-y-auto">
        {/* Top Header Bar */}
        <header className="sticky top-0 z-10 flex h-16 items-center justify-between border-b border-border bg-background/80 px-8 backdrop-blur-md">
          <div className="flex items-center gap-3">
            <div className="flex size-9 items-center justify-center rounded-lg border border-primary/20 bg-primary/10 text-primary">
              <GraduationCap className="size-5" />
            </div>
            <div>
              <h1 className="text-lg font-semibold tracking-tight text-foreground">
                Assessments
              </h1>
              <p className="text-xs text-muted-foreground">
                Create, configure, and monitor candidate assessment sessions
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <Link to="/admin/assessments/studio/new">
              <Button size="sm" className="gap-2 shadow-xs">
                <Plus className="size-4" />
                <span>Create Assessment</span>
              </Button>
            </Link>
          </div>
        </header>

        {/* Page Body */}
        <div className="space-y-6 p-8">
          {/* Stats Bar */}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <div className="flex items-center gap-4 rounded-xl border border-border/60 bg-card p-4">
              <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
                <GraduationCap className="size-5" />
              </div>
              <div>
                <p className="text-xs font-medium text-muted-foreground">
                  Total Assessments
                </p>
                <p className="text-2xl font-bold tracking-tight text-foreground">
                  {totalElements}
                </p>
              </div>
            </div>

            <div className="flex items-center gap-4 rounded-xl border border-border/60 bg-card p-4">
              <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-emerald-500/10 text-emerald-600 dark:text-emerald-400">
                <Play className="size-5" />
              </div>
              <div>
                <p className="text-xs font-medium text-muted-foreground">
                  Active Sessions
                </p>
                <p className="text-2xl font-bold tracking-tight text-emerald-600 dark:text-emerald-400">
                  {activeCount}
                </p>
              </div>
            </div>

            <div className="flex items-center gap-4 rounded-xl border border-border/60 bg-card p-4">
              <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-indigo-500/10 text-indigo-600 dark:text-indigo-400">
                <CheckCircle2 className="size-5" />
              </div>
              <div>
                <p className="text-xs font-medium text-muted-foreground">
                  Completed
                </p>
                <p className="text-2xl font-bold tracking-tight text-foreground">
                  {completedCount}
                </p>
              </div>
            </div>

            <div className="flex items-center gap-4 rounded-xl border border-border/60 bg-card p-4">
              <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-amber-500/10 text-amber-600 dark:text-amber-400">
                <Users className="size-5" />
              </div>
              <div>
                <p className="text-xs font-medium text-muted-foreground">
                  Candidates Enrolled
                </p>
                <p className="text-2xl font-bold tracking-tight text-foreground">
                  {totalAssignedCandidates}
                </p>
              </div>
            </div>
          </div>

          {/* Filter & Search Toolbar */}
          <div className="flex flex-col gap-4 rounded-xl border border-border bg-card/60 p-4 shadow-2xs backdrop-blur-xs md:flex-row md:items-center md:justify-between">
            {/* Search Input */}
            <div className="relative flex-1 md:max-w-md">
              <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                value={searchInput}
                onChange={(e) => {
                  setSearchInput(e.target.value)
                  setPage(0)
                }}
                placeholder="Search by title or description..."
                className="pl-9 text-sm"
              />
              {searchInput && (
                <button
                  type="button"
                  onClick={() => {
                    setSearchInput("")
                    setPage(0)
                  }}
                  className="absolute top-1/2 right-3 -translate-y-1/2 text-xs text-muted-foreground hover:text-foreground cursor-pointer"
                >
                  ✕
                </button>
              )}
            </div>

            {/* Dropdown Filters */}
            <div className="flex flex-wrap items-center gap-2.5">
              <div className="flex items-center gap-1.5">
                <span className="text-xs font-medium text-muted-foreground">
                  Status:
                </span>
                <NativeSelect
                  size="sm"
                  value={statusFilter}
                  onChange={(e) => {
                    setStatusFilter(e.target.value)
                    setPage(0)
                  }}
                >
                  <NativeSelectOption value="ALL">All Status</NativeSelectOption>
                  <NativeSelectOption value="ACTIVE">Active</NativeSelectOption>
                  <NativeSelectOption value="NOT_STARTED">Scheduled</NativeSelectOption>
                  <NativeSelectOption value="COMPLETED">Completed</NativeSelectOption>
                </NativeSelect>
              </div>

              {hasActiveFilters && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={handleResetFilters}
                  className="h-8 gap-1.5 px-2 text-xs text-muted-foreground hover:text-foreground"
                >
                  <RotateCcw className="size-3" />
                  <span>Reset</span>
                </Button>
              )}
            </div>
          </div>

          {/* Assessments Table Card */}
          <div className="rounded-xl border border-border bg-card shadow-xs overflow-hidden">
            {isLoading ? (
              <div className="flex flex-col items-center justify-center py-24 text-muted-foreground">
                <Loader2 className="size-8 animate-spin text-primary mb-3" />
                <span className="text-sm font-medium">
                  Loading assessments...
                </span>
              </div>
            ) : isError ? (
              <div className="flex flex-col items-center justify-center py-20 text-center">
                <AlertCircle className="size-10 text-destructive mb-2" />
                <h3 className="text-base font-semibold text-foreground">
                  Failed to load assessments
                </h3>
                <p className="text-sm text-muted-foreground max-w-sm mt-1 mb-4">
                  {error instanceof Error
                    ? error.message
                    : "Could not retrieve assessment sessions from the backend."}
                </p>
                <Button variant="outline" size="sm" onClick={() => refetch()}>
                  Retry
                </Button>
              </div>
            ) : assessments.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-20 px-4 text-center">
                <div className="flex size-14 items-center justify-center rounded-2xl bg-muted/60 text-muted-foreground mb-4">
                  <GraduationCap className="size-7" />
                </div>
                <h3 className="text-base font-semibold text-foreground">
                  {hasActiveFilters
                    ? "No assessments found"
                    : "No assessments created yet"}
                </h3>
                <p className="text-sm text-muted-foreground max-w-sm mt-1 mb-5">
                  {hasActiveFilters
                    ? "No assessments match your current search and filter criteria. Try resetting filters."
                    : "Your assessment catalog is empty. Create structured assessments with MCQs and coding challenges."}
                </p>
                {hasActiveFilters ? (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={handleResetFilters}
                    className="gap-2"
                  >
                    <RotateCcw className="size-3.5" />
                    <span>Clear Filters</span>
                  </Button>
                ) : (
                  <Link to="/admin/assessments/studio/new">
                    <Button size="sm" className="gap-2">
                      <Plus className="size-4" />
                      <span>Create Your First Assessment</span>
                    </Button>
                  </Link>
                )}
              </div>
            ) : (
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow className="hover:bg-transparent">
                      <TableHead className="w-[34%]">Assessment</TableHead>
                      <TableHead className="w-[12%]">Status</TableHead>
                      <TableHead className="w-[18%]">Composition</TableHead>
                      <TableHead className="w-[16%]">Duration & Schedule</TableHead>
                      <TableHead className="w-[10%]">Candidates</TableHead>
                      <TableHead className="w-[10%] text-right">Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {assessments.map((item) => (
                      <TableRow
                        key={item.id}
                        className="group transition-colors hover:bg-muted/40"
                      >
                        {/* Title & Invitation Code */}
                        <TableCell>
                          <div className="flex flex-col gap-1 min-w-0 pr-2">
                            <Link
                              to="/admin/assessments/$assessmentId"
                              params={{ assessmentId: item.id }}
                              className="text-sm font-semibold text-foreground line-clamp-1 hover:text-primary transition-colors cursor-pointer"
                            >
                              {item.title}
                            </Link>

                            <div className="flex items-center gap-2">
                              {item.invitationCode && item.invitationCodeEnabled && (
                                <div className="inline-flex items-center gap-1">
                                  <span className="font-mono text-[10px] font-bold text-primary bg-primary/10 px-1.5 py-0.2 rounded border border-primary/25">
                                    {item.invitationCode}
                                  </span>
                                  <button
                                    type="button"
                                    onClick={() => handleCopyCode(item.invitationCode!, item.id)}
                                    className="text-muted-foreground hover:text-foreground p-0.5 transition-colors cursor-pointer"
                                    title="Copy invitation code"
                                  >
                                    {copiedId === `code-${item.id}` ? (
                                      <Check className="size-3 text-emerald-500" />
                                    ) : (
                                      <Copy className="size-3" />
                                    )}
                                  </button>
                                </div>
                              )}

                              <span className="text-[11px] font-mono text-muted-foreground truncate">
                                /{item.slug}
                              </span>
                            </div>
                          </div>
                        </TableCell>

                        {/* Status */}
                        <TableCell>{getStatusBadge(item.status)}</TableCell>

                        {/* Composition */}
                        <TableCell>
                          <div className="flex flex-col gap-0.5">
                            <span className="text-xs font-medium text-foreground">
                              {item.totalQuestions} questions ({item.totalScore} pts)
                            </span>
                            <div className="flex items-center gap-2 text-[11px] text-muted-foreground">
                              <span className="flex items-center gap-1">
                                <CheckSquare className="size-3 text-purple-500" />
                                {item.mcqCount} MCQ
                              </span>
                              <span>•</span>
                              <span className="flex items-center gap-1">
                                <Code2 className="size-3 text-blue-500" />
                                {item.codingCount} Coding
                              </span>
                            </div>
                          </div>
                        </TableCell>

                        {/* Duration & Schedule */}
                        <TableCell>
                          <div className="flex flex-col gap-0.5">
                            <div className="flex items-center gap-1 text-xs text-foreground font-medium">
                              <Clock className="size-3 text-muted-foreground" />
                              <span>{item.durationMinutes} mins</span>
                              {item.maxAttempts ? (
                                <Badge variant="outline" className="text-[10px] px-1 py-0 ml-1">
                                  Max {item.maxAttempts}
                                </Badge>
                              ) : null}
                            </div>
                            <div className="flex items-center gap-1 text-[11px] text-muted-foreground">
                              <Calendar className="size-3 text-muted-foreground/70" />
                              <span>
                                {item.scheduledStart
                                  ? new Date(item.scheduledStart).toLocaleDateString()
                                  : "Flexible Schedule"}
                              </span>
                            </div>
                          </div>
                        </TableCell>

                        {/* Candidates */}
                        <TableCell>
                          <div className="flex items-center gap-1.5 text-xs text-foreground">
                            <Users className="size-3.5 text-muted-foreground" />
                            <span className="font-semibold">{item.assignedCandidateCount}</span>
                            <span className="text-muted-foreground text-[11px]">assigned</span>
                          </div>
                        </TableCell>

                        {/* Actions */}
                        <TableCell className="text-right">
                          <DropdownMenu>
                            <DropdownMenuTrigger
                              render={
                                <Button
                                  variant="ghost"
                                  size="icon"
                                  className="size-8 text-muted-foreground hover:text-foreground"
                                  aria-label="Open assessment actions"
                                >
                                  <MoreVertical className="size-4" />
                                </Button>
                              }
                            />
                            <DropdownMenuContent align="end" className="w-48">
                              <DropdownMenuItem
                                onClick={() =>
                                  navigate({
                                    to: "/admin/assessments/$assessmentId",
                                    params: { assessmentId: item.id },
                                  })
                                }
                                className="gap-2 cursor-pointer text-xs"
                              >
                                <Eye className="size-3.5 text-muted-foreground" />
                                <span>View & Monitor</span>
                              </DropdownMenuItem>

                              {item.invitationCode && item.invitationCodeEnabled && (
                                <>
                                  <DropdownMenuSeparator />
                                  <DropdownMenuItem
                                    onClick={() => handleCopyCode(item.invitationCode!, item.id)}
                                    className="gap-2 cursor-pointer text-xs"
                                  >
                                    <KeyRound className="size-3.5 text-muted-foreground" />
                                    <span>Copy Invitation Code</span>
                                  </DropdownMenuItem>
                                  <DropdownMenuItem
                                    onClick={() => handleCopyJoinLink(item.invitationCode!, item.id)}
                                    className="gap-2 cursor-pointer text-xs"
                                  >
                                    <Share2 className="size-3.5 text-muted-foreground" />
                                    <span>Copy Join Link</span>
                                  </DropdownMenuItem>
                                </>
                              )}

                              <DropdownMenuSeparator />

                              {item.status === "NOT_STARTED" && (
                                <DropdownMenuItem
                                  onClick={() => setActivateTarget(item)}
                                  className="gap-2 cursor-pointer text-xs text-emerald-600 focus:text-emerald-600"
                                >
                                  <Play className="size-3.5" />
                                  <span>Activate Assessment</span>
                                </DropdownMenuItem>
                              )}

                              {item.status !== "COMPLETED" && (
                                <DropdownMenuItem
                                  onClick={() => setCompleteTarget(item)}
                                  className="gap-2 cursor-pointer text-xs text-purple-600 focus:text-purple-600"
                                >
                                  <CheckCircle2 className="size-3.5" />
                                  <span>Complete Assessment</span>
                                </DropdownMenuItem>
                              )}

                              <DropdownMenuItem
                                onClick={() => setDeleteTarget(item)}
                                className="gap-2 cursor-pointer text-xs text-destructive focus:text-destructive"
                              >
                                <Trash2 className="size-3.5" />
                                <span>Delete Assessment</span>
                              </DropdownMenuItem>
                            </DropdownMenuContent>
                          </DropdownMenu>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            )}

            {/* Pagination Controls */}
            {assessments.length > 0 && (
              <div className="flex flex-col sm:flex-row items-center justify-between border-t border-border px-4 py-3 gap-3 text-xs text-muted-foreground bg-muted/10">
                <div className="flex items-center gap-2">
                  <span>
                    Showing{" "}
                    <span className="font-semibold text-foreground">
                      {page * pageSize + 1}
                    </span>{" "}
                    to{" "}
                    <span className="font-semibold text-foreground">
                      {Math.min((page + 1) * pageSize, totalElements)}
                    </span>{" "}
                    of{" "}
                    <span className="font-semibold text-foreground">
                      {totalElements}
                    </span>{" "}
                    assessments
                  </span>
                </div>

                <div className="flex items-center gap-4">
                  <div className="flex items-center gap-1.5">
                    <span>Rows per page:</span>
                    <NativeSelect
                      size="sm"
                      value={String(pageSize)}
                      onChange={(e) => {
                        setPageSize(Number(e.target.value))
                        setPage(0)
                      }}
                      className="w-16"
                    >
                      <NativeSelectOption value="10">10</NativeSelectOption>
                      <NativeSelectOption value="20">20</NativeSelectOption>
                      <NativeSelectOption value="50">50</NativeSelectOption>
                    </NativeSelect>
                  </div>

                  <span>
                    Page {page + 1} of {Math.max(1, totalPages)}
                  </span>

                  <div className="flex items-center gap-1">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPage((p) => Math.max(0, p - 1))}
                      disabled={isFirst}
                      className="h-7 text-xs px-2"
                    >
                      Previous
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPage((p) => p + 1)}
                      disabled={isLast}
                      className="h-7 text-xs px-2"
                    >
                      Next
                    </Button>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </main>

      {/* Confirmation Modals */}
      <ConfirmActionDialog
        open={Boolean(deleteTarget)}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
        title="Delete Assessment"
        description={`Are you sure you want to delete assessment "${deleteTarget?.title}"? This permanently purges candidate assignments, recorded attempts, and audit logs.`}
        confirmLabel="Delete Permanently"
        variant="destructive"
        isPending={deleteMutation.isPending}
        onConfirm={async () => {
          if (deleteTarget) {
            await deleteMutation.mutateAsync(deleteTarget.id)
            setDeleteTarget(null)
          }
        }}
      />

      <ConfirmActionDialog
        open={Boolean(activateTarget)}
        onOpenChange={(open) => !open && setActivateTarget(null)}
        title="Activate Assessment"
        description={`Activate "${activateTarget?.title}"? Candidates will immediately be allowed to enter the assessment lobby and begin attempts.`}
        confirmLabel="Activate Now"
        isPending={activateMutation.isPending}
        onConfirm={async () => {
          if (activateTarget) {
            await activateMutation.mutateAsync(activateTarget.id)
            setActivateTarget(null)
          }
        }}
      />

      <ConfirmActionDialog
        open={Boolean(completeTarget)}
        onOpenChange={(open) => !open && setCompleteTarget(null)}
        title="Conclude Assessment"
        description={`Mark "${completeTarget?.title}" as completed? Ongoing candidate sessions will be automatically graded and finalized.`}
        confirmLabel="Conclude Now"
        isPending={completeMutation.isPending}
        onConfirm={async () => {
          if (completeTarget) {
            await completeMutation.mutateAsync(completeTarget.id)
            setCompleteTarget(null)
          }
        }}
      />
    </div>
  )
}
