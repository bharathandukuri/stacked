import { useState, useImperativeHandle, forwardRef, useCallback } from "react"
import {
  Sparkles,
  Code2,
  Database,
  Check,
  AlertCircle,
  Loader2,
  RotateCcw,
} from "lucide-react"
import FormText from "@/components/form/form-text"
import { FormBase, type FormControlProps } from "@/components/form/form-base"
import { Button } from "@/components/ui/button"
import {
  FieldGroup,
  FieldSet,
  FieldLegend,
  FieldDescription,
  FieldSeparator,
} from "@/components/ui/field"
import { useAppForm, useFieldContext } from "@/hooks/form/create-form-hooks"
import { cn } from "@/lib/utils"
import FormMarkdown from "@/components/form/form-markdown"
import { FormCheckboxGroup } from "@/components/form/form-checkbox"
import { FormHints } from "@/components/form/form-hints"
import { FormTags } from "@/components/form/form-tags"
import { Badge } from "@/components/ui/badge"
import {
  GENERIC_LANGUAGES,
  DATABASE_LANGUAGES,
  POPULAR_TOPIC_TAGS,
  getDefaultLanguageIds,
  type Difficulty,
  type ProblemType,
} from "../types"
import { useCodingProblemStore } from "../store"
import { codingProblemSchema, type CodingProblemValues } from "../schemas"
import { toast } from "@/components/ui/toast"
import { fileService } from "@/services/file-service"

const DIFFICULTIES = [
  {
    level: "EASY" as Difficulty,
    label: "Easy",
    description: "Standard prompts with linear constraints",
    dot: "bg-emerald-500",
    active:
      "border-emerald-500/50 bg-emerald-500/10 text-emerald-950 dark:text-emerald-200 ring-2 ring-emerald-500/20 shadow-xs",
  },
  {
    level: "MEDIUM" as Difficulty,
    label: "Medium",
    description: "Balanced algorithmic challenge and depth",
    dot: "bg-amber-500",
    active:
      "border-amber-500/50 bg-amber-500/10 text-amber-950 dark:text-amber-200 ring-2 ring-amber-500/20 shadow-xs",
  },
  {
    level: "HARD" as Difficulty,
    label: "Hard",
    description: "Strict limits, edge cases and optimization",
    dot: "bg-rose-500",
    active:
      "border-rose-500/50 bg-rose-500/10 text-rose-950 dark:text-rose-200 ring-2 ring-rose-500/20 shadow-xs",
  },
] as const

interface DifficultySelectorProps extends FormControlProps {
  className?: string
}

export function DifficultySelector(props: DifficultySelectorProps) {
  const field = useFieldContext<Difficulty>()
  const currentValue = field.state.value ?? "EASY"

  return (
    <FormBase {...props}>
      <div
        className={cn("grid grid-cols-1 gap-3 sm:grid-cols-3", props.className)}
      >
        {DIFFICULTIES.map((d) => {
          const isSelected = currentValue === d.level
          return (
            <button
              key={d.level}
              type="button"
              onClick={() => field.handleChange(d.level)}
              className={cn(
                "group flex h-[76px] cursor-pointer flex-col justify-between rounded-xl border p-3.5 text-left transition-all duration-150",
                isSelected
                  ? d.active
                  : "border-border/80 bg-card text-foreground hover:border-border hover:bg-muted/40"
              )}
            >
              <div className="flex w-full items-center justify-between">
                <span className="text-xs font-semibold tracking-tight">
                  {d.label}
                </span>
                <span
                  className={cn(
                    "size-2 rounded-full transition-transform group-hover:scale-125",
                    d.dot
                  )}
                />
              </div>

              <p className="line-clamp-2 text-[11px] leading-tight text-muted-foreground">
                {d.description}
              </p>
            </button>
          )
        })}
      </div>
    </FormBase>
  )
}

const PROBLEM_TYPES = [
  {
    type: "GENERIC" as ProblemType,
    label: "Generic Problem",
    badge: "Algorithms",
    description: "Standard algorithmic coding problem across languages",
    icon: Code2,
  },
  {
    type: "DATABASE" as ProblemType,
    label: "Database Problem",
    badge: "SQL",
    description: "SQL schemas, DDL/DML scripts, and queries",
    icon: Database,
  },
] as const

interface ProblemTypeSelectorProps extends FormControlProps {
  className?: string
  onTypeChange?: (type: ProblemType) => void
}

export function ProblemTypeSelector(props: ProblemTypeSelectorProps) {
  const field = useFieldContext<ProblemType>()
  const currentValue = field.state.value ?? "GENERIC"

  return (
    <FormBase {...props}>
      <div
        className={cn("grid grid-cols-1 gap-3 sm:grid-cols-2", props.className)}
      >
        {PROBLEM_TYPES.map((pt) => {
          const isSelected = currentValue === pt.type
          const Icon = pt.icon
          return (
            <button
              key={pt.type}
              type="button"
              onClick={() => {
                field.handleChange(pt.type)
                props.onTypeChange?.(pt.type)
              }}
              className={cn(
                "group flex h-19 cursor-pointer items-center gap-3 rounded-xl border p-3.5 text-left transition-all duration-150",
                isSelected
                  ? "border-primary/50 bg-primary/5 text-foreground shadow-xs ring-2 ring-primary/20"
                  : "border-border/80 bg-card text-foreground hover:border-border hover:bg-muted/40"
              )}
            >
              <div
                className={cn(
                  "flex size-8.5 shrink-0 items-center justify-center rounded-lg border transition-colors",
                  isSelected
                    ? "border-primary/40 bg-primary/15 text-primary"
                    : "border-border/60 bg-muted/50 text-muted-foreground group-hover:text-foreground"
                )}
              >
                <Icon className="size-4" />
              </div>

              <div className="min-w-0 flex-1">
                <div className="flex items-center justify-between gap-1">
                  <span className="truncate text-xs font-semibold tracking-tight text-foreground">
                    {pt.label}
                  </span>
                  <span
                    className={cn(
                      "rounded-full px-1.5 py-0.5 font-mono text-[10px]",
                      isSelected
                        ? "bg-primary/20 font-semibold text-primary"
                        : "bg-muted text-muted-foreground"
                    )}
                  >
                    {pt.badge}
                  </span>
                </div>
                <p className="mt-0.5 line-clamp-2 text-[11px] leading-tight text-muted-foreground">
                  {pt.description}
                </p>
              </div>
            </button>
          )
        })}
      </div>
    </FormBase>
  )
}

export interface CodingProblemFormHandle {
  submit: () => void
}

export interface CodingProblemFormProps {
  className?: string
  onSaved?: (values: CodingProblemValues) => void
}

export const CodingProblemForm = forwardRef<
  CodingProblemFormHandle,
  CodingProblemFormProps
>(function CodingProblemForm({ className, onSaved }, ref) {
  const values = useCodingProblemStore((state) => state.values)
  const uploadedImageIds = useCodingProblemStore(
    (state) => state.uploadedImageIds
  )
  const isSaving = useCodingProblemStore((state) => state.isSaving)
  const actions = useCodingProblemStore((state) => state.actions)

  const [validationError, setValidationError] = useState<string | null>(null)

  const form = useAppForm({
    defaultValues: values,
    validators: {
      onChange: codingProblemSchema,
    },
    onSubmit: async ({ value }) => {
      await saveProblem(value)
    },
  })

  const saveProblem = useCallback(
    async (formValues: CodingProblemValues) => {
      setValidationError(null)
      const result = codingProblemSchema.safeParse(formValues)
      if (!result.success) {
        const issue = result.error.issues[0]
        const errorMsg = issue
          ? `${issue.path.join(".") || "Field"}: ${issue.message}`
          : "Please complete all required fields."
        setValidationError(errorMsg)
        window.scrollTo({ top: 0, behavior: "smooth" })
        return
      }

      actions.setIsSaving(true)
      try {
        if (uploadedImageIds.length > 0) {
          await Promise.allSettled(
            uploadedImageIds.map((id) => fileService.makePermanent(id))
          )
        }
        actions.setValues(result.data)

        if (result.data.languages.length > 0) {
          actions.setActiveCodeLanguage(result.data.languages[0])
        }

        toast.add({
          title: "Problem Details Validated",
          description:
            "Moving to Step 2: Code Templates, Solutions & Validation.",
          type: "success",
        })

        actions.setCurrentStep(2)
        onSaved?.(result.data)
      } catch {
        toast.add({
          title: "Problem Details Saved",
          description: "Proceeding to code templates step.",
          type: "info",
        })
        actions.setCurrentStep(2)
      } finally {
        actions.setIsSaving(false)
      }
    },
    [actions, uploadedImageIds, onSaved]
  )

  useImperativeHandle(
    ref,
    () => ({
      submit: () => {
        saveProblem(form.state.values)
      },
    }),
    [form, saveProblem]
  )

  const handleReset = () => {
    actions.resetStore()
    form.reset()
    setValidationError(null)
    toast.add({
      title: "Form Reset",
      description: "All fields have been reset to default values.",
      type: "info",
    })
  }

  return (
    <div className={cn("w-full px-4 py-6 sm:px-6 md:px-8 lg:px-12", className)}>
      {validationError && (
        <div className="mb-6 flex items-center gap-2 rounded-xl border border-destructive/40 bg-destructive/10 p-4 text-xs font-medium text-destructive shadow-2xs">
          <AlertCircle className="size-4 shrink-0" />
          <span>{validationError}</span>
        </div>
      )}

      <form
        onSubmit={(e) => {
          e.preventDefault()
          e.stopPropagation()
          saveProblem(form.state.values)
        }}
        className="w-full"
      >
        <FieldGroup className="gap-8">
          <FieldSet className="gap-5">
            <div>
              <FieldLegend className="text-base font-semibold tracking-tight text-foreground">
                1. Problem Overview
              </FieldLegend>
              <FieldDescription>
                Title, slug, problem type, difficulty, and supported languages.
              </FieldDescription>
            </div>

            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              <form.AppField
                name="title"
                children={() => (
                  <FormText
                    label="Problem Title"
                    placeholder="e.g. Two Sum"
                    required
                  />
                )}
              />

              <form.AppField
                name="slug"
                children={() => (
                  <FormText
                    label="Problem Slug"
                    placeholder="e.g. two-sum"
                    required
                    labelAction={
                      <Button
                        type="button"
                        variant="ghost"
                        size="xs"
                        onClick={() => {
                          const titleVal = form.state.values.title
                          if (titleVal) {
                            const generated = titleVal
                              .toLowerCase()
                              .trim()
                              .replace(/[^\w\s-]/g, "")
                              .replace(/[\s_-]+/g, "-")
                              .replace(/^-+|-+$/g, "")
                            form.setFieldValue("slug", generated)
                          }
                        }}
                        className="h-5.5 cursor-pointer gap-1 px-2 text-[11px] text-primary"
                      >
                        <Sparkles className="size-3" /> Generate Slug
                      </Button>
                    }
                  />
                )}
              />
            </div>

            <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
              <form.AppField
                name="problemType"
                children={() => (
                  <ProblemTypeSelector
                    label="Problem Type"
                    required
                    onTypeChange={(newType) => {
                      form.setFieldValue(
                        "languages",
                        getDefaultLanguageIds(newType)
                      )
                    }}
                  />
                )}
              />

              <form.AppField
                name="difficulty"
                children={() => (
                  <DifficultySelector label="Difficulty" required />
                )}
              />
            </div>

            <form.Subscribe
              selector={(state) => state.values.problemType}
              children={(problemType) => {
                const languageOptions =
                  problemType === "DATABASE"
                    ? DATABASE_LANGUAGES
                    : GENERIC_LANGUAGES

                return (
                  <form.AppField
                    name="languages"
                    children={() => (
                      <FormCheckboxGroup
                        label="Supported Languages"
                        options={languageOptions.map((l) => ({
                          label: l.name,
                          value: l.id,
                        }))}
                        required
                      />
                    )}
                  />
                )
              }}
            />
          </FieldSet>

          <FieldSeparator />

          <FieldSet className="gap-4">
            <div className="flex items-center justify-between">
              <div>
                <FieldLegend className="text-base font-semibold tracking-tight text-foreground">
                  2. Problem Statement
                </FieldLegend>
                <FieldDescription>
                  Write description, examples, and constraints inside this
                  Markdown editor.
                </FieldDescription>
              </div>
              <Badge
                variant="secondary"
                className="font-mono text-[10px] text-muted-foreground"
              >
                KaTeX + GFM
              </Badge>
            </div>

            <form.AppField
              name="description"
              children={() => (
                <FormMarkdown
                  label="Statement"
                  placeholder="Write the full problem description, example inputs and outputs, and constraints here using Markdown..."
                  height={580}
                  required
                />
              )}
            />
          </FieldSet>

          <FieldSeparator />

          <FieldSet className="gap-4">
            <div>
              <FieldLegend className="text-base font-semibold tracking-tight text-foreground">
                3. Topic Tags
              </FieldLegend>
              <FieldDescription>
                Classify algorithms and data structures.
              </FieldDescription>
            </div>

            <form.AppField
              name="topics"
              children={() => (
                <FormTags
                  label="Topics"
                  popularOptions={POPULAR_TOPIC_TAGS}
                  placeholder="Add topic and press Enter..."
                />
              )}
            />
          </FieldSet>

          <FieldSeparator />

          <FieldSet className="gap-4">
            <div>
              <FieldLegend className="text-base font-semibold tracking-tight text-foreground">
                4. Hints
              </FieldLegend>
              <FieldDescription>
                Helpful guidance revealed sequentially when learners need a
                nudge.
              </FieldDescription>
            </div>

            <form.AppField
              name="hints"
              children={() => <FormHints label="Hints" />}
            />
          </FieldSet>

          <div className="sticky bottom-4 z-20 flex items-center justify-between rounded-xl border border-border/80 bg-background/95 p-4 shadow-lg backdrop-blur-md">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleReset}
              className="cursor-pointer gap-1.5 text-xs text-muted-foreground hover:text-foreground"
            >
              <RotateCcw className="size-3.5" />
              <span>Reset Form</span>
            </Button>

            <Button
              type="submit"
              size="sm"
              disabled={isSaving}
              className="cursor-pointer gap-1.5 px-6 text-xs font-semibold shadow-xs"
            >
              {isSaving ? (
                <Loader2 className="size-3.5 animate-spin" />
              ) : (
                <Check className="size-3.5" />
              )}
              <span>
                {isSaving ? "Validating..." : "Next: Code & Solutions →"}
              </span>
            </Button>
          </div>
        </FieldGroup>
      </form>
    </div>
  )
})

export default CodingProblemForm
