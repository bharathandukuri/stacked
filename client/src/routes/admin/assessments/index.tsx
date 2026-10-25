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
  AlertCircle,
  Eye,
  Clock,
  Users,
  KeyRound,
  Copy,
  Check,
  Share2,
  Play,
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
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select"
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

  // Auth Guard
  useEffect(() => {
    if (!isInitialized) {
      initializeAuthSession()
    }
  }, [isInitialized])

  useEffect(() => {
    if (isInitialized && !isAuthenticated) {
      navigate({ to: "/admin/login" })
    }
  }, [isInitialized, isAuthenticated, navigate])

  // Filters & Pagination State
  const [searchInput, setSearchInput] = useState("")
  const deferredSearch = useDeferredValue(searchInput)
  const [statusFilter, setStatusFilter] = useState<AssessmentStatus | "">("")
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)

  const [copiedId, setCopiedId] = useState<string | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<AssessmentSummaryResponse | null>(null)
  const [completeTarget, setCompleteTarget] = useState<AssessmentSummaryResponse | null>(null)
  const [activateTarget, setActivateTarget] = useState<AssessmentSummaryResponse | null>(null)

  const handleSearchChange = (val: string) => {
    setSearchInput(val)
    setPage(0)
  }

  const handleStatusFilterChange = (val: AssessmentStatus | "") => {
    setStatusFilter(val)
    setPage(0)
  }

  const handleResetFilters = () => {
    setSearchInput("")
    setStatusFilter("")
    setPage(0)
  }

  // Data Queries & Mutations
  const { data, isLoading, isFetching, isError, error, refetch } = useAssessments({
    search: deferredSearch || undefined,
    status: statusFilter || undefined,
    page,
    size,
  })

  const deleteMutation = useDeleteAssessment()
  const completeMutation = useCompleteAssessment()
  const activateMutation = useActivateAssessment()

  const assessments = data?.items ?? []
  const totalElements = data?.totalElements ?? 0
  const totalPages = data?.totalPages ?? 0
  const hasActiveFilters = Boolean(searchInput || statusFilter)

  const handleDelete = () => {
    if (deleteTarget) {
      deleteMutation.mutate(deleteTarget.id, {
        onSuccess: () => setDeleteTarget(null),
      })
    }
  }

  const handleActivate = () => {
    if (activateTarget) {
      activateMutation.mutate(activateTarget.id, {
        onSuccess: () => setActivateTarget(null),
      })
    }
  }

  const handleComplete = () => {
    if (completeTarget) {
      completeMutation.mutate(completeTarget.id, {
        onSuccess: () => setCompleteTarget(null),
      })
    }
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
          <Badge className="bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20 text-xs">
            Active
          </Badge>
        )
      case "COMPLETED":
        return (
          <Badge className="bg-purple-500/10 text-purple-600 dark:text-purple-400 border-purple-500/20 text-xs">
            Completed
          </Badge>
        )
      case "NOT_STARTED":
      default:
        return (
          <Badge variant="secondary" className="text-xs">
            Not Started
          </Badge>
        )
    }
  }

  return (
    <div className="flex h-screen w-full overflow-hidden bg-background">
      <AdminSidebar currentPath="/admin/assessments" />

      <main className="flex flex-1 flex-col min-w-0 overflow-y-auto">
        {/* Header */}
        <header className="sticky top-0 z-10 flex h-16 items-center justify-between border-b border-border bg-card/70 px-8 backdrop-blur-md">
          <div className="flex items-center gap-3">
            <div className="flex size-9 items-center justify-center rounded-lg border border-primary/20 bg-primary/10 text-primary">
              <GraduationCap className="size-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-lg font-bold tracking-tight text-foreground">
                  Assessments
                </h1>
                <Badge variant="secondary" className="px-1.5 py-0 text-xs">
                  {totalElements}
                </Badge>
              </div>
              <p className="text-xs text-muted-foreground">
                Create, configure, and monitor candidate assessment sessions.
              </p>
            </div>
          </div>

          <Link to="/admin/assessments/studio/new">
            <Button size="sm" className="gap-2 shadow-sm text-xs">
              <Plus className="size-4" />
              <span>New Assessment</span>
            </Button>
          </Link>
        </header>

        {/* Filters Bar */}
        <div className="border-b border-border bg-muted/20 px-8 py-4">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            {/* Search Input */}
            <div className="relative flex-1 sm:max-w-md">
              <Search className="absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder="Search assessments..."
                value={searchInput}
                onChange={(e) => handleSearchChange(e.target.value)}
                className="pl-9 text-sm"
              />
            </div>

            {/* Status Dropdown */}
            <div className="flex flex-wrap items-center gap-2">
              <NativeSelect
                value={statusFilter}
                onChange={(e) => handleStatusFilterChange(e.target.value as AssessmentStatus | "")}
                className="w-36 text-xs"
              >
                <NativeSelectOption value="">All Statuses</NativeSelectOption>
                <NativeSelectOption value="NOT_STARTED">Not Started</NativeSelectOption>
                <NativeSelectOption value="ACTIVE">Active</NativeSelectOption>
                <NativeSelectOption value="COMPLETED">Completed</NativeSelectOption>
              </NativeSelect>

              {hasActiveFilters && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={handleResetFilters}
                  className="gap-1.5 text-xs text-muted-foreground hover:text-foreground"
                >
                  <RotateCcw className="size-3.5" />
                  <span>Reset</span>
                </Button>
              )}
            </div>
          </div>
        </div>

        {/* KPI Summary Cards */}
        <div className="grid grid-cols-2 gap-4 px-8 pt-6 sm:grid-cols-4">
          <div className="rounded-xl border border-border/80 bg-card/60 p-4 shadow-2xs">
            <div className="flex items-center justify-between text-xs text-muted-foreground">
              <span>Total Assessments</span>
              <GraduationCap className="size-4 text-primary" />
            </div>
            <div className="mt-2 text-2xl font-bold tracking-tight text-foreground">
              {totalElements}
            </div>
          </div>

          <div className="rounded-xl border border-border/80 bg-card/60 p-4 shadow-2xs">
            <div className="flex items-center justify-between text-xs text-muted-foreground">
              <span>Active Sessions</span>
              <Play className="size-4 text-emerald-500 fill-emerald-500" />
            </div>
            <div className="mt-2 text-2xl font-bold tracking-tight text-emerald-600 dark:text-emerald-400">
              {assessments.filter((a) => a.status === "ACTIVE").length}
            </div>
          </div>

          <div className="rounded-xl border border-border/80 bg-card/60 p-4 shadow-2xs">
            <div className="flex items-center justify-between text-xs text-muted-foreground">
              <span>Completed</span>
              <CheckCircle2 className="size-4 text-blue-500" />
            </div>
            <div className="mt-2 text-2xl font-bold tracking-tight text-foreground">
              {assessments.filter((a) => a.status === "COMPLETED").length}
            </div>
          </div>

          <div className="rounded-xl border border-border/80 bg-card/60 p-4 shadow-2xs">
            <div className="flex items-center justify-between text-xs text-muted-foreground">
              <span>Candidates Enrolled</span>
              <Users className="size-4 text-purple-500" />
            </div>
            <div className="mt-2 text-2xl font-bold tracking-tight text-foreground">
              {assessments.reduce((acc, a) => acc + (a.assignedCandidateCount || 0), 0)}
            </div>
          </div>
        </div>

        {/* Content Area */}
        <div className="flex-1 p-8">
          {isLoading ? (
            <div className="flex h-64 flex-col items-center justify-center gap-3 text-muted-foreground">
              <Loader2 className="size-8 animate-spin text-primary" />
              <p className="text-sm font-medium">Loading assessments...</p>
            </div>
          ) : isError ? (
            <div className="flex h-64 flex-col items-center justify-center gap-3 text-destructive">
              <AlertCircle className="size-8" />
              <p className="text-sm font-medium">
                Failed to load assessments: {error instanceof Error ? error.message : "Unknown error"}
              </p>
              <Button variant="outline" size="sm" onClick={() => refetch()}>
                Retry
              </Button>
            </div>
          ) : assessments.length === 0 ? (
            <div className="flex h-64 flex-col items-center justify-center gap-3 rounded-xl border border-dashed border-border bg-card/40 p-8 text-center">
              <div className="flex size-12 items-center justify-center rounded-full bg-muted text-muted-foreground">
                <GraduationCap className="size-6" />
              </div>
              <h3 className="text-base font-semibold text-foreground">No assessments found</h3>
              <p className="max-w-sm text-xs text-muted-foreground">
                {hasActiveFilters
                  ? "No assessments match your active filters. Try adjusting or clearing your search."
                  : "No assessments created yet. Click 'New Assessment' to get started."}
              </p>
              {hasActiveFilters ? (
                <Button variant="outline" size="sm" onClick={handleResetFilters}>
                  Clear Filters
                </Button>
              ) : (
                <Link to="/admin/assessments/studio/new">
                  <Button size="sm" className="gap-2 text-xs">
                    <Plus className="size-4" />
                    <span>Create First Assessment</span>
                  </Button>
                </Link>
              )}
            </div>
          ) : (
            <div className="rounded-xl border border-border bg-card shadow-sm">
              <Table>
                <TableHeader>
                  <TableRow className="hover:bg-transparent">
                    <TableHead className="w-[320px]">Assessment</TableHead>
                    <TableHead className="w-[120px]">Status</TableHead>
                    <TableHead className="w-[200px]">Questions</TableHead>
                    <TableHead className="w-[180px]">Duration</TableHead>
                    <TableHead className="w-[140px]">Candidates</TableHead>
                    <TableHead className="w-[80px] text-right">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {assessments.map((item) => (
                    <TableRow key={item.id} className="transition-colors hover:bg-muted/40">
                      {/* Title & Invitation Code */}
                      <TableCell>
                        <div className="flex flex-col overflow-hidden">
                          <Link
                            to="/admin/assessments/$assessmentId"
                            params={{ assessmentId: item.id }}
                            className="font-medium text-sm text-foreground hover:text-primary transition-colors truncate"
                          >
                            {item.title}
                          </Link>
                          {item.invitationCode && item.invitationCodeEnabled && (
                            <div className="flex items-center gap-1.5 mt-1">
                              <span className="font-mono text-[11px] font-semibold text-primary bg-primary/10 px-1.5 py-0.5 rounded border border-primary/20">
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
                        </div>
                      </TableCell>

                      {/* Status */}
                      <TableCell>{getStatusBadge(item.status)}</TableCell>

                      {/* Questions */}
                      <TableCell>
                        <div className="text-xs font-medium text-foreground">
                          {item.totalQuestions} Questions ({item.totalScore} pts)
                        </div>
                        <div className="text-[11px] text-muted-foreground mt-0.5">
                          {item.mcqCount} MCQ • {item.codingCount} Coding
                        </div>
                      </TableCell>

                      {/* Duration */}
                      <TableCell>
                        <div className="flex items-center gap-1 text-xs font-medium text-foreground">
                          <Clock className="size-3 text-muted-foreground" />
                          <span>{item.durationMinutes} mins</span>
                          {item.maxAttempts ? (
                            <Badge variant="outline" className="text-[10px] py-0 px-1 ml-1">
                              Max {item.maxAttempts}
                            </Badge>
                          ) : null}
                        </div>
                        <div className="text-[11px] text-muted-foreground mt-0.5">
                          {item.scheduledStart ? (
                            <span>{new Date(item.scheduledStart).toLocaleDateString()}</span>
                          ) : (
                            "Flexible schedule"
                          )}
                        </div>
                      </TableCell>

                      {/* Candidates */}
                      <TableCell>
                        <div className="flex items-center gap-1.5 text-xs text-foreground">
                          <Users className="size-3 text-muted-foreground" />
                          <span className="font-medium">{item.assignedCandidateCount}</span>
                          <span className="text-muted-foreground">assigned</span>
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
                                aria-label="Open actions"
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

              {/* Pagination Footer */}
              <div className="flex flex-col items-center justify-between gap-3 border-t border-border px-6 py-4 sm:flex-row">
                <div className="flex items-center gap-2 text-xs text-muted-foreground">
                  <span>Rows per page:</span>
                  <NativeSelect
                    value={size}
                    onChange={(e) => {
                      setSize(Number(e.target.value))
                      setPage(0)
                    }}
                    className="h-8 w-18 text-xs"
                  >
                    <NativeSelectOption value="10">10</NativeSelectOption>
                    <NativeSelectOption value="20">20</NativeSelectOption>
                    <NativeSelectOption value="50">50</NativeSelectOption>
                  </NativeSelect>
                  <span className="ml-2">
                    Showing {totalElements === 0 ? 0 : page * size + 1}–
                    {Math.min((page + 1) * size, totalElements)} of {totalElements}
                  </span>
                </div>

                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page === 0 || isFetching}
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    className="h-8 text-xs"
                  >
                    Previous
                  </Button>
                  <span className="px-2 text-xs font-medium text-muted-foreground">
                    Page {page + 1} of {Math.max(1, totalPages)}
                  </span>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page >= totalPages - 1 || isFetching}
                    onClick={() => setPage((p) => p + 1)}
                    className="h-8 text-xs"
                  >
                    Next
                  </Button>
                </div>
              </div>
            </div>
          )}
        </div>
      </main>

      {/* Delete Confirmation Alert */}
      <AlertDialog open={Boolean(deleteTarget)} onOpenChange={(open) => !open && setDeleteTarget(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle className="text-base font-semibold">Delete Assessment?</AlertDialogTitle>
            <AlertDialogDescription className="text-xs">
              This will permanently delete &ldquo;{deleteTarget?.title}&rdquo; and all associated candidate assignments and attempts. This action cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel className="text-xs">Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDelete}
              className="bg-destructive hover:bg-destructive/90 text-destructive-foreground text-xs"
            >
              Delete
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Complete Confirmation Alert */}
      <AlertDialog open={Boolean(completeTarget)} onOpenChange={(open) => !open && setCompleteTarget(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle className="text-base font-semibold">Complete Assessment?</AlertDialogTitle>
            <AlertDialogDescription className="text-xs">
              This will officially close &ldquo;{completeTarget?.title}&rdquo; immediately. Candidate enrollment and attempts will be closed, and effective status will become COMPLETED.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel className="text-xs">Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleComplete}
              className="bg-purple-600 hover:bg-purple-700 text-white text-xs"
            >
              {completeMutation.isPending ? <Loader2 className="size-3.5 animate-spin mr-1" /> : null}
              Complete Assessment
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Activate Confirmation Alert */}
      <AlertDialog open={Boolean(activateTarget)} onOpenChange={(open) => !open && setActivateTarget(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle className="text-base font-semibold">Activate Assessment?</AlertDialogTitle>
            <AlertDialogDescription className="text-xs">
              This will manually activate &ldquo;{activateTarget?.title}&rdquo; immediately, overriding any scheduled start time. The candidate roster will be frozen to prevent new enrollments, and candidates will be able to take the assessment.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel className="text-xs">Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleActivate}
              className="bg-emerald-600 hover:bg-emerald-700 text-white text-xs"
            >
              {activateMutation.isPending ? <Loader2 className="size-3.5 animate-spin mr-1" /> : null}
              Activate Assessment
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
