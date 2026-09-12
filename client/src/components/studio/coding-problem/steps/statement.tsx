import { Sparkles, Code2, Database, ArrowRight } from "lucide-react"
import FormText from "@/components/form/form-text"
import { FormBase, type FormControlProps } from "@/components/form/form-base"
import { Button } from "@/components/ui/button"
import { FieldGroup } from "@/components/ui/field"
import { useAppForm, useFieldContext } from "@/hooks/form/create-form-hooks"
import { cn } from "@/lib/utils"
import FormMarkdown from "@/components/form/form-markdown"
import { FormCheckboxGroup } from "@/components/form/form-checkbox"
import { FormConstraints } from "@/components/form/form-constraints"
import { FormHints } from "@/components/form/form-hints"
import { FormTags } from "@/components/form/form-tags"
import {
  GENERIC_LANGUAGES,
  DATABASE_LANGUAGES,
  POPULAR_TOPIC_TAGS,
  POPULAR_COMPANIES,
  type Difficulty,
  type ProblemType,
} from "../types"

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
    description: "Strict time limits, edge cases and optimization",
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
                "group flex cursor-pointer flex-col items-start justify-between rounded-xl border p-4 text-left transition-all duration-150",
                isSelected
                  ? d.active
                  : "border-border/80 bg-card text-foreground hover:border-border hover:bg-muted/40"
              )}
            >
              <div className="flex w-full items-center justify-between">
                <span className="text-sm font-semibold tracking-tight">
                  {d.label}
                </span>
                <span
                  className={cn(
                    "size-2.5 rounded-full transition-transform group-hover:scale-110",
                    d.dot
                  )}
                />
              </div>

              <p className="mt-1.5 text-xs leading-normal text-muted-foreground">
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
    description:
      "Standard algorithmic coding problem with functions and parameters across languages",
    icon: Code2,
  },
  {
    type: "DATABASE" as ProblemType,
    label: "Database Problem",
    badge: "SQL",
    description:
      "Relational database queries, table schema definitions, and SQL assertions",
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
                "group flex cursor-pointer items-start gap-3 rounded-xl border p-4 text-left transition-all duration-150",
                isSelected
                  ? "border-primary/50 bg-primary/5 text-foreground shadow-xs ring-2 ring-primary/20"
                  : "border-border/80 bg-card text-foreground hover:border-border hover:bg-muted/40"
              )}
            >
              <div
                className={cn(
                  "flex size-9 shrink-0 items-center justify-center rounded-lg border transition-colors",
                  isSelected
                    ? "border-primary/40 bg-primary/15 text-primary"
                    : "border-border/60 bg-muted/50 text-muted-foreground group-hover:text-foreground"
                )}
              >
                <Icon className="size-4" />
              </div>

              <div className="min-w-0 flex-1">
                <div className="flex items-center justify-between gap-2">
                  <span className="text-sm font-semibold tracking-tight text-foreground">
                    {pt.label}
                  </span>
                  <span
                    className={cn(
                      "rounded-full px-2 py-0.5 font-mono text-[10px]",
                      isSelected
                        ? "bg-primary/20 font-semibold text-primary"
                        : "bg-muted text-muted-foreground"
                    )}
                  >
                    {pt.badge}
                  </span>
                </div>
                <p className="mt-1 text-xs leading-normal text-muted-foreground">
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

function Statement() {
  const form = useAppForm({
    defaultValues: {
      title: "",
      slug: "",
      difficulty: "EASY" as Difficulty,
      problemType: "GENERIC" as ProblemType,
      languages: ["javascript", "typescript", "python"] as string[],
      description: "",
      constraints: [
        "1 <= nums.length <= 10^5",
        "-10^9 <= nums[i] <= 10^9",
        "-10^9 <= target <= 10^9",
      ] as string[],
      hints: [
        "A brute force approach would search all pairs, which takes O(n^2) time.",
      ] as string[],
      topics: ["Array", "Hash Table"] as string[],
      companies: ["Google", "Amazon", "Meta"] as string[],
    },
  })

  return (
    <div className="w-full px-6 py-8 lg:px-12">
      <form
        onSubmit={(e) => {
          e.preventDefault()
          e.stopPropagation()
          form.handleSubmit()
        }}
        className="space-y-8"
      >
        <FieldGroup className="space-y-6">
          {/* Title & Slug */}
          <FieldGroup className="flex flex-col gap-4 sm:flex-row">
            <div className="flex-1">
              <form.AppField
                name="title"
                children={() => (
                  <FormText
                    label="Title"
                    placeholder="Problem title (e.g. Two Sum)"
                    type="text"
                    required
                  />
                )}
              />
            </div>

            <div className="flex-1">
              <form.AppField
                name="slug"
                children={() => (
                  <FormText
                    label="Slug"
                    placeholder="problem-slug (e.g. two-sum)"
                    type="text"
                    required
                    labelAction={
                      <Button
                        type="button"
                        variant="outline"
                        size="xs"
                        onClick={() => {
                          const titleVal = form.getFieldValue("title")
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
          </FieldGroup>

          {/* Difficulty */}
          <form.AppField
            name="difficulty"
            children={() => (
              <DifficultySelector
                label="Difficulty"
                required
              />
            )}
          />

          {/* Problem Type (Generic vs Database) */}
          <form.AppField
            name="problemType"
            children={() => (
              <ProblemTypeSelector
                label="Problem Type"
                required
                onTypeChange={(newType) => {
                  if (newType === "DATABASE") {
                    form.setFieldValue("languages", [
                      "mysql",
                      "postgresql",
                      "sqlite",
                    ])
                  } else {
                    form.setFieldValue("languages", [
                      "javascript",
                      "typescript",
                      "python",
                    ])
                  }
                }}
              />
            )}
          />

          {/* Supported Languages */}
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
                        value: l.id,
                        label: l.name,
                      }))}
                      card
                      showSelectAll
                      required
                    />
                  )}
                />
              )
            }}
          />

          {/* Problem Description */}
          <form.AppField
            name="description"
            children={() => (
              <FormMarkdown
                label="Problem Description"
                height="450px"
                required
              />
            )}
          />

          {/* Constraints */}
          <form.AppField
            name="constraints"
            children={() => (
              <FormConstraints
                label="Constraints"
                placeholder="e.g. 1 <= nums.length <= 10^5"
                required
              />
            )}
          />

          {/* Hints */}
          <form.AppField
            name="hints"
            children={() => (
              <FormHints
                label="Hints"
              />
            )}
          />

          {/* Topics */}
          <form.AppField
            name="topics"
            children={() => (
              <FormTags
                label="Topic Tags"
                popularOptions={POPULAR_TOPIC_TAGS}
                placeholder="Add topic tag..."
              />
            )}
          />

          {/* Company Tags */}
          <form.AppField
            name="companies"
            children={() => (
              <FormTags
                label="Company Tags"
                popularOptions={POPULAR_COMPANIES}
                placeholder="Add company..."
              />
            )}
          />

          {/* Submit Action */}
          <div className="flex items-center justify-end pt-4">
            <Button
              type="submit"
              size="default"
              className="cursor-pointer gap-2 px-6 font-semibold shadow-xs"
            >
              <span>Next: Test Cases & Examples</span>
              <ArrowRight className="size-4" />
            </Button>
          </div>
        </FieldGroup>
      </form>
    </div>
  )
}

export default Statement
