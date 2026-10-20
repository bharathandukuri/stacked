import { useEffect, useState, useDeferredValue } from "react"
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router"
import {
  Code2,
  Plus,
  Search,
  CheckCircle2,
  Copy,
  RotateCcw,
  Database,
  Layers,
  AlertCircle,
  Loader2,
  Check,
  MoreVertical,
  Pencil,
  Trash2,
} from "lucide-react"

import { AdminSidebar } from "@/components/admin/sidebar"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Switch } from "@/components/ui/switch"
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
  useCodingProblems,
  useUpdateProblemStatus,
  useDeleteCodingProblem,
} from "@/hooks/use-coding-problems"
import { getLanguageById } from "@/config/languages"
import type { CodingProblemSummary } from "@/services/coding-problem-service"

export const Route = createFileRoute("/admin/coding-problem/")({
  component: AdminCodingProblemsPage,
})

export function AdminCodingProblemsPage() {
  const navigate = useNavigate()
  const isAuthenticated = useIsAuthenticated()
  const isInitialized = useIsInitialized()

  // Filters and Pagination State
  const [searchInput, setSearchInput] = useState("")
  const deferredSearch = useDeferredValue(searchInput)

  const [statusFilter, setStatusFilter] = useState<
    "ALL" | "PUBLISHED" | "UNPUBLISHED"
  >("ALL")
  const [typeFilter, setTypeFilter] = useState<"ALL" | "GENERIC" | "DATABASE">(
    "ALL"
  )
  const [difficultyFilter, setDifficultyFilter] = useState<
    "ALL" | "EASY" | "MEDIUM" | "HARD"
  >("ALL")
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const [copiedSlug, setCopiedSlug] = useState<string | null>(null)

  // Auth gate
  useEffect(() => {
    let isMounted = true

    async function verifySession() {
      if (!isInitialized) {
        const hasActiveSession = await initializeAuthSession()
        if (isMounted && !hasActiveSession) {
          await navigate({ to: "/admin/login" })
        }
      } else if (!isAuthenticated) {
        await navigate({ to: "/admin/login" })
      }
    }

    verifySession()

    return () => {
      isMounted = false
    }
  }, [isInitialized, isAuthenticated, navigate])

  // Reset page when filters change
  useEffect(() => {
    setPage(0)
  }, [deferredSearch, statusFilter, typeFilter, difficultyFilter, pageSize])

  // Data Query
  const {
    data: problemsPage,
    isLoading,
    isFetching,
    error,
    refetch,
  } = useCodingProblems({
    search: deferredSearch,
    status: statusFilter,
    problemType: typeFilter,
    difficulty: difficultyFilter,
    page,
    size: pageSize,
    sortBy: "createdAt",
    sortDirection: "DESC",
  })

  // Status Mutation
  const updateStatusMutation = useUpdateProblemStatus()
  const [mutatingId, setMutatingId] = useState<string | null>(null)

  // Delete Mutation & State
  const deleteMutation = useDeleteCodingProblem()
  const [problemToDelete, setProblemToDelete] = useState<CodingProblemSummary | null>(null)

  const handleDeleteConfirm = async () => {
    if (!problemToDelete) return
    try {
      await deleteMutation.mutateAsync(problemToDelete.id)
      toast.add({
        title: "Problem Deleted",
        description: `Successfully deleted "${problemToDelete.title}".`,
        type: "success",
      })
      setProblemToDelete(null)
    } catch (err: unknown) {
      const errorMsg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message || "Failed to delete problem"
      toast.add({
        title: "Delete Failed",
        description: errorMsg,
        type: "error",
      })
    }
  }

  const handleToggleStatus = async (problem: CodingProblemSummary) => {
    const nextStatus =
      problem.status === "PUBLISHED" ? "UNPUBLISHED" : "PUBLISHED"
    setMutatingId(problem.id)
    try {
      await updateStatusMutation.mutateAsync({
        idOrSlug: problem.id,
        status: nextStatus,
      })
    } finally {
      setMutatingId(null)
    }
  }

  const handleCopySlug = (slug: string) => {
    navigator.clipboard.writeText(slug)
    setCopiedSlug(slug)
    toast.add({
      title: "Slug Copied",
      description: `Copied "${slug}" to clipboard.`,
      type: "info",
    })
    setTimeout(() => setCopiedSlug(null), 2000)
  }

  const hasActiveFilters =
    searchInput.trim().length > 0 ||
    statusFilter !== "ALL" ||
    typeFilter !== "ALL" ||
    difficultyFilter !== "ALL"

  const handleResetFilters = () => {
    setSearchInput("")
    setStatusFilter("ALL")
    setTypeFilter("ALL")
    setDifficultyFilter("ALL")
    setPage(0)
  }

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

  const items = problemsPage?.items || []
  const totalElements = problemsPage?.totalElements ?? 0
  const totalPages = problemsPage?.totalPages ?? 0
  const isFirst = problemsPage?.isFirst ?? true
  const isLast = problemsPage?.isLast ?? true

  return (
    <div className="flex min-h-screen bg-background text-foreground">
      {/* Sidebar */}
      <AdminSidebar currentPath="/admin/coding-problem" />

      {/* Main Content */}
      <main className="flex-1 overflow-y-auto">
        {/* Top Header Bar */}
        <header className="sticky top-0 z-10 flex h-16 items-center justify-between border-b border-border bg-background/80 px-8 backdrop-blur-md">
          <div className="flex items-center gap-3">
            <div className="flex size-9 items-center justify-center rounded-lg border border-primary/20 bg-primary/10 text-primary">
              <Code2 className="size-5" />
            </div>
            <div>
              <h1 className="text-lg font-semibold tracking-tight text-foreground">
                Coding Problems
              </h1>
              <p className="text-xs text-muted-foreground">
                Manage algorithmic problems, testcase configurations, and
                publication statuses
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <Link to="/admin/coding-problem/studio/new">
              <Button size="sm" className="gap-2 shadow-xs">
                <Plus className="size-4" />
                <span>Create Problem</span>
              </Button>
            </Link>
          </div>
        </header>

        {/* Page Body */}
        <div className="space-y-6 p-8">
          {/* Filter & Search Toolbar */}
          <div className="flex flex-col gap-4 rounded-xl border border-border bg-card/60 p-4 shadow-2xs backdrop-blur-xs md:flex-row md:items-center md:justify-between">
            {/* Search Input */}
            <div className="relative flex-1 md:max-w-md">
              <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                placeholder="Search by title or slug..."
                className="pl-9 text-sm"
              />
              {searchInput && (
                <button
                  type="button"
                  onClick={() => setSearchInput("")}
                  className="absolute top-1/2 right-3 -translate-y-1/2 text-xs text-muted-foreground hover:text-foreground"
                >
                  ✕
                </button>
              )}
            </div>

            {/* Dropdown Filters */}
            <div className="flex flex-wrap items-center gap-2.5">
              {/* Status Filter */}
              <div className="flex items-center gap-1.5">
                <span className="text-xs font-medium text-muted-foreground">
                  Status:
                </span>
                <NativeSelect
                  size="sm"
                  value={statusFilter}
                  onChange={(e) =>
                    setStatusFilter(
                      e.target.value as "ALL" | "PUBLISHED" | "UNPUBLISHED"
                    )
                  }
                >
                  <NativeSelectOption value="ALL">All Status</NativeSelectOption>
                  <NativeSelectOption value="PUBLISHED">
                    Published
                  </NativeSelectOption>
                  <NativeSelectOption value="UNPUBLISHED">
                    Unpublished
                  </NativeSelectOption>
                </NativeSelect>
              </div>

              {/* Type Filter */}
              <div className="flex items-center gap-1.5">
                <span className="text-xs font-medium text-muted-foreground">
                  Type:
                </span>
                <NativeSelect
                  size="sm"
                  value={typeFilter}
                  onChange={(e) =>
                    setTypeFilter(
                      e.target.value as "ALL" | "GENERIC" | "DATABASE"
                    )
                  }
                >
                  <NativeSelectOption value="ALL">All Types</NativeSelectOption>
                  <NativeSelectOption value="GENERIC">
                    Generic Algorithm
                  </NativeSelectOption>
                  <NativeSelectOption value="DATABASE">
                    Database Query
                  </NativeSelectOption>
                </NativeSelect>
              </div>

              {/* Difficulty Filter */}
              <div className="flex items-center gap-1.5">
                <span className="text-xs font-medium text-muted-foreground">
                  Difficulty:
                </span>
                <NativeSelect
                  size="sm"
                  value={difficultyFilter}
                  onChange={(e) =>
                    setDifficultyFilter(
                      e.target.value as "ALL" | "EASY" | "MEDIUM" | "HARD"
                    )
                  }
                >
                  <NativeSelectOption value="ALL">
                    All Difficulties
                  </NativeSelectOption>
                  <NativeSelectOption value="EASY">Easy</NativeSelectOption>
                  <NativeSelectOption value="MEDIUM">Medium</NativeSelectOption>
                  <NativeSelectOption value="HARD">Hard</NativeSelectOption>
                </NativeSelect>
              </div>

              {/* Reset button */}
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

          {/* Problems Table Card */}
          <div className="rounded-xl border border-border bg-card shadow-xs overflow-hidden">
            {isLoading ? (
              <div className="flex flex-col items-center justify-center py-24 text-muted-foreground">
                <Loader2 className="size-8 animate-spin text-primary mb-3" />
                <span className="text-sm font-medium">
                  Loading coding problems...
                </span>
              </div>
            ) : error ? (
              <div className="flex flex-col items-center justify-center py-20 text-center">
                <AlertCircle className="size-10 text-destructive mb-2" />
                <h3 className="text-base font-semibold text-foreground">
                  Failed to load coding problems
                </h3>
                <p className="text-sm text-muted-foreground max-w-sm mt-1 mb-4">
                  {(error as any)?.message ||
                    "Could not retrieve problem list from the backend."}
                </p>
                <Button variant="outline" size="sm" onClick={() => refetch()}>
                  Retry
                </Button>
              </div>
            ) : items.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-20 px-4 text-center">
                <div className="flex size-14 items-center justify-center rounded-2xl bg-muted/60 text-muted-foreground mb-4">
                  <Code2 className="size-7" />
                </div>
                <h3 className="text-base font-semibold text-foreground">
                  No coding problems found
                </h3>
                <p className="text-sm text-muted-foreground max-w-sm mt-1 mb-5">
                  {hasActiveFilters
                    ? "No problems match your current search and filter criteria. Try resetting filters."
                    : "Your problem catalog is currently empty. Create your first coding challenge in the studio."}
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
                  <Link to="/admin/coding-problem/studio/new">
                    <Button size="sm" className="gap-2">
                      <Plus className="size-4" />
                      <span>Create Your First Problem</span>
                    </Button>
                  </Link>
                )}
              </div>
            ) : (
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow className="hover:bg-transparent">
                      <TableHead className="w-[300px]">Problem</TableHead>
                      <TableHead className="w-[120px]">Type</TableHead>
                      <TableHead className="w-[110px]">Difficulty</TableHead>
                      <TableHead className="w-[200px]">Languages</TableHead>
                      <TableHead className="w-[140px]">Testcases</TableHead>
                      <TableHead className="w-[150px]">Status</TableHead>
                      <TableHead className="w-[80px] text-right">Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {items.map((problem) => {
                      const isMutating = mutatingId === problem.id
                      const isPublished = problem.status === "PUBLISHED"

                      // Difficulty style
                      const difficultyColor =
                        problem.difficulty === "EASY"
                          ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
                          : problem.difficulty === "MEDIUM"
                            ? "border-amber-500/30 bg-amber-500/10 text-amber-600 dark:text-amber-400"
                            : "border-rose-500/30 bg-rose-500/10 text-rose-600 dark:text-rose-400"

                      // Type badge
                      const isDatabase = problem.problemType === "DATABASE"

                      return (
                        <TableRow
                          key={problem.id}
                          className="group transition-colors hover:bg-muted/40"
                        >
                          {/* Title & Slug */}
                          <TableCell className="font-medium">
                            <div className="flex flex-col gap-0.5">
                              <Link
                                to="/admin/coding-problem/studio/$problemId"
                                params={{ problemId: problem.id }}
                                className="text-sm font-semibold text-foreground line-clamp-1 hover:text-primary transition-colors cursor-pointer"
                              >
                                {problem.title}
                              </Link>
                              <div className="flex items-center gap-1.5 text-xs text-muted-foreground font-mono">
                                <span>{problem.slug}</span>
                                <button
                                  type="button"
                                  onClick={() => handleCopySlug(problem.slug)}
                                  className="text-muted-foreground/60 transition-colors hover:text-foreground"
                                  title="Copy slug"
                                >
                                  {copiedSlug === problem.slug ? (
                                    <Check className="size-3 text-emerald-500" />
                                  ) : (
                                    <Copy className="size-3" />
                                  )}
                                </button>
                              </div>
                            </div>
                          </TableCell>

                          {/* Problem Type */}
                          <TableCell>
                            <Badge
                              variant="outline"
                              className={`gap-1 font-normal text-xs ${
                                isDatabase
                                  ? "border-cyan-500/30 bg-cyan-500/10 text-cyan-600 dark:text-cyan-400"
                                  : "border-indigo-500/30 bg-indigo-500/10 text-indigo-600 dark:text-indigo-400"
                              }`}
                            >
                              {isDatabase ? (
                                <Database className="size-3" />
                              ) : (
                                <Layers className="size-3" />
                              )}
                              <span>
                                {isDatabase ? "Database" : "Algorithm"}
                              </span>
                            </Badge>
                          </TableCell>

                          {/* Difficulty */}
                          <TableCell>
                            <Badge
                              variant="outline"
                              className={`font-semibold text-xs capitalize ${difficultyColor}`}
                            >
                              {problem.difficulty?.toLowerCase()}
                            </Badge>
                          </TableCell>

                          {/* Supported Languages */}
                          <TableCell>
                            <div className="flex flex-wrap items-center gap-1">
                              {(problem.supportedLanguageIds || [])
                                .slice(0, 2)
                                .map((langId) => {
                                  const lang = getLanguageById(langId)
                                  return (
                                    <span
                                      key={langId}
                                      className="inline-flex items-center rounded-md border border-border bg-muted/60 px-1.5 py-0.5 text-[11px] font-medium text-muted-foreground"
                                    >
                                      {lang?.name ? lang.name.split(" ")[0] : langId}
                                    </span>
                                  )
                                })}
                              {(problem.supportedLanguageIds?.length || 0) >
                                2 && (
                                <span className="inline-flex items-center rounded-md border border-border bg-muted/30 px-1.5 py-0.5 text-[11px] text-muted-foreground">
                                  +
                                  {(problem.supportedLanguageIds?.length || 0) -
                                    2}
                                </span>
                              )}
                            </div>
                          </TableCell>

                          {/* Testcase Counts */}
                          <TableCell>
                            <div className="flex flex-col text-xs text-muted-foreground">
                              <span className="font-medium text-foreground">
                                {problem.sampleTestCaseCount} sample
                              </span>
                              <span className="text-[11px]">
                                {problem.hiddenTestCaseCount} hidden
                              </span>
                            </div>
                          </TableCell>

                          {/* Live Status Toggle */}
                          <TableCell>
                            <div className="flex items-center gap-2.5">
                              <Switch
                                size="sm"
                                checked={isPublished}
                                disabled={isMutating}
                                onCheckedChange={() =>
                                  handleToggleStatus(problem)
                                }
                                aria-label={`Toggle publication status for ${problem.title}`}
                              />
                              <div className="flex items-center gap-1">
                                {isMutating ? (
                                  <Loader2 className="size-3 animate-spin text-muted-foreground" />
                                ) : (
                                  <span
                                    className={`text-xs font-medium ${
                                      isPublished
                                        ? "text-emerald-600 dark:text-emerald-400"
                                        : "text-muted-foreground"
                                    }`}
                                  >
                                    {isPublished ? "Published" : "Unpublished"}
                                  </span>
                                )}
                              </div>
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
                                    className="size-8"
                                    aria-label="Open problem actions"
                                  >
                                    <MoreVertical className="size-4" />
                                  </Button>
                                }
                              />
                              <DropdownMenuContent align="end" className="w-44">
                                <DropdownMenuItem
                                  onClick={() => handleCopySlug(problem.slug)}
                                  className="gap-2 cursor-pointer"
                                >
                                  <Copy className="size-3.5" />
                                  <span>Copy Slug</span>
                                </DropdownMenuItem>
                                <DropdownMenuItem
                                  onClick={() => handleToggleStatus(problem)}
                                  className="gap-2 cursor-pointer"
                                >
                                  <CheckCircle2 className="size-3.5" />
                                  <span>
                                    {isPublished ? "Unpublish" : "Publish"}
                                  </span>
                                </DropdownMenuItem>
                                <DropdownMenuSeparator />
                                <DropdownMenuItem
                                  onClick={() =>
                                    navigate({
                                      to: "/admin/coding-problem/studio/$problemId",
                                      params: { problemId: problem.id },
                                    })
                                  }
                                  className="gap-2 cursor-pointer"
                                >
                                  <Pencil className="size-3.5" />
                                  <span>Edit Problem</span>
                                </DropdownMenuItem>
                                <DropdownMenuSeparator />
                                <DropdownMenuItem
                                  onClick={() => setProblemToDelete(problem)}
                                  className="gap-2 cursor-pointer text-destructive focus:text-destructive"
                                >
                                  <Trash2 className="size-3.5" />
                                  <span>Delete Problem</span>
                                </DropdownMenuItem>
                              </DropdownMenuContent>
                            </DropdownMenu>
                          </TableCell>
                        </TableRow>
                      )
                    })}
                  </TableBody>
                </Table>
              </div>
            )}

            {/* Pagination Controls */}
            {items.length > 0 && (
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
                    problems
                  </span>

                  {isFetching && (
                    <Loader2 className="size-3 animate-spin text-primary ml-1" />
                  )}
                </div>

                <div className="flex items-center gap-4">
                  {/* Rows per page selector */}
                  <div className="flex items-center gap-1.5">
                    <span>Rows per page:</span>
                    <NativeSelect
                      size="sm"
                      value={pageSize}
                      onChange={(e) => setPageSize(Number(e.target.value))}
                    >
                      <NativeSelectOption value="10">10</NativeSelectOption>
                      <NativeSelectOption value="20">20</NativeSelectOption>
                      <NativeSelectOption value="50">50</NativeSelectOption>
                    </NativeSelect>
                  </div>

                  {/* Previous / Next buttons */}
                  <div className="flex items-center gap-1.5">
                    <span className="mr-1">
                      Page {page + 1} of {Math.max(1, totalPages)}
                    </span>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPage((p) => Math.max(0, p - 1))}
                      disabled={isFirst || isLoading}
                      className="h-8 px-2.5 text-xs"
                    >
                      Previous
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPage((p) => p + 1)}
                      disabled={isLast || isLoading}
                      className="h-8 px-2.5 text-xs"
                    >
                      Next
                    </Button>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Delete Confirmation Dialog */}
        <AlertDialog
          open={Boolean(problemToDelete)}
          onOpenChange={(open) => !open && setProblemToDelete(null)}
        >
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>Delete Coding Problem</AlertDialogTitle>
              <AlertDialogDescription>
                Are you sure you want to delete{" "}
                <span className="font-semibold text-foreground">
                  "{problemToDelete?.title}"
                </span>
                ? This action cannot be undone and will permanently remove the
                problem along with any orphaned images.
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel disabled={deleteMutation.isPending}>
                Cancel
              </AlertDialogCancel>
              <AlertDialogAction
                variant="destructive"
                disabled={deleteMutation.isPending}
                onClick={handleDeleteConfirm}
              >
                {deleteMutation.isPending ? (
                  <>
                    <Loader2 className="size-4 animate-spin mr-1.5" />
                    Deleting...
                  </>
                ) : (
                  "Delete"
                )}
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </main>
    </div>
  )
}
