import { useState, useEffect } from "react"
import { useNavigate } from "@tanstack/react-router"
import {
  ArrowLeft,
  Plus,
  Trash2,
  Save,
  Loader2,
  Copy,
  ChevronUp,
  ChevronDown,
  Layers,
  Code2,
  CheckSquare,
  AlertCircle,
} from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select"
import { toast } from "@/components/ui/toast"
import { useIsAuthenticated, useIsInitialized } from "@/stores/auth-store"
import { initializeAuthSession } from "@/hooks/use-auth"
import { QuestionBankMarkdownField } from "./question-bank-markdown-field"
import {
  QuestionPickerDialog,
  type SelectedQuestionItem,
} from "./question-picker-dialog"
import {
  useQuestionBank,
  useCreateQuestionBank,
  useUpdateQuestionBank,
  useDuplicateQuestionBank,
  useQuestionBankCategories,
} from "@/hooks/use-question-banks"
import type {
  BankQuestionType,
  QuestionBankDetail,
  QuestionBankStatus,
} from "@/types/question-bank"

export interface StudioBankItem {
  questionType: BankQuestionType
  questionId: string
  order: number
  customPoints?: number | null
  effectivePoints: number
  title: string
  slug: string
  snippet?: string | null
  difficulty?: string | null
  topics?: string[]
}

interface QuestionBankStudioProps {
  mode?: "create" | "edit"
  bankId?: string
}

export function QuestionBankStudio({
  mode = "create",
  bankId,
}: QuestionBankStudioProps) {
  const navigate = useNavigate()
  const isAuthenticated = useIsAuthenticated()
  const isInitialized = useIsInitialized()

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

  const isEditMode = mode === "edit" && Boolean(bankId)
  const { data: initialData, isLoading: isLoadingBank } = useQuestionBank(
    isEditMode ? bankId : undefined
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

  if (isEditMode && isLoadingBank) {
    return (
      <div className="flex h-screen items-center justify-center">
        <Loader2 className="size-8 animate-spin text-primary" />
      </div>
    )
  }

  return (
    <QuestionBankStudioForm
      key={initialData?.id || "create-new"}
      initialData={initialData}
      isEditMode={isEditMode}
      bankId={bankId}
    />
  )
}

interface QuestionBankStudioFormProps {
  initialData?: QuestionBankDetail
  isEditMode: boolean
  bankId?: string
}

function QuestionBankStudioForm({
  initialData,
  isEditMode,
  bankId,
}: QuestionBankStudioFormProps) {
  const navigate = useNavigate()

  // Mutations & Queries
  const { data: existingCategories = [] } = useQuestionBankCategories()
  const createMutation = useCreateQuestionBank()
  const updateMutation = useUpdateQuestionBank()
  const duplicateMutation = useDuplicateQuestionBank()

  // Form State
  const [title, setTitle] = useState(initialData?.title || "")
  const [description, setDescription] = useState(initialData?.description || "")
  const [category, setCategory] = useState(initialData?.category || "")
  const [status, setStatus] = useState<QuestionBankStatus>(
    initialData?.status || "DRAFT"
  )
  const [tags, setTags] = useState<string[]>(initialData?.tags || [])
  const [tagInput, setTagInput] = useState("")

  // Curated Questions State
  const [items, setItems] = useState<StudioBankItem[]>(
    initialData?.items?.map((item, idx) => ({
      questionType: item.questionType,
      questionId: item.questionId,
      order: item.order || idx + 1,
      customPoints: item.customPoints,
      effectivePoints: item.effectivePoints,
      title: item.title,
      slug: item.slug,
      snippet: item.snippet,
      difficulty: item.difficulty,
      topics: item.topics,
    })) || []
  )

  // Dialog State
  const [pickerOpen, setPickerOpen] = useState(false)

  // Validation
  const [errors, setErrors] = useState<Record<string, string>>({})

  // Compute live breakdown stats
  const totalQuestions = items.length
  const mcqCount = items.filter((i) => i.questionType === "MCQ").length
  const codingCount = items.filter((i) => i.questionType === "CODING").length
  const totalPoints = items.reduce((sum, item) => {
    const pts =
      item.customPoints !== null &&
      item.customPoints !== undefined &&
      !isNaN(item.customPoints)
        ? Number(item.customPoints)
        : Number(item.effectivePoints || 0)
    return sum + pts
  }, 0)

  // Question Picker Handler
  const existingQuestionIds = new Set(items.map((i) => i.questionId))

  const handleAddQuestions = (newSelected: SelectedQuestionItem[]) => {
    setItems((prev) => {
      const startOrder = prev.length + 1
      const addedItems: StudioBankItem[] = newSelected.map((sel, idx) => ({
        questionType: sel.questionType,
        questionId: sel.questionId,
        order: startOrder + idx,
        customPoints: null,
        effectivePoints: sel.defaultPoints || 10,
        title: sel.title,
        slug: sel.slug,
        snippet: sel.snippet,
        difficulty: sel.difficulty,
        topics: sel.topics,
      }))
      return [...prev, ...addedItems]
    })
    toast.add({
      title: "Questions Added",
      description: `Added ${newSelected.length} question(s) to this bank.`,
      type: "success",
    })
  }

  // Reorder Item Handler
  const handleMoveItem = (index: number, direction: "up" | "down") => {
    const targetIndex = direction === "up" ? index - 1 : index + 1
    if (targetIndex < 0 || targetIndex >= items.length) return

    setItems((prev) => {
      const copy = [...prev]
      const temp = copy[index]
      copy[index] = copy[targetIndex]
      copy[targetIndex] = temp

      // Update sequential order numbers
      return copy.map((item, idx) => ({
        ...item,
        order: idx + 1,
      }))
    })
  }

  // Remove Item Handler
  const handleRemoveItem = (index: number) => {
    setItems((prev) => {
      const copy = prev.filter((_, idx) => idx !== index)
      return copy.map((item, idx) => ({
        ...item,
        order: idx + 1,
      }))
    })
  }

  // Custom Points Handler
  const handleCustomPointsChange = (index: number, value: string) => {
    setItems((prev) => {
      const copy = [...prev]
      if (value.trim() === "") {
        copy[index] = { ...copy[index], customPoints: null }
      } else {
        const parsed = parseFloat(value)
        copy[index] = {
          ...copy[index],
          customPoints: isNaN(parsed) ? null : parsed,
        }
      }
      return copy
    })
  }

  // Tag Handlers
  const handleAddTag = () => {
    const trimmed = tagInput.trim()
    if (!trimmed) return
    if (!tags.includes(trimmed)) {
      setTags([...tags, trimmed])
    }
    setTagInput("")
  }

  const handleRemoveTag = (tagToRemove: string) => {
    setTags(tags.filter((t) => t !== tagToRemove))
  }

  // Validate form
  const validate = (): boolean => {
    const errs: Record<string, string> = {}
    if (!title.trim()) {
      errs.title = "Question Bank title is required."
    }
    if (status === "PUBLISHED" && items.length === 0) {
      errs.items = "A published Question Bank must have at least one curated question."
    }
    setErrors(errs)
    return Object.keys(errs).length === 0
  }

  // Save / Submit
  const handleSave = async () => {
    if (!validate()) {
      const firstError = Object.values(errors)[0] || (status === "PUBLISHED" && items.length === 0 ? "A published Question Bank must have at least one curated question." : "Please fill in all required fields.")
      toast.add({
        title: "Validation Error",
        description: firstError,
        type: "error",
      })
      return
    }

    const payload = {
      title: title.trim(),
      description: description.trim() || undefined,
      category: category.trim() || undefined,
      status,
      tags: tags.length > 0 ? tags : undefined,
      items: items.map((item, idx) => ({
        questionType: item.questionType,
        questionId: item.questionId,
        order: idx + 1,
        customPoints: item.customPoints ?? null,
      })),
    }

    try {
      if (isEditMode && bankId) {
        await updateMutation.mutateAsync({
          idOrSlug: bankId,
          payload,
        })
      } else {
        const created = await createMutation.mutateAsync(payload)
        navigate({
          to: "/admin/questions/studio/$bankId",
          params: { bankId: created.id },
        })
      }
    } catch {
      // Toast notification is handled in mutation onError hook
    }
  }

  // Duplicate handler
  const handleDuplicate = async () => {
    if (!bankId) return
    try {
      const duplicated = await duplicateMutation.mutateAsync(bankId)
      navigate({
        to: "/admin/questions/studio/$bankId",
        params: { bankId: duplicated.id },
      })
    } catch {
      // Toast notification handled in mutation hook
    }
  }

  const isSaving = createMutation.isPending || updateMutation.isPending

  return (
    <div className="flex min-h-screen flex-col bg-background">
      {/* Top Navigation Bar */}
      <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-border bg-card/80 px-6 backdrop-blur-md">
        <div className="flex items-center gap-4">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => navigate({ to: "/admin/questions" })}
            className="gap-2 text-muted-foreground hover:text-foreground"
          >
            <ArrowLeft className="size-4" />
            <span>Question Banks</span>
          </Button>
          <div className="h-5 w-px bg-border" />
          <div>
            <h1 className="text-base font-bold text-foreground">
              {isEditMode ? "Edit Question Bank" : "Create Question Bank"}
            </h1>
            <p className="text-xs text-muted-foreground">
              {isEditMode ? initialData?.slug : "New reusable assessment bundle"}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          {isEditMode && (
            <Button
              variant="outline"
              size="sm"
              onClick={handleDuplicate}
              disabled={duplicateMutation.isPending}
              className="gap-1.5"
            >
              {duplicateMutation.isPending ? (
                <Loader2 className="size-4 animate-spin" />
              ) : (
                <Copy className="size-4" />
              )}
              <span>Duplicate</span>
            </Button>
          )}

          <Button
            size="sm"
            onClick={handleSave}
            disabled={isSaving}
            className="gap-2 px-4"
          >
            {isSaving ? (
              <Loader2 className="size-4 animate-spin" />
            ) : (
              <Save className="size-4" />
            )}
            <span>{isEditMode ? "Save Changes" : "Create Bank"}</span>
          </Button>
        </div>
      </header>

      {/* Main Studio Content: 2-Column Responsive Layout */}
      <main className="mx-auto flex w-full max-w-7xl flex-1 flex-col gap-6 p-6 lg:flex-row">
        {/* Left Column: Title, Description, Curated Questions */}
        <section className="flex flex-1 flex-col gap-6">
          {/* Title Card */}
          <div className="rounded-xl border border-border bg-card p-5 shadow-xs">
            <label
              htmlFor="bank-title"
              className="block text-xs font-semibold tracking-wider uppercase text-muted-foreground"
            >
              Question Bank Title <span className="text-destructive">*</span>
            </label>
            <Input
              id="bank-title"
              value={title}
              onChange={(e) => {
                setTitle(e.target.value)
                if (errors.title) setErrors((prev) => ({ ...prev, title: "" }))
              }}
              placeholder="e.g. Senior Backend Java & Spring Assessment Bank"
              className={`mt-2 text-base font-semibold ${
                errors.title ? "border-destructive ring-destructive/20" : ""
              }`}
            />
            {errors.title && (
              <p className="mt-1 text-xs text-destructive flex items-center gap-1">
                <AlertCircle className="size-3" />
                {errors.title}
              </p>
            )}
          </div>

          {/* Markdown Description */}
          <div className="rounded-xl border border-border bg-card p-5 shadow-xs">
            <QuestionBankMarkdownField
              label="Bank Description & Instructions"
              description="Outline syllabus coverage, candidate instructions, difficulty targets, or evaluation guidelines."
              value={description}
              onChange={setDescription}
              height={260}
            />
          </div>

          {/* Curated Questions Section */}
          <div className="flex flex-col gap-4 rounded-xl border border-border bg-card p-5 shadow-xs">
            <div className="flex flex-wrap items-center justify-between gap-3 border-b border-border pb-4">
              <div>
                <div className="flex items-center gap-2">
                  <h2 className="text-base font-bold text-foreground">
                    Curated Questions
                  </h2>
                  <Badge variant="secondary" className="text-xs font-semibold">
                    {totalQuestions}
                  </Badge>
                </div>
                <p className="text-xs text-muted-foreground mt-0.5">
                  Pre-configured questions bundled into this question bank.
                </p>
              </div>

              <Button
                size="sm"
                onClick={() => setPickerOpen(true)}
                className="gap-1.5"
              >
                <Plus className="size-4" />
                <span>Add Questions</span>
              </Button>
            </div>

            {/* Questions List or Empty State */}
            {items.length === 0 ? (
              <div className="flex flex-col items-center justify-center rounded-lg border border-dashed border-border py-12 text-center">
                <div className="flex size-12 items-center justify-center rounded-full bg-primary/10 text-primary">
                  <Layers className="size-6" />
                </div>
                <h3 className="mt-3 text-sm font-semibold text-foreground">
                  No questions in this bank yet
                </h3>
                <p className="mt-1 max-w-sm text-xs text-muted-foreground">
                  Pick prewritten MCQs and Coding Problems from your library to compose this reusable question bank.
                </p>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => setPickerOpen(true)}
                  className="mt-4 gap-2"
                >
                  <Plus className="size-4" />
                  <span>Add First Question</span>
                </Button>
              </div>
            ) : (
              <div className="flex flex-col gap-3">
                {items.map((item, index) => {
                  const isMcq = item.questionType === "MCQ"
                  const hasCustomPoints =
                    item.customPoints !== null &&
                    item.customPoints !== undefined &&
                    !isNaN(item.customPoints)

                  return (
                    <div
                      key={`${item.questionType}-${item.questionId}`}
                      className="group flex flex-col gap-3 rounded-lg border border-border bg-background p-4 transition-all hover:border-primary/40 hover:shadow-2xs md:flex-row md:items-center md:justify-between"
                    >
                      {/* Left: Reorder Controls + Order Number + Type Icon + Title */}
                      <div className="flex items-start gap-3 md:items-center">
                        {/* Up/Down buttons */}
                        <div className="flex flex-col gap-0.5">
                          <button
                            type="button"
                            onClick={() => handleMoveItem(index, "up")}
                            disabled={index === 0}
                            className="rounded p-0.5 text-muted-foreground hover:bg-muted hover:text-foreground disabled:opacity-30"
                            title="Move Up"
                          >
                            <ChevronUp className="size-3.5" />
                          </button>
                          <button
                            type="button"
                            onClick={() => handleMoveItem(index, "down")}
                            disabled={index === items.length - 1}
                            className="rounded p-0.5 text-muted-foreground hover:bg-muted hover:text-foreground disabled:opacity-30"
                            title="Move Down"
                          >
                            <ChevronDown className="size-3.5" />
                          </button>
                        </div>

                        {/* Order badge */}
                        <div className="flex size-7 items-center justify-center rounded-md bg-muted text-xs font-bold text-muted-foreground">
                          #{item.order}
                        </div>

                        {/* Question Info */}
                        <div className="flex flex-col gap-1">
                          <div className="flex flex-wrap items-center gap-2">
                            {isMcq ? (
                              <Badge
                                variant="outline"
                                className="border-purple-500/30 bg-purple-500/10 text-purple-600 dark:text-purple-400 gap-1 text-[11px]"
                              >
                                <CheckSquare className="size-3" />
                                MCQ
                              </Badge>
                            ) : (
                              <Badge
                                variant="outline"
                                className="border-blue-500/30 bg-blue-500/10 text-blue-600 dark:text-blue-400 gap-1 text-[11px]"
                              >
                                <Code2 className="size-3" />
                                Coding
                              </Badge>
                            )}

                            {item.difficulty && (
                              <Badge
                                variant="outline"
                                className={`text-[10px] ${
                                  item.difficulty === "HARD"
                                    ? "border-rose-500/30 bg-rose-500/10 text-rose-600 dark:text-rose-400"
                                    : item.difficulty === "MEDIUM"
                                      ? "border-amber-500/30 bg-amber-500/10 text-amber-600 dark:text-amber-400"
                                      : "border-emerald-500/30 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
                                }`}
                              >
                                {item.difficulty}
                              </Badge>
                            )}

                            <span className="font-semibold text-foreground text-sm line-clamp-1">
                              {item.title}
                            </span>
                          </div>

                          {item.topics && item.topics.length > 0 && (
                            <div className="flex flex-wrap gap-1">
                              {item.topics.slice(0, 4).map((topic) => (
                                <span
                                  key={topic}
                                  className="text-[10px] text-muted-foreground bg-muted/60 px-1.5 py-0.5 rounded"
                                >
                                  {topic}
                                </span>
                              ))}
                            </div>
                          )}
                        </div>
                      </div>

                      {/* Right: Points override + Remove button */}
                      <div className="flex items-center gap-3 self-end md:self-center">
                        <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                          <label
                            htmlFor={`pts-${index}`}
                            className="whitespace-nowrap"
                          >
                            Pts:
                          </label>
                          <Input
                            id={`pts-${index}`}
                            type="number"
                            min="0"
                            step="0.5"
                            value={
                              hasCustomPoints ? String(item.customPoints) : ""
                            }
                            placeholder={String(item.effectivePoints || 10)}
                            onChange={(e) =>
                              handleCustomPointsChange(index, e.target.value)
                            }
                            className="h-8 w-20 text-xs text-center"
                            title={`Default points: ${item.effectivePoints}. Leave empty to use default.`}
                          />
                        </div>

                        <Button
                          variant="ghost"
                          size="icon-sm"
                          onClick={() => handleRemoveItem(index)}
                          className="text-muted-foreground hover:bg-destructive/10 hover:text-destructive"
                          title="Remove from bank"
                        >
                          <Trash2 className="size-4" />
                        </Button>
                      </div>
                    </div>
                  )
                })}
              </div>
            )}
          </div>
        </section>

        {/* Right Column: Settings, Metadata, Summary */}
        <aside className="flex w-full flex-col gap-6 lg:w-80">
          {/* Status & Category Settings */}
          <div className="rounded-xl border border-border bg-card p-5 shadow-xs">
            <h3 className="text-sm font-bold text-foreground">Bank Settings</h3>
            <div className="mt-4 space-y-4">
              {/* Status */}
              <div>
                <label className="text-xs font-medium text-muted-foreground">
                  Status
                </label>
                <NativeSelect
                  value={status}
                  onChange={(e) =>
                    setStatus(e.target.value as QuestionBankStatus)
                  }
                  className="mt-1 w-full"
                >
                  <NativeSelectOption value="DRAFT">Draft</NativeSelectOption>
                  <NativeSelectOption value="PUBLISHED">Published</NativeSelectOption>
                  <NativeSelectOption value="ARCHIVED">Archived</NativeSelectOption>
                </NativeSelect>
              </div>

              {/* Category */}
              <div>
                <label className="text-xs font-medium text-muted-foreground">
                  Category
                </label>
                <div className="mt-1 space-y-1.5">
                  <Input
                    value={category}
                    onChange={(e) => setCategory(e.target.value)}
                    placeholder="e.g. Backend Development"
                    className="text-xs"
                    list="categories-list"
                  />
                  {existingCategories.length > 0 && (
                    <datalist id="categories-list">
                      {existingCategories.map((cat) => (
                        <option key={cat} value={cat} />
                      ))}
                    </datalist>
                  )}
                  {existingCategories.length > 0 && !category && (
                    <div className="flex flex-wrap gap-1">
                      {existingCategories.slice(0, 4).map((cat) => (
                        <button
                          key={cat}
                          type="button"
                          onClick={() => setCategory(cat)}
                          className="rounded bg-muted/60 px-1.5 py-0.5 text-[10px] text-muted-foreground hover:bg-primary/10 hover:text-primary transition-colors"
                        >
                          {cat}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              </div>

              {/* Tags */}
              <div>
                <label className="text-xs font-medium text-muted-foreground">
                  Tags
                </label>
                <div className="mt-1 flex items-center gap-1.5">
                  <Input
                    value={tagInput}
                    onChange={(e) => setTagInput(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") {
                        e.preventDefault()
                        handleAddTag()
                      }
                    }}
                    placeholder="Add tag and hit Enter..."
                    className="text-xs"
                  />
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={handleAddTag}
                  >
                    <Plus className="size-3.5" />
                  </Button>
                </div>
                {tags.length > 0 && (
                  <div className="mt-2 flex flex-wrap gap-1.5">
                    {tags.map((tag) => (
                      <Badge
                        key={tag}
                        variant="secondary"
                        className="flex items-center gap-1 text-xs"
                      >
                        <span>{tag}</span>
                        <button
                          type="button"
                          onClick={() => handleRemoveTag(tag)}
                          className="hover:text-destructive"
                        >
                          ×
                        </button>
                      </Badge>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Composition Breakdown Card */}
          <div className="rounded-xl border border-border bg-card p-5 shadow-xs">
            <h3 className="text-sm font-bold text-foreground">Composition Summary</h3>
            <p className="text-xs text-muted-foreground mt-0.5">
              Live statistics for this question bank bundle.
            </p>

            <div className="mt-4 space-y-3">
              <div className="flex items-center justify-between text-xs">
                <span className="text-muted-foreground">Total Questions</span>
                <span className="font-bold text-foreground text-sm">
                  {totalQuestions}
                </span>
              </div>

              <div className="flex items-center justify-between text-xs">
                <span className="flex items-center gap-1.5 text-muted-foreground">
                  <CheckSquare className="size-3.5 text-purple-500" />
                  Multiple Choice (MCQ)
                </span>
                <span className="font-semibold text-foreground">
                  {mcqCount}
                </span>
              </div>

              <div className="flex items-center justify-between text-xs">
                <span className="flex items-center gap-1.5 text-muted-foreground">
                  <Code2 className="size-3.5 text-blue-500" />
                  Coding Problems
                </span>
                <span className="font-semibold text-foreground">
                  {codingCount}
                </span>
              </div>

              {/* Progress proportion bar */}
              {totalQuestions > 0 && (
                <div className="h-2 w-full overflow-hidden rounded-full bg-muted flex">
                  <div
                    className="bg-purple-500 transition-all"
                    style={{
                      width: `${(mcqCount / totalQuestions) * 100}%`,
                    }}
                    title={`${mcqCount} MCQs`}
                  />
                  <div
                    className="bg-blue-500 transition-all"
                    style={{
                      width: `${(codingCount / totalQuestions) * 100}%`,
                    }}
                    title={`${codingCount} Coding Problems`}
                  />
                </div>
              )}

              <div className="border-t border-border pt-3 flex items-center justify-between text-xs">
                <span className="font-semibold text-foreground">Total Score / Points</span>
                <Badge variant="outline" className="font-bold text-primary">
                  {totalPoints} pts
                </Badge>
              </div>
            </div>
          </div>
        </aside>
      </main>

      {/* Question Picker Dialog */}
      <QuestionPickerDialog
        open={pickerOpen}
        onOpenChange={setPickerOpen}
        existingQuestionIds={existingQuestionIds}
        onAddQuestions={handleAddQuestions}
      />
    </div>
  )
}
