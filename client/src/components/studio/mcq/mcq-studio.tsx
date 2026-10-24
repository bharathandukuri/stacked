import { useState } from "react"
import { useNavigate } from "@tanstack/react-router"
import {
  ArrowLeft,
  Check,
  Plus,
  Trash2,
  Save,
  Loader2,
  Eye,
  Settings2,
  Tag,
  AlertCircle,
} from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import { Switch } from "@/components/ui/switch"
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select"
import { toast } from "@/components/ui/toast"
import { TopicManagerDialog } from "./topic-manager-dialog"
import { McqPreviewModal } from "./mcq-preview-modal"
import { McqMarkdownField } from "./mcq-markdown-field"
import {
  useMcqQuestion,
  useCreateMcqQuestion,
  useUpdateMcqQuestion,
  useMcqTopics,
} from "@/hooks/use-mcq"
import type {
  CreateMcqPayload,
  McqDifficulty,
  McqQuestionDetail,
  McqStatus,
  McqType,
  UpdateMcqPayload,
} from "@/types/mcq"
import { cn } from "@/lib/utils"

interface McqStudioProps {
  mode?: "create" | "edit"
  questionId?: string
}

function generateOptionId() {
  return "opt_" + Math.random().toString(36).substring(2, 9)
}

const DEFAULT_OPTIONS = [
  { id: generateOptionId(), text: "", explanation: "" },
  { id: generateOptionId(), text: "", explanation: "" },
  { id: generateOptionId(), text: "", explanation: "" },
  { id: generateOptionId(), text: "", explanation: "" },
]

export function McqStudio({ mode = "create", questionId }: McqStudioProps) {
  const isEditMode = mode === "edit" && Boolean(questionId)
  const { data: initialData, isLoading: isLoadingQuestion } = useMcqQuestion(
    isEditMode ? questionId : undefined
  )

  if (isEditMode && isLoadingQuestion) {
    return (
      <div className="flex h-screen items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    )
  }

  return (
    <McqStudioForm
      key={initialData?.id || "create-new"}
      initialData={initialData}
      isEditMode={isEditMode}
      questionId={questionId}
    />
  )
}

interface McqStudioFormProps {
  initialData?: McqQuestionDetail
  isEditMode: boolean
  questionId?: string
}

function McqStudioForm({
  initialData,
  isEditMode,
  questionId,
}: McqStudioFormProps) {
  const navigate = useNavigate()

  // Query and mutations
  const { data: topicList = [] } = useMcqTopics()
  const createMutation = useCreateMcqQuestion()
  const updateMutation = useUpdateMcqQuestion()

  // Form State initialized from initialData or defaults
  const [title, setTitle] = useState(initialData?.title || "")
  const [content, setContent] = useState(initialData?.content || "")
  const [type, setType] = useState<McqType>(initialData?.type || "SINGLE_CHOICE")
  const [difficulty, setDifficulty] = useState<McqDifficulty>(
    initialData?.difficulty || "MEDIUM"
  )
  const [status, setStatus] = useState<McqStatus>(
    initialData?.status || "DRAFT"
  )
  const [topics, setTopics] = useState<string[]>(initialData?.topics || [])
  const [tagInput, setTagInput] = useState("")
  const [tags, setTags] = useState<string[]>(initialData?.tags || [])
  const [options, setOptions] = useState<
    { id: string; text: string; explanation?: string | null }[]
  >(
    initialData?.options?.map((opt) => ({
      id: opt.id,
      text: opt.text,
      explanation: opt.explanation || "",
    })) || DEFAULT_OPTIONS
  )
  const [correctOptionIds, setCorrectOptionIds] = useState<string[]>(
    initialData?.correctOptionIds || []
  )
  const [explanation, setExplanation] = useState(
    initialData?.explanation || ""
  )
  const [shuffleOptions, setShuffleOptions] = useState(
    initialData?.shuffleOptions ?? true
  )
  const [imageFileIds, setImageFileIds] = useState<string[]>(
    initialData?.imageFileIds || []
  )

  // UI state
  const [topicManagerOpen, setTopicManagerOpen] = useState(false)
  const [previewModalOpen, setPreviewModalOpen] = useState(false)
  const [validationErrors, setValidationErrors] = useState<string[]>([])

  // Handle type change adjustments
  const handleTypeChange = (newType: McqType) => {
    setType(newType)
    if (newType === "BOOLEAN") {
      const boolOptions = [
        { id: "opt_true", text: "True", explanation: "" },
        { id: "opt_false", text: "False", explanation: "" },
      ]
      setOptions(boolOptions)
      setCorrectOptionIds(["opt_true"])
    } else if (newType === "SINGLE_CHOICE") {
      // If multiple were previously selected, pick only the first
      if (correctOptionIds.length > 1) {
        setCorrectOptionIds([correctOptionIds[0]])
      }
    }
  }

  // Option text change
  const handleOptionTextChange = (index: number, text: string) => {
    setOptions((prev) => {
      const updated = [...prev]
      updated[index] = { ...updated[index], text }
      return updated
    })
  }

  // Option explanation change
  const handleOptionExplanationChange = (index: number, explanation: string) => {
    setOptions((prev) => {
      const updated = [...prev]
      updated[index] = { ...updated[index], explanation }
      return updated
    })
  }

  // Toggle correct option
  const toggleCorrectOption = (optionId: string) => {
    if (type === "SINGLE_CHOICE" || type === "BOOLEAN") {
      setCorrectOptionIds([optionId])
    } else {
      setCorrectOptionIds((prev) =>
        prev.includes(optionId)
          ? prev.filter((id) => id !== optionId)
          : [...prev, optionId]
      )
    }
  }

  // Add option
  const addOption = () => {
    if (options.length >= 8) {
      toast.add({
        title: "Limit Reached",
        description: "Maximum 8 options allowed per question.",
        type: "error",
      })
      return
    }
    const newId = generateOptionId()
    setOptions((prev) => [...prev, { id: newId, text: "", explanation: "" }])
  }

  // Remove option
  const removeOption = (index: number) => {
    if (options.length <= 2) {
      toast.add({
        title: "Minimum Options Required",
        description: "A question must have at least 2 options.",
        type: "error",
      })
      return
    }
    const removedId = options[index].id
    setOptions((prev) => prev.filter((_, i) => i !== index))
    setCorrectOptionIds((prev) => prev.filter((id) => id !== removedId))
  }

  // Topic toggle
  const toggleTopic = (topicName: string) => {
    setTopics((prev) =>
      prev.includes(topicName)
        ? prev.filter((t) => t !== topicName)
        : [...prev, topicName]
    )
  }

  // Tag add/remove
  const handleAddTag = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" || e.key === ",") {
      e.preventDefault()
      const trimmed = tagInput.trim().replace(/^,+|,+$/g, "")
      if (trimmed && !tags.includes(trimmed)) {
        setTags((prev) => [...prev, trimmed])
        setTagInput("")
      }
    }
  }

  const removeTag = (tagToRemove: string) => {
    setTags((prev) => prev.filter((t) => t !== tagToRemove))
  }

  // Validation
  const validateForm = (): boolean => {
    const errors: string[] = []
    if (!title.trim()) errors.push("Question title is required.")
    if (!content.trim()) errors.push("Question prompt content is required.")
    if (options.length < 2) errors.push("At least 2 options are required.")

    const emptyOptionIndex = options.findIndex((opt) => !opt.text.trim())
    if (emptyOptionIndex !== -1) {
      errors.push(`Option ${emptyOptionIndex + 1} text cannot be blank.`)
    }

    if (correctOptionIds.length === 0) {
      errors.push("At least one option must be marked as correct.")
    } else if (
      (type === "SINGLE_CHOICE" || type === "BOOLEAN") &&
      correctOptionIds.length !== 1
    ) {
      errors.push("Single choice and boolean questions must have exactly 1 correct answer.")
    }

    setValidationErrors(errors)
    return errors.length === 0
  }

  // Submit handler
  const handleSave = async () => {
    if (!validateForm()) {
      toast.add({
        title: "Validation Error",
        description: "Please fix the highlighted errors before saving.",
        type: "error",
      })
      return
    }

    const cleanedOptions = options.map((opt) => ({
      id: opt.id,
      text: opt.text.trim(),
      explanation: opt.explanation?.trim() || null,
    }))

    if (isPublishedReadOnly) {
      toast.add({
        title: "Question is Published",
        description: "Only draft questions can be edited. Unpublish to DRAFT first.",
        type: "warning",
      })
      return
    }

    if (isEditMode && questionId) {
      const payload: UpdateMcqPayload = {
        title: title.trim(),
        content: content.trim(),
        type,
        difficulty,
        status,
        topics,
        tags,
        options: cleanedOptions,
        correctOptionIds,
        explanation: explanation.trim() || null,
        shuffleOptions,
        imageFileIds,
      }
      await updateMutation.mutateAsync({ id: questionId, payload })
      navigate({ to: "/admin/mcq" })
    } else {
      const payload: CreateMcqPayload = {
        title: title.trim(),
        content: content.trim(),
        type,
        difficulty,
        status,
        topics,
        tags,
        options: cleanedOptions,
        correctOptionIds,
        explanation: explanation.trim() || null,
        shuffleOptions,
        imageFileIds,
      }
      await createMutation.mutateAsync(payload)
      navigate({ to: "/admin/mcq" })
    }
  }

  const isPublishedReadOnly = isEditMode && initialData?.status === "PUBLISHED"

  // Build live preview object
  const previewQuestion: McqQuestionDetail = {
    id: questionId || "preview-id",
    title: title || "Untitled Question",
    content: content || "No question prompt provided yet.",
    type,
    difficulty,
    status,
    topics,
    tags,
    options: options.map((o) => ({
      id: o.id,
      text: o.text || "Empty option",
      explanation: o.explanation || null,
    })),
    correctOptionIds,
    explanation: explanation || null,
    shuffleOptions,
    imageFileIds,
  }

  const isSaving = createMutation.isPending || updateMutation.isPending

  return (
    <div className="flex min-h-screen flex-col bg-background text-foreground">
      {/* Top Navigation Bar */}
      <header className="sticky top-0 z-30 flex h-14 items-center justify-between border-b border-border bg-card/80 px-6 backdrop-blur">
        <div className="flex items-center gap-4">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => navigate({ to: "/admin/mcq" })}
            className="gap-1.5 text-muted-foreground hover:text-foreground"
          >
            <ArrowLeft className="h-4 w-4" />
            Back to MCQs
          </Button>
          <div className="h-4 w-px bg-border" />
          <span className="text-sm font-semibold tracking-tight text-foreground">
            {isEditMode ? "Edit MCQ Question" : "Create New MCQ Question"}
          </span>
          <Badge
            variant={
              status === "PUBLISHED"
                ? "default"
                : status === "DRAFT"
                ? "secondary"
                : "outline"
            }
            className="text-[10px] uppercase font-bold tracking-wider"
          >
            {status}
          </Badge>
        </div>

        <div className="flex items-center gap-2.5">
          <Button
            variant="outline"
            size="sm"
            onClick={() => setPreviewModalOpen(true)}
            className="gap-1.5 text-xs h-8"
          >
            <Eye className="h-3.5 w-3.5" />
            Candidate Preview
          </Button>

          <Button
            size="sm"
            onClick={handleSave}
            disabled={isSaving || isPublishedReadOnly}
            className="gap-1.5 text-xs h-8"
          >
            {isSaving ? (
              <Loader2 className="h-3.5 w-3.5 animate-spin" />
            ) : (
              <Save className="h-3.5 w-3.5" />
            )}
            {isEditMode ? "Save Changes" : "Create MCQ"}
          </Button>
        </div>
      </header>

      {/* Main Studio Body: Spacious Grid */}
      <main className="mx-auto w-full max-w-[1600px] grid flex-1 grid-cols-1 gap-6 p-6 xl:grid-cols-4">
        {/* Left Column (3 spans): Full Width Markdown Editors & Options */}
        <div className="space-y-6 xl:col-span-3">
          {isPublishedReadOnly && (
            <div className="rounded-lg border border-amber-500/50 bg-amber-500/10 p-3.5 text-xs text-amber-700 dark:text-amber-300 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <AlertCircle className="h-4 w-4 shrink-0 text-amber-600 dark:text-amber-400" />
                <span>
                  This MCQ is currently <strong>PUBLISHED</strong> and read-only. Only draft questions can be edited. Unpublish to DRAFT to make changes.
                </span>
              </div>
            </div>
          )}

          {/* Validation Banner */}
          {validationErrors.length > 0 && (
            <div className="rounded-lg border border-destructive/50 bg-destructive/10 p-3.5 text-xs text-destructive">
              <div className="flex items-center gap-1.5 font-semibold mb-1">
                <AlertCircle className="h-4 w-4" />
                Please correct the following:
              </div>
              <ul className="list-inside list-disc space-y-0.5">
                {validationErrors.map((err, i) => (
                  <li key={i}>{err}</li>
                ))}
              </ul>
            </div>
          )}

          {/* Title & Core Meta */}
          <div className="space-y-3 rounded-xl border border-border/70 bg-card p-5">
            <label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              Question Title / Header <span className="text-destructive">*</span>
            </label>
            <Input
              placeholder="e.g. Time Complexity of QuickSort Worst Case"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="text-base font-medium"
            />
          </div>

          {/* Full Markdown Editor for Question Prompt */}
          <McqMarkdownField
            id="mcq-question-prompt"
            label="Question Prompt"
            description="Full-width Monaco markdown editor with syntax highlighting, LaTeX math ($O(N \log N)$), tables, code snippets, and image attachments. Use tabs to preview."
            required
            value={content}
            onChange={setContent}
            onImageIdsChange={(ids) =>
              setImageFileIds((prev) => Array.from(new Set([...prev, ...ids])))
            }
            height={380}
            minHeight={240}
            defaultViewMode="edit"
            showViewSwitcher={true}
            placeholder="Write the full problem description, code snippet, math formula, or question statement here..."
          />

          {/* Options Builder with Markdown Editors */}
          <div className="space-y-4 rounded-xl border border-border/70 bg-card p-5">
            <div className="flex items-center justify-between border-b border-border/50 pb-3">
              <div>
                <h3 className="text-sm font-semibold text-foreground">
                  Answer Options & Correct Key
                </h3>
                <p className="text-xs text-muted-foreground">
                  {type === "MULTIPLE_CHOICE"
                    ? "Check all options that apply as correct answers. Each option supports full Markdown and LaTeX."
                    : "Select the indicator to designate the single correct answer. Each option supports full Markdown and LaTeX."}
                </p>
              </div>

              {type !== "BOOLEAN" && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={addOption}
                  className="gap-1.5 text-xs h-7"
                >
                  <Plus className="h-3.5 w-3.5" />
                  Add Option
                </Button>
              )}
            </div>

            <div className="space-y-4">
              {options.map((option, index) => {
                const isCorrect = correctOptionIds.includes(option.id)
                const letter = String.fromCharCode(65 + index)

                return (
                  <div
                    key={option.id}
                    className={cn(
                      "group rounded-lg border p-4 transition-all",
                      isCorrect
                        ? "border-emerald-500/80 bg-emerald-500/5 ring-1 ring-emerald-500/40"
                        : "border-border/60 bg-muted/20 hover:border-border"
                    )}
                  >
                    <div className="flex items-start gap-3.5">
                      {/* Checkbox or Radio selector button */}
                      <button
                        type="button"
                        onClick={() => toggleCorrectOption(option.id)}
                        className={cn(
                          "mt-1 flex h-7 w-7 shrink-0 items-center justify-center rounded-md border text-xs font-bold transition-all cursor-pointer",
                          isCorrect
                            ? "border-emerald-600 bg-emerald-600 text-white shadow-xs"
                            : "border-border/80 bg-card text-muted-foreground hover:border-primary/60"
                        )}
                        title="Click to mark as correct answer"
                      >
                        {isCorrect ? (
                          <Check className="h-4 w-4 stroke-[3]" />
                        ) : (
                          letter
                        )}
                      </button>

                      {/* Markdown Editor for Option Statement and Explanation */}
                      <div className="flex-1 space-y-3">
                        <div className="space-y-1">
                          <div className="flex items-center justify-between text-xs text-muted-foreground">
                            <span className="font-medium text-foreground">
                              Option {letter} Statement (Markdown)
                            </span>
                            {type === "BOOLEAN" && (
                              <span className="text-[11px] italic">
                                Locked for Boolean
                              </span>
                            )}
                          </div>

                          {type === "BOOLEAN" ? (
                            <Input
                              value={option.text}
                              disabled
                              className="bg-card font-medium text-sm"
                            />
                          ) : (
                            <McqMarkdownField
                              id={`mcq-opt-${option.id}`}
                              value={option.text}
                              onChange={(val) =>
                                handleOptionTextChange(index, val)
                              }
                              onImageIdsChange={(ids) =>
                                setImageFileIds((prev) =>
                                  Array.from(new Set([...prev, ...ids]))
                                )
                              }
                              height={130}
                              minHeight={90}
                              defaultViewMode="edit"
                              showViewSwitcher={true}
                              hideContainerBorder
                              placeholder={`Option ${letter} statement in Markdown, code, or math formula ($...$)...`}
                            />
                          )}
                        </div>

                        {/* Optional Explanation for Option */}
                        <div className="space-y-1 pt-1">
                          <span className="text-[11px] font-medium text-muted-foreground">
                            Option {letter} Rationale (Optional explanation shown upon review)
                          </span>
                          <McqMarkdownField
                            id={`mcq-opt-exp-${option.id}`}
                            value={option.explanation || ""}
                            onChange={(val) =>
                              handleOptionExplanationChange(index, val)
                            }
                            onImageIdsChange={(ids) =>
                              setImageFileIds((prev) =>
                                Array.from(new Set([...prev, ...ids]))
                              )
                            }
                            height={110}
                            minHeight={80}
                            defaultViewMode="edit"
                            showViewSwitcher={true}
                            hideContainerBorder
                            placeholder={`Optional explanation for Option ${letter}...`}
                          />
                        </div>
                      </div>

                      {/* Delete Option button */}
                      {type !== "BOOLEAN" && options.length > 2 && (
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => removeOption(index)}
                          className="h-8 w-8 p-0 text-muted-foreground hover:text-destructive"
                          title="Remove option"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </Button>
                      )}
                    </div>
                  </div>
                )
              })}
            </div>
          </div>

          {/* Full Markdown Editor for Comprehensive Solution Rationale */}
          <McqMarkdownField
            id="mcq-solution-explanation"
            label="Overall Solution Rationale & Explanation"
            description="In-depth conceptual breakdown presented during candidate solution reviews. Supports math formulas, code blocks, and diagrams."
            value={explanation}
            onChange={setExplanation}
            onImageIdsChange={(ids) =>
              setImageFileIds((prev) => Array.from(new Set([...prev, ...ids])))
            }
            height={260}
            minHeight={180}
            defaultViewMode="edit"
            showViewSwitcher={true}
            placeholder="Explain why the correct answer is right and why other options are incorrect. Markdown and formulas supported..."
          />
        </div>

        {/* Right Column (1 span): Settings & Metadata Sidebar */}
        <div className="space-y-6 xl:col-span-1">
          <div className="space-y-4 rounded-xl border border-border/70 bg-card p-5">
            <div className="flex items-center gap-2 border-b border-border/50 pb-3">
              <Settings2 className="h-4 w-4 text-primary" />
              <h3 className="text-sm font-semibold text-foreground">
                Question Configuration
              </h3>
            </div>

            {/* Question Type */}
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-foreground">
                Question Type
              </label>
              <NativeSelect
                value={type}
                onChange={(e) => handleTypeChange(e.target.value as McqType)}
              >
                <NativeSelectOption value="SINGLE_CHOICE">
                  Single Choice (One correct answer)
                </NativeSelectOption>
                <NativeSelectOption value="MULTIPLE_CHOICE">
                  Multiple Choice (Multiple correct answers)
                </NativeSelectOption>
                <NativeSelectOption value="BOOLEAN">
                  Boolean (True / False)
                </NativeSelectOption>
              </NativeSelect>
            </div>

            {/* Difficulty */}
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-foreground">
                Difficulty Level
              </label>
              <NativeSelect
                value={difficulty}
                onChange={(e) =>
                  setDifficulty(e.target.value as McqDifficulty)
                }
              >
                <NativeSelectOption value="EASY">Easy</NativeSelectOption>
                <NativeSelectOption value="MEDIUM">Medium</NativeSelectOption>
                <NativeSelectOption value="HARD">Hard</NativeSelectOption>
              </NativeSelect>
            </div>

            {/* Publication Status */}
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-foreground">
                Publication Status
              </label>
              <NativeSelect
                value={status}
                onChange={(e) => setStatus(e.target.value as McqStatus)}
              >
                <NativeSelectOption value="DRAFT">
                  Draft (Private / Work in progress)
                </NativeSelectOption>
                <NativeSelectOption value="PUBLISHED">
                  Published (Available for assessments)
                </NativeSelectOption>
              </NativeSelect>
            </div>

            {/* Shuffle Options */}
            <div className="flex items-center justify-between border-t border-border/50 pt-3">
              <div className="space-y-0.5">
                <div className="text-xs font-medium text-foreground">
                  Shuffle Options
                </div>
                <div className="text-[11px] text-muted-foreground">
                  Randomize option order for candidates
                </div>
              </div>
              <Switch
                checked={shuffleOptions}
                onCheckedChange={setShuffleOptions}
              />
            </div>
          </div>

          {/* Topics & Categorization */}
          <div className="space-y-4 rounded-xl border border-border/70 bg-card p-5">
            <div className="flex items-center justify-between border-b border-border/50 pb-3">
              <div className="flex items-center gap-2">
                <Tag className="h-4 w-4 text-primary" />
                <h3 className="text-sm font-semibold text-foreground">
                  Topic Tags
                </h3>
              </div>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setTopicManagerOpen(true)}
                className="text-xs text-primary hover:text-primary/80 h-7 px-2"
              >
                Manage Topics
              </Button>
            </div>

            {/* Available Topics Pill Selector */}
            <div className="space-y-2">
              <span className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
                Select from System Topics:
              </span>
              <div className="flex flex-wrap gap-1.5">
                {topicList.length === 0 ? (
                  <p className="text-xs text-muted-foreground italic">
                    No topics found. Click &quot;Manage Topics&quot; to create one.
                  </p>
                ) : (
                  topicList.map((t) => {
                    const isSelected = topics.includes(t.name)
                    return (
                      <button
                        key={t.id}
                        type="button"
                        onClick={() => toggleTopic(t.name)}
                        className={cn(
                          "inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium transition-all",
                          isSelected
                            ? "bg-primary text-primary-foreground shadow-xs"
                            : "bg-muted/60 text-muted-foreground hover:bg-muted"
                        )}
                      >
                        {isSelected && <Check className="h-3 w-3" />}
                        {t.name}
                      </button>
                    )
                  })
                )}
              </div>
            </div>

            {/* Custom Search Tags */}
            <div className="space-y-2 border-t border-border/50 pt-3">
              <span className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
                Custom Tags (press Enter to add)
              </span>
              <Input
                placeholder="e.g. spring-boot-4, reactive, dynamic-programming"
                value={tagInput}
                onChange={(e) => setTagInput(e.target.value)}
                onKeyDown={handleAddTag}
                className="h-8 text-xs"
              />
              {tags.length > 0 && (
                <div className="flex flex-wrap gap-1.5 pt-1">
                  {tags.map((tag) => (
                    <Badge
                      key={tag}
                      variant="outline"
                      className="gap-1 bg-accent/30 text-xs"
                    >
                      {tag}
                      <button
                        type="button"
                        onClick={() => removeTag(tag)}
                        className="text-muted-foreground hover:text-foreground"
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
      </main>

      {/* Topics Manager Modal */}
      <TopicManagerDialog
        open={topicManagerOpen}
        onOpenChange={setTopicManagerOpen}
      />

      {/* Candidate Preview Modal */}
      <McqPreviewModal
        question={previewQuestion}
        open={previewModalOpen}
        onOpenChange={setPreviewModalOpen}
      />
    </div>
  )
}

export default McqStudio
