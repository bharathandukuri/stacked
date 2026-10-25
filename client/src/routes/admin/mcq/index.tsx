import { useState, useEffect, useDeferredValue } from "react";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { z } from "zod";
import {
  Search,
  Plus,
  Eye,
  Edit,
  Trash2,
  Tag,
  Loader2,
  FolderOpen,
  ArrowRight,
  ArrowLeft,
  BookOpen,
  CheckCircle2,
  Clock,
  CheckSquare,
  HelpCircle,
  MoreVertical,
  FolderPlus,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Switch } from "@/components/ui/switch";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import {
  NativeSelect,
  NativeSelectOption,
} from "@/components/ui/native-select";
import { AdminSidebar } from "@/components/admin/sidebar";
import { McqPreviewModal } from "@/components/studio/mcq/mcq-preview-modal";
import { TopicFormDialog } from "@/components/studio/mcq/topic-form-dialog";
import { toast } from "@/components/ui/toast";
import { mcqService } from "@/services/mcq-service";
import { useIsAuthenticated, useIsInitialized } from "@/stores/auth-store";
import { initializeAuthSession } from "@/hooks/use-auth";
import {
  useMcqQuestions,
  useMcqTopics,
  useMcqStats,
  useDeleteMcqQuestion,
  useDeleteMcqTopic,
  usePublishMcq,
  useUnpublishMcq,
} from "@/hooks/use-mcq";
import type {
  McqDifficulty,
  McqQuestionDetail,
  McqQuestionItem,
  McqStatus,
  McqTopicItem,
  McqType,
} from "@/types/mcq";
import { cn } from "@/lib/utils";

const mcqSearchSchema = z.object({
  topic: z.string().optional(),
});

export const Route = createFileRoute("/admin/mcq/")({
  validateSearch: mcqSearchSchema,
  component: AdminMcqPage,
});

export function AdminMcqPage() {
  const navigate = useNavigate();
  const { topic: currentTopicName } = Route.useSearch();
  const isAuthenticated = useIsAuthenticated();
  const isInitialized = useIsInitialized();

  // Filters and Pagination State (for Topic Workspace)
  const [searchInput, setSearchInput] = useState("");
  const deferredSearch = useDeferredValue(searchInput);

  const [subtopicInput, setSubtopicInput] = useState("");
  const deferredSubtopic = useDeferredValue(subtopicInput);

  const [typeFilter, setTypeFilter] = useState<string>("ALL");
  const [difficultyFilter, setDifficultyFilter] = useState<string>("ALL");
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  // Search for root topic catalog
  const [topicSearchInput, setTopicSearchInput] = useState("");

  // Dialog States
  const [topicFormOpen, setTopicFormOpen] = useState(false);
  const [topicToEdit, setTopicToEdit] = useState<McqTopicItem | null>(null);
  const [questionToDelete, setQuestionToDelete] =
    useState<McqQuestionItem | null>(null);
  const [topicToDelete, setTopicToDelete] = useState<McqTopicItem | null>(null);
  const [previewQuestion, setPreviewQuestion] =
    useState<McqQuestionDetail | null>(null);
  const [previewModalOpen, setPreviewModalOpen] = useState(false);
  const [mutatingId, setMutatingId] = useState<string | null>(null);

  // Auth gate
  useEffect(() => {
    let isMounted = true;

    async function verifySession() {
      if (!isInitialized) {
        const hasActiveSession = await initializeAuthSession();
        if (isMounted && !hasActiveSession) {
          await navigate({ to: "/admin/login" });
        }
      } else if (!isAuthenticated) {
        await navigate({ to: "/admin/login" });
      }
    }

    verifySession();

    return () => {
      isMounted = false;
    };
  }, [isAuthenticated, isInitialized, navigate]);

  // Queries & Mutations
  const statsQuery = useMcqStats();
  const stats = statsQuery.data;
  const { data: topicList = [], isLoading: loadingTopics } = useMcqTopics();
  const deleteQuestionMutation = useDeleteMcqQuestion();
  const deleteTopicMutation = useDeleteMcqTopic();
  const publishMutation = usePublishMcq();
  const unpublishMutation = useUnpublishMcq();

  // Find currently selected topic object if topic query param is active
  const selectedTopic = currentTopicName
    ? topicList.find(
        (t) => t.name.toLowerCase() === currentTopicName.toLowerCase()
      ) || {
        id: currentTopicName,
        name: currentTopicName,
        slug: currentTopicName.toLowerCase().replace(/[^a-z0-9]+/g, "-"),
        description: "",
        questionCount: 0,
      }
    : null;

  // Query for questions in active topic workspace
  const { data: mcqPage, isLoading: loadingQuestions } = useMcqQuestions({
    page,
    size: pageSize,
    search: deferredSearch.trim() || undefined,
    topic: selectedTopic?.name,
    tag: deferredSubtopic.trim() || undefined,
    type: typeFilter !== "ALL" ? (typeFilter as McqType) : undefined,
    difficulty:
      difficultyFilter !== "ALL"
        ? (difficultyFilter as McqDifficulty)
        : undefined,
    status: statusFilter !== "ALL" ? (statusFilter as McqStatus) : undefined,
  });

  const items = mcqPage?.items || [];
  const totalElements = mcqPage?.totalElements ?? 0;
  const totalPages = mcqPage?.totalPages ?? 0;
  const isFirst = page === 0;
  const isLast = page >= totalPages - 1 || totalPages === 0;

  const hasActiveFilters =
    searchInput.trim().length > 0 ||
    subtopicInput.trim().length > 0 ||
    typeFilter !== "ALL" ||
    difficultyFilter !== "ALL" ||
    statusFilter !== "ALL";

  const handleResetFilters = () => {
    setSearchInput("");
    setSubtopicInput("");
    setTypeFilter("ALL");
    setDifficultyFilter("ALL");
    setStatusFilter("ALL");
    setPage(0);
  };

  const handleSelectTopic = (topic: McqTopicItem) => {
    setSearchInput("");
    setSubtopicInput("");
    setPage(0);
    navigate({
      to: "/admin/mcq",
      search: { topic: topic.name },
    });
  };

  const handleBackToTopics = () => {
    setSearchInput("");
    setSubtopicInput("");
    setPage(0);
    navigate({
      to: "/admin/mcq",
      search: {},
    });
  };

  const handleOpenCreateTopic = () => {
    setTopicToEdit(null);
    setTopicFormOpen(true);
  };

  const handleOpenEditTopic = (topic: McqTopicItem) => {
    setTopicToEdit(topic);
    setTopicFormOpen(true);
  };

  const handleOpenPreview = async (questionItem: McqQuestionItem) => {
    try {
      const detail = await mcqService.getQuestion(questionItem.id);
      setPreviewQuestion(detail);
      setPreviewModalOpen(true);
    } catch {
      toast.add({
        title: "Preview Failed",
        description: "Could not fetch question details for preview.",
        type: "error",
      });
    }
  };

  const handleToggleStatus = async (item: McqQuestionItem) => {
    setMutatingId(item.id);
    try {
      if (item.status === "PUBLISHED") {
        await unpublishMutation.mutateAsync(item.id);
      } else {
        await publishMutation.mutateAsync(item.id);
      }
    } finally {
      setMutatingId(null);
    }
  };

  const getDifficultyBadge = (diff: McqDifficulty) => {
    switch (diff) {
      case "EASY":
        return (
          <Badge className="border-emerald-500/20 bg-emerald-500/10 text-xs font-medium text-emerald-600 dark:text-emerald-400">
            Easy
          </Badge>
        );
      case "HARD":
        return (
          <Badge className="border-rose-500/20 bg-rose-500/10 text-xs font-medium text-rose-600 dark:text-rose-400">
            Hard
          </Badge>
        );
      case "MEDIUM":
      default:
        return (
          <Badge className="border-amber-500/20 bg-amber-500/10 text-xs font-medium text-amber-600 dark:text-amber-400">
            Medium
          </Badge>
        );
    }
  };

  const filteredTopics = topicList.filter((t) => {
    if (!topicSearchInput.trim()) return true;
    const q = topicSearchInput.toLowerCase();
    return (
      t.name.toLowerCase().includes(q) ||
      (t.description && t.description.toLowerCase().includes(q))
    );
  });

  if (!isInitialized) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-3 bg-background">
        <Loader2 className="size-8 animate-spin text-primary" />
        <span className="text-sm font-medium text-muted-foreground">
          Verifying administrator session...
        </span>
      </div>
    );
  }

  if (!isAuthenticated) {
    return null;
  }

  return (
    <div className="flex h-screen w-full overflow-hidden bg-background text-foreground">
      {/* Sidebar */}
      <AdminSidebar currentPath="/admin/mcq" />

      {/* Main Content Area */}
      <main className="min-w-0 flex-1 overflow-y-auto">
        {/* Top Header Bar */}
        <header className="sticky top-0 z-10 flex h-16 items-center justify-between border-b border-border bg-background/80 px-8 backdrop-blur-md">
          <div className="flex min-w-0 items-center gap-3">
            {selectedTopic ? (
              <Button
                variant="ghost"
                size="icon"
                onClick={handleBackToTopics}
                className="size-8 shrink-0 text-muted-foreground hover:text-foreground"
                title="Back to Topics Catalog"
              >
                <ArrowLeft className="size-4" />
              </Button>
            ) : (
              <div className="flex size-9 shrink-0 items-center justify-center rounded-lg border border-primary/20 bg-primary/10 text-primary">
                <CheckSquare className="size-5" />
              </div>
            )}

            <div className="min-w-0">
              <div className="flex items-center gap-2">
                <h1 className="truncate text-lg font-semibold tracking-tight text-foreground">
                  {selectedTopic ? selectedTopic.name : "MCQ Question Bank"}
                </h1>
                {selectedTopic ? (
                  <Badge variant="secondary" className="font-mono text-xs">
                    Topic Workspace
                  </Badge>
                ) : (
                  <Badge
                    variant="outline"
                    className="font-mono text-xs text-muted-foreground"
                  >
                    Curriculum Topics
                  </Badge>
                )}
              </div>
              <p className="truncate text-xs text-muted-foreground">
                {selectedTopic
                  ? selectedTopic.description ||
                    `Curated multiple choice questions and subtopics for ${selectedTopic.name}`
                  : "Select a topic to manage its questions, or create a new topic domain"}
              </p>
            </div>
          </div>

          <div className="flex shrink-0 items-center gap-2.5">
            {!selectedTopic ? (
              <Button
                size="sm"
                onClick={handleOpenCreateTopic}
                className="gap-2 text-xs font-semibold shadow-xs"
              >
                <Plus className="size-4" />
                <span>Create Topic</span>
              </Button>
            ) : (
              <>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleBackToTopics}
                  className="gap-1.5 text-xs text-muted-foreground hover:text-foreground"
                >
                  <ArrowLeft className="size-3.5" />
                  <span>All Topics</span>
                </Button>

                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleOpenEditTopic(selectedTopic)}
                  className="gap-1.5 text-xs text-muted-foreground hover:text-foreground"
                >
                  <Edit className="size-3.5" />
                  <span>Edit Topic</span>
                </Button>

                <Link
                  to="/admin/mcq/studio/new"
                  search={{ topic: selectedTopic.name }}
                >
                  <Button
                    size="sm"
                    className="gap-2 text-xs font-semibold shadow-xs"
                  >
                    <Plus className="size-4" />
                    <span>Create MCQ in {selectedTopic.name}</span>
                  </Button>
                </Link>
              </>
            )}
          </div>
        </header>

        {/* Page Body */}
        <div className="space-y-6 p-8">
          {/* LEVEL 1: ROOT TOPICS CATALOG (NO TABLE) */}
          {!selectedTopic ? (
            <>
              {/* Metric Stats Ribbon */}
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
                <div className="flex items-center gap-4 rounded-xl border border-border/60 bg-card p-4">
                  <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-indigo-500/10 text-indigo-600 dark:text-indigo-400">
                    <FolderOpen className="size-5" />
                  </div>
                  <div>
                    <p className="text-xs font-medium text-muted-foreground">
                      Curriculum Topics
                    </p>
                    <p className="font-mono text-2xl font-bold tracking-tight text-foreground">
                      {topicList.length}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-4 rounded-xl border border-border/60 bg-card p-4">
                  <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
                    <CheckSquare className="size-5" />
                  </div>
                  <div>
                    <p className="text-xs font-medium text-muted-foreground">
                      Total MCQs
                    </p>
                    <p className="font-mono text-2xl font-bold tracking-tight text-foreground">
                      {stats?.totalQuestions ?? 0}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-4 rounded-xl border border-border/60 bg-card p-4">
                  <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-emerald-500/10 text-emerald-600 dark:text-emerald-400">
                    <CheckCircle2 className="size-5" />
                  </div>
                  <div>
                    <p className="text-xs font-medium text-muted-foreground">
                      Published
                    </p>
                    <p className="font-mono text-2xl font-bold tracking-tight text-foreground">
                      {stats?.publishedQuestions ?? 0}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-4 rounded-xl border border-border/60 bg-card p-4">
                  <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-amber-500/10 text-amber-600 dark:text-amber-400">
                    <Clock className="size-5" />
                  </div>
                  <div>
                    <p className="text-xs font-medium text-muted-foreground">
                      Drafts
                    </p>
                    <p className="font-mono text-2xl font-bold tracking-tight text-foreground">
                      {stats?.draftQuestions ?? 0}
                    </p>
                  </div>
                </div>
              </div>

              {/* Toolbar: Search Topics & Action */}
              <div className="flex flex-col gap-4 rounded-xl border border-border bg-card/60 p-4 shadow-2xs backdrop-blur-xs sm:flex-row sm:items-center sm:justify-between">
                <div className="relative flex-1 sm:max-w-xl">
                  <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
                  <Input
                    value={topicSearchInput}
                    onChange={(e) => setTopicSearchInput(e.target.value)}
                    placeholder="Search topics by name..."
                    className="pl-9 text-sm"
                  />
                  {topicSearchInput && (
                    <button
                      type="button"
                      onClick={() => setTopicSearchInput("")}
                      className="absolute top-1/2 right-3 -translate-y-1/2 cursor-pointer text-xs text-muted-foreground hover:text-foreground"
                    >
                      ✕
                    </button>
                  )}
                </div>
                {/* 
                <div className="flex items-center gap-2">
                  <Button
                    size="sm"
                    onClick={handleOpenCreateTopic}
                    className="text-xs gap-1.5 shadow-2xs"
                  >
                    <Plus className="size-3.5" />
                    <span>Create Topic</span>
                  </Button>
                </div> */}
              </div>

              {/* Topics Card Grid */}
              {loadingTopics ? (
                <div className="flex flex-col items-center justify-center py-24 text-muted-foreground">
                  <Loader2 className="mb-3 size-8 animate-spin text-primary" />
                  <span className="text-sm font-medium">
                    Loading topics catalog...
                  </span>
                </div>
              ) : topicList.length === 0 ? (
                <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border/80 bg-card/30 p-16 text-center">
                  <div className="mb-4 flex size-14 items-center justify-center rounded-2xl bg-primary/10 text-primary">
                    <FolderPlus className="size-7" />
                  </div>
                  <h3 className="text-base font-semibold text-foreground">
                    No Topics Created Yet
                  </h3>
                  <p className="mt-1.5 max-w-sm text-xs text-muted-foreground">
                    Create your first curriculum topic (e.g. Quantitative
                    Aptitude, Computer Networks) to start organizing MCQs.
                  </p>
                  <Button
                    size="sm"
                    onClick={handleOpenCreateTopic}
                    className="mt-6 gap-2 text-xs font-semibold"
                  >
                    <Plus className="size-4" />
                    <span>Create First Topic</span>
                  </Button>
                </div>
              ) : filteredTopics.length === 0 ? (
                <div className="flex flex-col items-center justify-center py-16 text-center">
                  <Search className="mb-2 size-8 text-muted-foreground/40" />
                  <p className="text-sm font-medium text-foreground">
                    No topics found matching &ldquo;{topicSearchInput}&rdquo;
                  </p>
                  <Button
                    variant="link"
                    size="sm"
                    onClick={() => setTopicSearchInput("")}
                    className="mt-1 text-xs text-primary"
                  >
                    Clear search
                  </Button>
                </div>
              ) : (
                <div className="grid grid-cols-1 gap-5 md:grid-cols-2 lg:grid-cols-3">
                  {filteredTopics.map((topic) => (
                    <div
                      key={topic.id}
                      onClick={() => handleSelectTopic(topic)}
                      className="group relative flex cursor-pointer flex-col justify-between rounded-xl border border-border/70 bg-card p-5 shadow-xs transition-all duration-200 hover:border-primary/50 hover:shadow-md"
                    >
                      <div>
                        <div className="mb-3 flex items-start justify-between gap-3">
                          <div className="flex items-center gap-3">
                            <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary transition-transform group-hover:scale-105">
                              <BookOpen className="size-5" />
                            </div>
                            <div>
                              <h3 className="line-clamp-1 text-base font-semibold text-foreground transition-colors group-hover:text-primary">
                                {topic.name}
                              </h3>
                              <span className="font-mono text-[11px] text-muted-foreground">
                                /{topic.slug}
                              </span>
                            </div>
                          </div>

                          <DropdownMenu>
                            <DropdownMenuTrigger
                              onClick={(e) => e.stopPropagation()}
                              render={
                                <Button
                                  variant="ghost"
                                  size="icon"
                                  className="size-8 shrink-0 text-muted-foreground hover:text-foreground"
                                  aria-label="Topic options"
                                >
                                  <MoreVertical className="size-4" />
                                </Button>
                              }
                            />
                            <DropdownMenuContent align="end" className="w-40">
                              <DropdownMenuItem
                                onClick={(e) => {
                                  e.stopPropagation();
                                  handleSelectTopic(topic);
                                }}
                                className="cursor-pointer gap-2 text-xs"
                              >
                                <Eye className="size-3.5 text-muted-foreground" />
                                <span>Open Workspace</span>
                              </DropdownMenuItem>
                              <DropdownMenuItem
                                onClick={(e) => {
                                  e.stopPropagation();
                                  handleOpenEditTopic(topic);
                                }}
                                className="cursor-pointer gap-2 text-xs"
                              >
                                <Edit className="size-3.5 text-muted-foreground" />
                                <span>Edit Topic</span>
                              </DropdownMenuItem>
                              <DropdownMenuSeparator />
                              <DropdownMenuItem
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setTopicToDelete(topic);
                                }}
                                className="cursor-pointer gap-2 text-xs text-destructive focus:text-destructive"
                              >
                                <Trash2 className="size-3.5" />
                                <span>Delete Topic</span>
                              </DropdownMenuItem>
                            </DropdownMenuContent>
                          </DropdownMenu>
                        </div>

                        <p className="line-clamp-2 text-xs leading-relaxed text-muted-foreground">
                          {topic.description ||
                            "Collection of multiple choice questions."}
                        </p>
                      </div>

                      <div className="mt-5 flex items-center justify-between border-t border-border/60 pt-3 text-xs">
                        <Badge
                          variant="secondary"
                          className="gap-1 font-mono text-[11px]"
                        >
                          <CheckSquare className="size-3 text-primary" />
                          <span>
                            {topic.questionCount}{" "}
                            {topic.questionCount === 1 ? "MCQ" : "MCQs"}
                          </span>
                        </Badge>

                        <span className="flex items-center gap-1 text-xs font-medium text-primary transition-transform group-hover:translate-x-0.5">
                          <span>Open</span>
                          <ArrowRight className="size-3" />
                        </span>
                      </div>
                    </div>
                  ))}

                  {/* Add New Topic Quick Card */}
                  <button
                    type="button"
                    onClick={handleOpenCreateTopic}
                    className="flex min-h-[160px] cursor-pointer flex-col items-center justify-center gap-2 rounded-xl border border-dashed border-border/80 bg-card/40 p-8 text-muted-foreground transition-all hover:border-primary/50 hover:bg-card/70 hover:text-primary"
                  >
                    <div className="flex size-10 items-center justify-center rounded-lg bg-muted text-muted-foreground group-hover:text-primary">
                      <FolderPlus className="size-5" />
                    </div>
                    <span className="text-xs font-semibold text-foreground">
                      Add Main Topic
                    </span>
                    <span className="text-[11px] text-muted-foreground">
                      Create a new curriculum domain
                    </span>
                  </button>
                </div>
              )}
            </>
          ) : (
            /* LEVEL 2: TOPIC WORKSPACE WITH EXCLUSIVE TABLE */
            <>
              {/* Workspace Header Info Strip */}
              {/* <div className="flex flex-col gap-4 rounded-xl border border-border bg-card/60 p-4 shadow-2xs backdrop-blur-xs sm:flex-row sm:items-center sm:justify-between">
                <div className="flex items-center gap-3">
                  <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary">
                    <BookOpen className="size-5" />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h2 className="text-base font-bold text-foreground">
                        {selectedTopic.name}
                      </h2>
                      <Badge variant="secondary" className="font-mono text-xs">
                        {totalElements}{" "}
                        {totalElements === 1 ? "Question" : "Questions"}
                      </Badge>
                    </div>
                    <p className="text-xs text-muted-foreground">
                      {selectedTopic.description ||
                        `All multiple choice questions assigned to ${selectedTopic.name}`}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={handleBackToTopics}
                    className="gap-1.5 text-xs text-muted-foreground hover:text-foreground"
                  >
                    <ArrowLeft className="size-3.5" />
                    <span>Back to Topics</span>
                  </Button>

                  <Link
                    to="/admin/mcq/studio/new"
                    search={{ topic: selectedTopic.name }}
                  >
                    <Button
                      size="sm"
                      className="gap-2 text-xs font-semibold shadow-xs"
                    >
                      <Plus className="size-4" />
                      <span>Create MCQ</span>
                    </Button>
                  </Link>
                </div>
              </div> */}

              {/* Filters Bar: Search, Subtopic Input, Difficulty, Status */}
              <div className="flex flex-col gap-3 rounded-xl border border-border bg-card p-4 sm:flex-row sm:items-center sm:justify-between">
                <div className="flex flex-1 flex-wrap items-center gap-3">
                  {/* Question Search */}
                  <div className="relative min-w-[220px] flex-1">
                    <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
                    <Input
                      value={searchInput}
                      onChange={(e) => {
                        setSearchInput(e.target.value);
                        setPage(0);
                      }}
                      placeholder="Search questions by prompt..."
                      className="pl-9 text-xs"
                    />
                    {searchInput && (
                      <button
                        type="button"
                        onClick={() => setSearchInput("")}
                        className="absolute top-1/2 right-3 -translate-y-1/2 cursor-pointer text-xs text-muted-foreground hover:text-foreground"
                      >
                        ✕
                      </button>
                    )}
                  </div>

                  {/* Subtopic Text Input Filter */}
                  <div className="relative min-w-[200px] flex-1 sm:max-w-xs">
                    <Tag className="pointer-events-none absolute top-1/2 left-3 size-3.5 -translate-y-1/2 text-muted-foreground" />
                    <Input
                      value={subtopicInput}
                      onChange={(e) => {
                        setSubtopicInput(e.target.value);
                        setPage(0);
                      }}
                      placeholder="Filter by subtopic..."
                      className="pl-9 text-xs"
                    />
                    {subtopicInput && (
                      <button
                        type="button"
                        onClick={() => setSubtopicInput("")}
                        className="absolute top-1/2 right-3 -translate-y-1/2 cursor-pointer text-xs text-muted-foreground hover:text-foreground"
                      >
                        ✕
                      </button>
                    )}
                  </div>

                  {/* Difficulty Filter */}
                  <NativeSelect
                    value={difficultyFilter}
                    onChange={(e) => {
                      setDifficultyFilter(e.target.value);
                      setPage(0);
                    }}
                    size="sm"
                    className="w-32 text-xs"
                  >
                    <NativeSelectOption value="ALL">
                      Difficulty: All
                    </NativeSelectOption>
                    <NativeSelectOption value="EASY">Easy</NativeSelectOption>
                    <NativeSelectOption value="MEDIUM">
                      Medium
                    </NativeSelectOption>
                    <NativeSelectOption value="HARD">Hard</NativeSelectOption>
                  </NativeSelect>

                  {/* Type Filter */}
                  <NativeSelect
                    value={typeFilter}
                    onChange={(e) => {
                      setTypeFilter(e.target.value);
                      setPage(0);
                    }}
                    size="sm"
                    className="w-36 text-xs"
                  >
                    <NativeSelectOption value="ALL">
                      Type: All
                    </NativeSelectOption>
                    <NativeSelectOption value="SINGLE_CHOICE">
                      Single Choice
                    </NativeSelectOption>
                    <NativeSelectOption value="MULTIPLE_CHOICE">
                      Multiple Choice
                    </NativeSelectOption>
                    <NativeSelectOption value="BOOLEAN">
                      Boolean
                    </NativeSelectOption>
                  </NativeSelect>

                  {/* Status Filter */}
                  <NativeSelect
                    value={statusFilter}
                    onChange={(e) => {
                      setStatusFilter(e.target.value);
                      setPage(0);
                    }}
                    size="sm"
                    className="w-32 text-xs"
                  >
                    <NativeSelectOption value="ALL">
                      Status: All
                    </NativeSelectOption>
                    <NativeSelectOption value="PUBLISHED">
                      Published
                    </NativeSelectOption>
                    <NativeSelectOption value="DRAFT">Draft</NativeSelectOption>
                  </NativeSelect>
                </div>

                {hasActiveFilters && (
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={handleResetFilters}
                    className="self-start text-xs text-muted-foreground hover:text-foreground sm:self-auto"
                  >
                    Reset Filters
                  </Button>
                )}
              </div>

              {/* Paginated Question Table */}
              <div className="overflow-hidden rounded-xl border border-border bg-card">
                <Table>
                  <TableHeader>
                    <TableRow className="border-b border-border/80 hover:bg-transparent">
                      <TableHead className="w-[45%] text-xs font-semibold">
                        Question
                      </TableHead>
                      <TableHead className="text-xs font-semibold">
                        Subtopics
                      </TableHead>
                      <TableHead className="text-xs font-semibold">
                        Difficulty
                      </TableHead>
                      <TableHead className="text-xs font-semibold">
                        Type
                      </TableHead>
                      <TableHead className="text-xs font-semibold">
                        Status
                      </TableHead>
                      <TableHead className="text-right text-xs font-semibold">
                        Actions
                      </TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {loadingQuestions ? (
                      <TableRow>
                        <TableCell colSpan={6} className="h-44 text-center">
                          <div className="flex flex-col items-center justify-center gap-2 text-muted-foreground">
                            <Loader2 className="size-6 animate-spin text-primary" />
                            <span className="text-xs">
                              Loading questions in {selectedTopic.name}...
                            </span>
                          </div>
                        </TableCell>
                      </TableRow>
                    ) : items.length === 0 ? (
                      <TableRow>
                        <TableCell colSpan={6} className="h-48 text-center">
                          <div className="flex flex-col items-center justify-center gap-2 text-muted-foreground">
                            <HelpCircle className="mb-1 size-8 opacity-40" />
                            <p className="text-sm font-medium text-foreground">
                              {hasActiveFilters
                                ? "No questions match your current filters"
                                : `No questions in ${selectedTopic.name} yet`}
                            </p>
                            <p className="max-w-sm text-xs">
                              {hasActiveFilters
                                ? "Try resetting search or adjusting your subtopic and difficulty filters."
                                : `Get started by authoring the first MCQ under ${selectedTopic.name}.`}
                            </p>
                            {hasActiveFilters ? (
                              <Button
                                variant="outline"
                                size="sm"
                                onClick={handleResetFilters}
                                className="mt-3 text-xs"
                              >
                                Clear All Filters
                              </Button>
                            ) : (
                              <Link
                                to="/admin/mcq/studio/new"
                                search={{ topic: selectedTopic.name }}
                              >
                                <Button
                                  size="sm"
                                  className="mt-3 gap-1.5 text-xs font-semibold"
                                >
                                  <Plus className="size-3.5" />
                                  <span>Create First MCQ</span>
                                </Button>
                              </Link>
                            )}
                          </div>
                        </TableCell>
                      </TableRow>
                    ) : (
                      items.map((item) => (
                        <TableRow
                          key={item.id}
                          className="border-b border-border/40 transition-colors hover:bg-muted/40"
                        >
                          {/* Question Title & Snippet */}
                          <TableCell className="py-3">
                            <div className="flex flex-col gap-0.5">
                              <span className="line-clamp-1 text-sm font-semibold text-foreground">
                                {item.title}
                              </span>
                              <span className="line-clamp-1 text-xs font-normal text-muted-foreground">
                                {item.contentSnippet ||
                                  "No preview snippet available"}
                              </span>
                            </div>
                          </TableCell>

                          {/* Subtopics (Tags) */}
                          <TableCell className="py-3">
                            <div className="flex max-w-[200px] flex-wrap gap-1">
                              {item.tags && item.tags.length > 0 ? (
                                item.tags.map((tag) => (
                                  <Badge
                                    key={tag}
                                    variant="outline"
                                    onClick={() => setSubtopicInput(tag)}
                                    className="cursor-pointer bg-muted/40 px-1.5 py-0 text-[10px] font-normal transition-colors hover:bg-primary/10 hover:text-primary"
                                    title={`Click to filter by ${tag}`}
                                  >
                                    #{tag}
                                  </Badge>
                                ))
                              ) : (
                                <span className="text-xs text-muted-foreground/60 italic">
                                  —
                                </span>
                              )}
                            </div>
                          </TableCell>

                          {/* Difficulty */}
                          <TableCell className="py-3">
                            {getDifficultyBadge(item.difficulty)}
                          </TableCell>

                          {/* Type */}
                          <TableCell className="py-3">
                            <Badge
                              variant="outline"
                              className="font-mono text-[11px] capitalize"
                            >
                              {item.type.toLowerCase().replace("_", " ")}
                            </Badge>
                          </TableCell>

                          {/* Status Switch */}
                          <TableCell className="py-3">
                            <div className="flex items-center gap-2">
                              <Switch
                                checked={item.status === "PUBLISHED"}
                                onCheckedChange={() => handleToggleStatus(item)}
                                disabled={mutatingId === item.id}
                                className="scale-90"
                              />
                              <span
                                className={cn(
                                  "text-xs font-medium",
                                  item.status === "PUBLISHED"
                                    ? "text-emerald-600 dark:text-emerald-400"
                                    : "text-muted-foreground"
                                )}
                              >
                                {item.status === "PUBLISHED"
                                  ? "Published"
                                  : "Draft"}
                              </span>
                            </div>
                          </TableCell>

                          {/* Actions */}
                          <TableCell className="py-3 text-right">
                            <div className="flex items-center justify-end gap-1">
                              <Button
                                variant="ghost"
                                size="icon"
                                className="size-8 text-muted-foreground hover:text-foreground"
                                onClick={() => handleOpenPreview(item)}
                                title="Candidate Preview"
                              >
                                <Eye className="size-4" />
                              </Button>

                              <Link
                                to="/admin/mcq/studio/$questionId"
                                params={{ questionId: item.id }}
                              >
                                <Button
                                  variant="ghost"
                                  size="icon"
                                  className="size-8 text-muted-foreground hover:text-foreground"
                                  title="Edit MCQ"
                                >
                                  <Edit className="size-4" />
                                </Button>
                              </Link>

                              <Button
                                variant="ghost"
                                size="icon"
                                className="size-8 text-muted-foreground hover:text-destructive"
                                onClick={() => setQuestionToDelete(item)}
                                title="Delete Question"
                              >
                                <Trash2 className="size-4" />
                              </Button>
                            </div>
                          </TableCell>
                        </TableRow>
                      ))
                    )}
                  </TableBody>
                </Table>

                {/* Pagination Controls */}
                {totalElements > 0 && (
                  <div className="flex flex-col items-center justify-between gap-3 border-t border-border px-4 py-3 sm:flex-row">
                    <span className="text-xs text-muted-foreground">
                      Showing{" "}
                      <span className="font-medium text-foreground">
                        {page * pageSize + 1}
                      </span>{" "}
                      to{" "}
                      <span className="font-medium text-foreground">
                        {Math.min((page + 1) * pageSize, totalElements)}
                      </span>{" "}
                      of{" "}
                      <span className="font-medium text-foreground">
                        {totalElements}
                      </span>{" "}
                      questions
                    </span>

                    <div className="flex items-center gap-3">
                      <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                        <span>Rows per page:</span>
                        <NativeSelect
                          value={String(pageSize)}
                          onChange={(e) => {
                            setPageSize(Number(e.target.value));
                            setPage(0);
                          }}
                          size="sm"
                          className="h-7 w-16 text-xs"
                        >
                          <NativeSelectOption value="10">10</NativeSelectOption>
                          <NativeSelectOption value="20">20</NativeSelectOption>
                          <NativeSelectOption value="50">50</NativeSelectOption>
                        </NativeSelect>
                      </div>

                      <div className="flex items-center gap-1">
                        <span className="mr-1 text-xs text-muted-foreground">
                          Page {page + 1} of {Math.max(1, totalPages)}
                        </span>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => setPage((p) => Math.max(0, p - 1))}
                          disabled={isFirst}
                          className="h-7 px-2.5 text-xs"
                        >
                          Previous
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => setPage((p) => p + 1)}
                          disabled={isLast}
                          className="h-7 px-2.5 text-xs"
                        >
                          Next
                        </Button>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </>
          )}
        </div>
      </main>

      {/* Delete Question Confirmation Dialog */}
      <AlertDialog
        open={Boolean(questionToDelete)}
        onOpenChange={(open) => !open && setQuestionToDelete(null)}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete MCQ Question</AlertDialogTitle>
            <AlertDialogDescription>
              Are you sure you want to delete &ldquo;{questionToDelete?.title}
              &rdquo;? This action cannot be undone. Any question banks
              referencing this question will no longer include it.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={async () => {
                if (questionToDelete) {
                  await deleteQuestionMutation.mutateAsync(questionToDelete.id);
                  setQuestionToDelete(null);
                }
              }}
              className="text-destructive-foreground bg-destructive hover:bg-destructive/90"
            >
              Delete
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Delete Topic Confirmation Dialog */}
      <AlertDialog
        open={Boolean(topicToDelete)}
        onOpenChange={(open) => !open && setTopicToDelete(null)}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete Topic Domain</AlertDialogTitle>
            <AlertDialogDescription>
              Are you sure you want to delete &ldquo;{topicToDelete?.name}
              &rdquo;?
              {topicToDelete && topicToDelete.questionCount > 0 && (
                <span className="mt-2 block font-medium text-destructive">
                  Warning: There are {topicToDelete.questionCount} question(s)
                  associated with this topic.
                </span>
              )}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={async () => {
                if (topicToDelete) {
                  await deleteTopicMutation.mutateAsync(topicToDelete.id);
                  setTopicToDelete(null);
                }
              }}
              className="text-destructive-foreground bg-destructive hover:bg-destructive/90"
            >
              Delete Topic
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Create / Edit Topic Dialog */}
      <TopicFormDialog
        open={topicFormOpen}
        onOpenChange={setTopicFormOpen}
        topicToEdit={topicToEdit}
      />

      {/* MCQ Candidate Preview Modal */}
      <McqPreviewModal
        open={previewModalOpen}
        onOpenChange={setPreviewModalOpen}
        question={previewQuestion}
      />
    </div>
  );
}
