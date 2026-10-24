import { useEffect, useState, useDeferredValue } from "react"
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router"
import {
  CheckSquare,
  Plus,
  Search,
  CheckCircle2,
  RotateCcw,
  Layers,
  Loader2,
  MoreVertical,
  Pencil,
  Trash2,
  Eye,
  Tag,
  FolderOpen,
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
  useMcqQuestions,
  useMcqStats,
  useMcqTopics,
  useDeleteMcqQuestion,
  usePublishMcq,
  useUnpublishMcq,
} from "@/hooks/use-mcq"
import { mcqService } from "@/services/mcq-service"
import { TopicManagerDialog } from "@/components/studio/mcq/topic-manager-dialog"
import { McqPreviewModal } from "@/components/studio/mcq/mcq-preview-modal"
import type {
  McqDifficulty,
  McqQuestionDetail,
  McqQuestionItem,
  McqStatus,
  McqType,
} from "@/types/mcq"

export const Route = createFileRoute("/admin/mcq/")({
  component: AdminMcqPage,
})

export function AdminMcqPage() {
  const navigate = useNavigate()
  const isAuthenticated = useIsAuthenticated()
  const isInitialized = useIsInitialized()

  // Filters and Pagination State
  const [searchInput, setSearchInput] = useState("")
  const deferredSearch = useDeferredValue(searchInput)

  const [typeFilter, setTypeFilter] = useState<string>("ALL")
  const [difficultyFilter, setDifficultyFilter] = useState<string>("ALL")
  const [statusFilter, setStatusFilter] = useState<string>("ALL")
  const [topicFilter, setTopicFilter] = useState<string>("ALL")
  const [page, setPage] = useState(0)
  const pageSize = 10

  // Dialogs
  const [topicManagerOpen, setTopicManagerOpen] = useState(false)
  const [previewQuestion, setPreviewQuestion] = useState<McqQuestionDetail | null>(null)
  const [previewModalOpen, setPreviewModalOpen] = useState(false)
  const [questionToDelete, setQuestionToDelete] = useState<McqQuestionItem | null>(null)

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
  }, [isAuthenticated, isInitialized, navigate])

  // Queries & Mutations
  const statsQuery = useMcqStats()
  const stats = statsQuery.data
  const { data: topicList = [] } = useMcqTopics()
  const deleteMutation = useDeleteMcqQuestion()
  const publishMutation = usePublishMcq()
  const unpublishMutation = useUnpublishMcq()
  const [mutatingId, setMutatingId] = useState<string | null>(null)

  const { data: mcqPage, isLoading } = useMcqQuestions({
    page,
    size: pageSize,
    search: deferredSearch.trim() || undefined,
    type: typeFilter !== "ALL" ? (typeFilter as McqType) : undefined,
    difficulty: difficultyFilter !== "ALL" ? (difficultyFilter as McqDifficulty) : undefined,
    status: statusFilter !== "ALL" ? (statusFilter as McqStatus) : undefined,
    topic: topicFilter !== "ALL" ? topicFilter : undefined,
  })

  // Handlers
  const handleOpenPreview = async (questionItem: McqQuestionItem) => {
    try {
      const detail = await mcqService.getQuestion(questionItem.id)
      setPreviewQuestion(detail)
      setPreviewModalOpen(true)
    } catch {
      toast.add({
        title: "Preview Failed",
        description: "Could not load question details for preview.",
        type: "error",
      })
    }
  }

  const handleDeleteConfirm = async () => {
    if (!questionToDelete) return
    try {
      await deleteMutation.mutateAsync(questionToDelete.id)
      setQuestionToDelete(null)
    } catch {
      // Error handled by mutation onError
    }
  }

  const handleToggleStatus = async (item: McqQuestionItem) => {
    const isCurrentlyPublished = item.status === "PUBLISHED"
    setMutatingId(item.id)
    try {
      if (isCurrentlyPublished) {
        await unpublishMutation.mutateAsync(item.id)
      } else {
        await publishMutation.mutateAsync(item.id)
      }
    } catch {
      // Error handled by mutation onError
    } finally {
      setMutatingId(null)
    }
  }

  const handleResetFilters = () => {
    setSearchInput("")
    setTypeFilter("ALL")
    setDifficultyFilter("ALL")
    setStatusFilter("ALL")
    setTopicFilter("ALL")
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

  const items = mcqPage?.items || []
  const totalElements = mcqPage?.totalElements ?? 0
  const totalPages = mcqPage?.totalPages ?? 0
  const isFirst = mcqPage?.isFirst ?? true
  const isLast = mcqPage?.isLast ?? true

  return (
    <div className="flex h-screen w-full overflow-hidden bg-background text-foreground">
      {/* Sidebar */}
      <AdminSidebar currentPath="/admin/mcq" />

      {/* Main Content Area */}
      <main className="flex-1 overflow-y-auto">
        {/* Top Header Bar */}
        <header className="sticky top-0 z-10 flex h-16 items-center justify-between border-b border-border bg-background/80 px-8 backdrop-blur-md">
          <div className="flex items-center gap-3">
            <div className="flex size-9 items-center justify-center rounded-lg border border-primary/20 bg-primary/10 text-primary">
              <CheckSquare className="size-5" />
            </div>
            <div>
              <h1 className="text-lg font-semibold tracking-tight text-foreground">
                Multiple Choice Questions
              </h1>
              <p className="text-xs text-muted-foreground">
                Curate, organize, and administer conceptual and objective questions across domains
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setTopicManagerOpen(true)}
              className="gap-2 text-xs"
            >
              <Tag className="size-3.5" />
              Manage Topics
            </Button>

            <Link to="/admin/mcq/studio/new">
              <Button
                size="sm"
                className="gap-2 text-xs bg-primary text-primary-foreground hover:bg-primary/90"
              >
                <Plus className="size-4" />
                New MCQ
              </Button>
            </Link>
          </div>
        </header>

        <div className="space-y-6 p-8">
          {/* Stats Bar */}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <div className="flex items-center gap-4 rounded-xl border border-border/60 bg-card p-4">
              <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
                <CheckSquare className="size-5" />
              </div>
              <div>
                <p className="text-xs font-medium text-muted-foreground">Total MCQs</p>
                <p className="text-2xl font-bold tracking-tight text-foreground">
                  {stats?.totalQuestions ?? 0}
                </p>
              </div>
            </div>

            <div className="flex items-center gap-4 rounded-xl border border-border/60 bg-card p-4">
              <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-emerald-500/10 text-emerald-600 dark:text-emerald-400">
                <CheckCircle2 className="size-5" />
              </div>
              <div>
                <p className="text-xs font-medium text-muted-foreground">Single Choice</p>
                <p className="text-2xl font-bold tracking-tight text-foreground">
                  {stats?.singleChoiceCount ?? 0}
                </p>
              </div>
            </div>

            <div className="flex items-center gap-4 rounded-xl border border-border/60 bg-card p-4">
              <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-indigo-500/10 text-indigo-600 dark:text-indigo-400">
                <Layers className="size-5" />
              </div>
              <div>
                <p className="text-xs font-medium text-muted-foreground">Multiple Choice</p>
                <p className="text-2xl font-bold tracking-tight text-foreground">
                  {stats?.multipleChoiceCount ?? 0}
                </p>
              </div>
            </div>

            <div className="flex items-center gap-4 rounded-xl border border-border/60 bg-card p-4">
              <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-amber-500/10 text-amber-600 dark:text-amber-400">
                <Tag className="size-5" />
              </div>
              <div>
                <p className="text-xs font-medium text-muted-foreground">Active Topics</p>
                <p className="text-2xl font-bold tracking-tight text-foreground">
                  {stats?.totalTopics ?? topicList.length}
                </p>
              </div>
            </div>
          </div>

          {/* Search & Filter Toolbar */}
          <div className="flex flex-col gap-3 rounded-xl border border-border/70 bg-card p-4 shadow-xs">
            <div className="flex flex-wrap items-center gap-3">
              {/* Search Box */}
              <div className="relative min-w-[240px] flex-1">
                <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  placeholder="Search questions by title, content, or tag..."
                  value={searchInput}
                  onChange={(e) => {
                    setSearchInput(e.target.value)
                    setPage(0)
                  }}
                  className="pl-9 text-xs"
                />
              </div>

              {/* Type Filter */}
              <div className="w-36">
                <NativeSelect
                  value={typeFilter}
                  onChange={(e) => {
                    setTypeFilter(e.target.value)
                    setPage(0)
                  }}
                  className="h-8 text-xs"
                >
                  <NativeSelectOption value="ALL">All Types</NativeSelectOption>
                  <NativeSelectOption value="SINGLE_CHOICE">Single Choice</NativeSelectOption>
                  <NativeSelectOption value="MULTIPLE_CHOICE">Multi Choice</NativeSelectOption>
                  <NativeSelectOption value="BOOLEAN">Boolean</NativeSelectOption>
                </NativeSelect>
              </div>

              {/* Difficulty Filter */}
              <div className="w-36">
                <NativeSelect
                  value={difficultyFilter}
                  onChange={(e) => {
                    setDifficultyFilter(e.target.value)
                    setPage(0)
                  }}
                  className="h-8 text-xs"
                >
                  <NativeSelectOption value="ALL">All Difficulties</NativeSelectOption>
                  <NativeSelectOption value="EASY">Easy</NativeSelectOption>
                  <NativeSelectOption value="MEDIUM">Medium</NativeSelectOption>
                  <NativeSelectOption value="HARD">Hard</NativeSelectOption>
                </NativeSelect>
              </div>

              {/* Status Filter */}
              <div className="w-36">
                <NativeSelect
                  value={statusFilter}
                  onChange={(e) => {
                    setStatusFilter(e.target.value)
                    setPage(0)
                  }}
                  className="h-8 text-xs"
                >
                  <NativeSelectOption value="ALL">All Statuses</NativeSelectOption>
                  <NativeSelectOption value="PUBLISHED">Published</NativeSelectOption>
                  <NativeSelectOption value="DRAFT">Draft</NativeSelectOption>
                </NativeSelect>
              </div>

              {/* Topic Filter */}
              <div className="w-40">
                <NativeSelect
                  value={topicFilter}
                  onChange={(e) => {
                    setTopicFilter(e.target.value)
                    setPage(0)
                  }}
                  className="h-8 text-xs"
                >
                  <NativeSelectOption value="ALL">All Topics</NativeSelectOption>
                  {topicList.map((t) => (
                    <NativeSelectOption key={t.id} value={t.name}>
                      {t.name}
                    </NativeSelectOption>
                  ))}
                </NativeSelect>
              </div>

              {/* Reset Button */}
              <Button
                variant="ghost"
                size="sm"
                onClick={handleResetFilters}
                className="gap-1.5 text-xs text-muted-foreground hover:text-foreground h-8"
              >
                <RotateCcw className="size-3.5" />
                Reset
              </Button>
            </div>
          </div>

          {/* MCQ Table */}
          <div className="overflow-hidden rounded-xl border border-border/70 bg-card">
            <Table>
              <TableHeader>
                <TableRow className="hover:bg-transparent">
                  <TableHead className="w-[44%]">Question</TableHead>
                  <TableHead className="w-[16%]">Type & Difficulty</TableHead>
                  <TableHead className="w-[14%]">Options</TableHead>
                  <TableHead className="w-[12%]">Status</TableHead>
                  <TableHead className="w-[14%] text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {isLoading ? (
                  <TableRow>
                    <TableCell colSpan={5} className="h-48 text-center">
                      <div className="flex flex-col items-center justify-center gap-2">
                        <Loader2 className="size-6 animate-spin text-primary" />
                        <span className="text-xs text-muted-foreground">
                          Loading multiple choice questions...
                        </span>
                      </div>
                    </TableCell>
                  </TableRow>
                ) : items.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={5} className="h-48 text-center">
                      <div className="flex flex-col items-center justify-center gap-2">
                        <FolderOpen className="size-8 stroke-1 text-muted-foreground/60" />
                        <span className="text-sm font-medium text-foreground">
                          No multiple choice questions found
                        </span>
                        <span className="text-xs text-muted-foreground max-w-sm">
                          Try adjusting your search criteria, or click &ldquo;New MCQ&rdquo; to add your first question.
                        </span>
                      </div>
                    </TableCell>
                  </TableRow>
                ) : (
                  items.map((item) => {
                    const diffBadgeClass =
                      item.difficulty === "EASY"
                        ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
                        : item.difficulty === "MEDIUM"
                        ? "border-amber-500/30 bg-amber-500/10 text-amber-600 dark:text-amber-400"
                        : "border-rose-500/30 bg-rose-500/10 text-rose-600 dark:text-rose-400"

                    return (
                      <TableRow
                        key={item.id}
                        className="group transition-colors hover:bg-muted/40"
                      >
                        {/* Title & Topics */}
                        <TableCell>
                          <div className="flex flex-col gap-1">
                            <Link
                              to="/admin/mcq/studio/$questionId"
                              params={{ questionId: item.id }}
                              className="text-sm font-semibold text-foreground line-clamp-1 hover:text-primary transition-colors cursor-pointer"
                            >
                              {item.title}
                            </Link>
                            <div className="flex flex-wrap items-center gap-1.5 pt-0.5">
                              {item.topics?.map((topic) => (
                                <Badge
                                  key={topic}
                                  variant="outline"
                                  className="text-[10px] px-1.5 py-0 h-4 bg-muted/40"
                                >
                                  {topic}
                                </Badge>
                              ))}
                            </div>
                          </div>
                        </TableCell>

                        {/* Type & Difficulty */}
                        <TableCell>
                          <div className="flex flex-col gap-1 items-start">
                            <span className="text-xs font-medium text-foreground">
                              {item.type === "SINGLE_CHOICE"
                                ? "Single Choice"
                                : item.type === "MULTIPLE_CHOICE"
                                ? "Multiple Choice"
                                : "Boolean"}
                            </span>
                            <Badge
                              variant="outline"
                              className={`text-[10px] font-semibold h-4 px-1.5 ${diffBadgeClass}`}
                            >
                              {item.difficulty}
                            </Badge>
                          </div>
                        </TableCell>

                        {/* Options count */}
                        <TableCell>
                          <div className="flex flex-col text-xs text-muted-foreground">
                            <span className="font-medium text-foreground">
                              {item.optionsCount} options
                            </span>
                            <span className="text-[11px] text-emerald-600 dark:text-emerald-400">
                              {item.correctOptionsCount} correct
                            </span>
                          </div>
                        </TableCell>

                        {/* Status Toggle */}
                        <TableCell>
                          <div className="flex items-center gap-2.5">
                            <Switch
                              size="sm"
                              checked={item.status === "PUBLISHED"}
                              disabled={mutatingId === item.id}
                              onCheckedChange={() => handleToggleStatus(item)}
                              aria-label={`Toggle publication status for ${item.title}`}
                            />
                            <div className="flex items-center gap-1">
                              {mutatingId === item.id ? (
                                <Loader2 className="size-3 animate-spin text-muted-foreground" />
                              ) : (
                                <span
                                  className={`text-xs font-medium ${
                                    item.status === "PUBLISHED"
                                      ? "text-emerald-600 dark:text-emerald-400"
                                      : "text-muted-foreground"
                                  }`}
                                >
                                  {item.status === "PUBLISHED" ? "Published" : "Draft"}
                                </span>
                              )}
                            </div>
                          </div>
                        </TableCell>

                        {/* Action Menu */}
                        <TableCell className="text-right">
                          <DropdownMenu>
                            <DropdownMenuTrigger
                              render={
                                <Button
                                  variant="ghost"
                                  size="icon"
                                  className="size-8"
                                  aria-label="Open question actions"
                                >
                                  <MoreVertical className="size-4" />
                                </Button>
                              }
                            />
                            <DropdownMenuContent align="end" className="w-44">
                              <DropdownMenuItem
                                onClick={() => handleOpenPreview(item)}
                                className="gap-2 cursor-pointer"
                              >
                                <Eye className="size-3.5" />
                                <span>Preview Question</span>
                              </DropdownMenuItem>

                              <DropdownMenuItem
                                onClick={() => handleToggleStatus(item)}
                                disabled={mutatingId === item.id}
                                className="gap-2 cursor-pointer"
                              >
                                <CheckCircle2 className="size-3.5" />
                                <span>
                                  {item.status === "PUBLISHED" ? "Unpublish" : "Publish"}
                                </span>
                              </DropdownMenuItem>

                              <DropdownMenuSeparator />

                              <DropdownMenuItem
                                onClick={() => {
                                  if (item.status === "PUBLISHED") {
                                    toast.add({
                                      title: "Cannot Edit Published Question",
                                      description: "Only draft questions can be edited. Unpublish to DRAFT first.",
                                      type: "warning",
                                    })
                                    return
                                  }
                                  navigate({
                                    to: "/admin/mcq/studio/$questionId",
                                    params: { questionId: item.id },
                                  })
                                }}
                                disabled={item.status === "PUBLISHED"}
                                className="gap-2 cursor-pointer"
                              >
                                <Pencil className="size-3.5" />
                                <span>Edit Question</span>
                              </DropdownMenuItem>

                              <DropdownMenuSeparator />

                              <DropdownMenuItem
                                onClick={() => setQuestionToDelete(item)}
                                className="gap-2 text-destructive focus:bg-destructive/10 focus:text-destructive cursor-pointer"
                              >
                                <Trash2 className="size-3.5" />
                                <span>Delete</span>
                              </DropdownMenuItem>
                            </DropdownMenuContent>
                          </DropdownMenu>
                        </TableCell>
                      </TableRow>
                    )
                  })
                )}
              </TableBody>
            </Table>

            {/* Pagination footer */}
            <div className="flex items-center justify-between border-t border-border px-4 py-3 text-xs text-muted-foreground">
              <div>
                Showing{" "}
                <span className="font-semibold text-foreground">
                  {items.length > 0 ? page * pageSize + 1 : 0}
                </span>{" "}
                to{" "}
                <span className="font-semibold text-foreground">
                  {Math.min((page + 1) * pageSize, totalElements)}
                </span>{" "}
                of <span className="font-semibold text-foreground">{totalElements}</span> questions
              </div>

              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={isFirst || isLoading}
                  className="h-7 text-xs"
                >
                  Previous
                </Button>
                <span>
                  Page {page + 1} of {Math.max(1, totalPages)}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage((p) => p + 1)}
                  disabled={isLast || isLoading}
                  className="h-7 text-xs"
                >
                  Next
                </Button>
              </div>
            </div>
          </div>
        </div>
      </main>

      {/* Topics Manager Dialog */}
      <TopicManagerDialog
        open={topicManagerOpen}
        onOpenChange={setTopicManagerOpen}
      />

      {/* Quick Candidate Preview Modal */}
      <McqPreviewModal
        question={previewQuestion}
        open={previewModalOpen}
        onOpenChange={setPreviewModalOpen}
      />

      {/* Delete Confirmation Alert Dialog */}
      <AlertDialog
        open={Boolean(questionToDelete)}
        onOpenChange={(open) => !open && setQuestionToDelete(null)}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete MCQ Question</AlertDialogTitle>
            <AlertDialogDescription>
              Are you sure you want to permanently delete the question &ldquo;{questionToDelete?.title}&rdquo;?
              This action cannot be undone and will remove it from any draft assessments.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDeleteConfirm}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              Delete Question
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
