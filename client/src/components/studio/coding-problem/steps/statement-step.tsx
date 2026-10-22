import { useState, useCallback, useEffect, useMemo } from "react";
import {
  AlertCircle,
  Sparkles,
  Check,
  RotateCcw,
  CheckCircle2,
  XCircle,
  Loader2,
  Code2,
  Database,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { toast } from "@/components/ui/toast";
import { cn } from "@/lib/utils";
import FormText from "@/components/form/form-text";
import FormMarkdown from "@/components/form/form-markdown";
import { FormCheckboxGroup, type FormCheckboxOption } from "@/components/form/form-checkbox";
import { FormTags } from "@/components/form/form-tags";
import { FormHints } from "@/components/form/form-hints";
import { FormBase, type FormControlProps } from "@/components/form/form-base";
import { useAppForm, useFieldContext } from "@/hooks/form/create-form-hooks";
import { useStore } from "@tanstack/react-form";
import { InputGroupAddon } from "@/components/ui/input-group";
import {
  FieldGroup,
  FieldSet,
  FieldLegend,
  FieldDescription,
  FieldSeparator,
} from "@/components/ui/field";
import { useCodingProblemStore } from "../store";
import {
  statementStepSchema,
  type StatementStepValues,
  type Difficulty,
  type ProblemType,
} from "../schemas";
import { POPULAR_TOPIC_TAGS } from "../types";
import { codingProblemService } from "@/services/coding-problem-service";
import {
  getDatabaseLanguages,
  getLanguageCheckboxOptions,
} from "@/config/languages";

/* ==========================================================================
   Difficulty Selector Component
   ========================================================================== */

const DIFFICULTIES = [
  {
    level: "EASY" as Difficulty,
    label: "Easy",
    description: "Standard algorithmic prompts and constraints",
    dot: "bg-emerald-500",
    active:
      "border-emerald-500/50 bg-emerald-500/10 text-emerald-950 dark:text-emerald-200 ring-2 ring-emerald-500/20 shadow-xs",
  },
  {
    level: "MEDIUM" as Difficulty,
    label: "Medium",
    description: "Requires optimal data structures and design",
    dot: "bg-amber-500",
    active:
      "border-amber-500/50 bg-amber-500/10 text-amber-950 dark:text-amber-200 ring-2 ring-amber-500/20 shadow-xs",
  },
  {
    level: "HARD" as Difficulty,
    label: "Hard",
    description: "Strict limits, edge cases, and optimization",
    dot: "bg-rose-500",
    active:
      "border-rose-500/50 bg-rose-500/10 text-rose-950 dark:text-rose-200 ring-2 ring-rose-500/20 shadow-xs",
  },
] as const;

interface DifficultySelectorProps extends FormControlProps {
  className?: string;
}

function DifficultySelector(props: DifficultySelectorProps) {
  const field = useFieldContext<Difficulty>();
  const currentValue = field.state.value ?? "EASY";

  return (
    <FormBase {...props}>
      <div
        className={cn("grid grid-cols-1 gap-3 sm:grid-cols-3", props.className)}
      >
        {DIFFICULTIES.map((d) => {
          const isSelected = currentValue === d.level;
          return (
            <button
              key={d.level}
              type="button"
              onClick={() => field.handleChange(d.level)}
              className={cn(
                "group flex h-18 cursor-pointer flex-col justify-between rounded-xl border p-3 text-left transition-all duration-150 select-none",
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

              <p className="line-clamp-1 text-[11px] text-muted-foreground">
                {d.description}
              </p>
            </button>
          );
        })}
      </div>
    </FormBase>
  );
}

/* ==========================================================================
   Problem Type Selector (Generic vs Database)
   ========================================================================== */

const PROBLEM_TYPES = [
  {
    type: "GENERIC" as ProblemType,
    label: "Generic Problem",
    description: "Multi-language algorithms & data structures",
    icon: Code2,
  },
  {
    type: "DATABASE" as ProblemType,
    label: "Database Problem",
    description: "SQL schemas, DDL/DML, and queries",
    icon: Database,
  },
] as const;

interface ProblemTypeSelectorProps extends FormControlProps {
  className?: string;
  onTypeChange?: (type: ProblemType) => void;
}

function ProblemTypeSelector(props: ProblemTypeSelectorProps) {
  const field = useFieldContext<ProblemType>();
  const currentValue = field.state.value ?? "GENERIC";

  return (
    <FormBase {...props}>
      <div
        className={cn("grid grid-cols-1 gap-3 sm:grid-cols-2", props.className)}
      >
        {PROBLEM_TYPES.map((pt) => {
          const isSelected = currentValue === pt.type;
          const Icon = pt.icon;
          return (
            <button
              key={pt.type}
              type="button"
              onClick={() => {
                field.handleChange(pt.type);
                props.onTypeChange?.(pt.type);
              }}
              className={cn(
                "group flex h-18 cursor-pointer items-center gap-3 rounded-xl border p-3.5 text-left transition-all duration-150 select-none",
                isSelected
                  ? "border-primary/50 bg-primary/10 text-foreground ring-2 ring-primary/20 shadow-xs"
                  : "border-border/80 bg-card text-foreground hover:border-border hover:bg-muted/40"
              )}
            >
              <div
                className={cn(
                  "flex size-9 shrink-0 items-center justify-center rounded-lg border transition-colors",
                  isSelected
                    ? "border-primary/40 bg-primary/20 text-primary"
                    : "border-border/60 bg-muted/50 text-muted-foreground group-hover:text-foreground"
                )}
              >
                <Icon className="size-4.5" />
              </div>

              <div className="min-w-0 flex-1">
                <span className="truncate text-xs font-semibold tracking-tight text-foreground block">
                  {pt.label}
                </span>
                <p className="truncate text-[11px] text-muted-foreground">
                  {pt.description}
                </p>
              </div>
            </button>
          );
        })}
      </div>
    </FormBase>
  );
}

/* ==========================================================================
   Database Language Radio Selector (Single Select for SQL)
   ========================================================================== */

interface DatabaseLanguageRadioProps extends FormControlProps {
  languages: Array<{ id: string; name: string }>;
  className?: string;
}

function DatabaseLanguageRadio({
  languages,
  ...props
}: DatabaseLanguageRadioProps) {
  const field = useFieldContext<string[]>();
  const selectedId = field.state.value?.[0] ?? languages[0]?.id ?? "postgresql-16";

  return (
    <FormBase {...props}>
      <div
        className={cn("grid grid-cols-1 gap-3 sm:grid-cols-3", props.className)}
      >
        {languages.map((db) => {
          const isSelected = selectedId === db.id;
          return (
            <label
              key={db.id}
              onClick={() => field.handleChange([db.id])}
              className={cn(
                "group relative flex cursor-pointer items-center justify-between rounded-xl border p-3.5 text-left transition-all duration-150 select-none",
                isSelected
                  ? "border-primary/50 bg-primary/10 text-foreground ring-2 ring-primary/20 shadow-xs"
                  : "border-border/80 bg-card text-muted-foreground hover:border-border hover:bg-muted/40 hover:text-foreground"
              )}
            >
              <div className="flex items-center gap-2.5">
                <div
                  className={cn(
                    "flex size-4 shrink-0 items-center justify-center rounded-full border transition-colors",
                    isSelected
                      ? "border-primary bg-primary"
                      : "border-muted-foreground/50 bg-transparent group-hover:border-foreground"
                  )}
                >
                  {isSelected && (
                    <span className="size-1.5 rounded-full bg-primary-foreground" />
                  )}
                </div>
                <span className="text-xs font-semibold text-foreground tracking-tight">
                  {db.name}
                </span>
              </div>
            </label>
          );
        })}
      </div>
    </FormBase>
  );
}

/* ==========================================================================
   Statement Step (Step 1)
   ========================================================================== */

export interface StatementStepProps {
  className?: string;
  onNext?: () => void;
}

export function StatementStep({ className, onNext }: StatementStepProps) {
  const storeValues = useCodingProblemStore((state) => state.values);
  const setValues = useCodingProblemStore((state) => state.setValues);
  const setCurrentStep = useCodingProblemStore((state) => state.setCurrentStep);
  const editingProblemId = useCodingProblemStore(
    (state) => state.editingProblemId
  );
  const storeSetUniqueSlug = useCodingProblemStore(
    (state) => state.setUniqueSlug
  );

  // Supported languages stored directly in frontend configuration
  const databaseLanguages = useMemo(
    () => getDatabaseLanguages().map((l) => ({ id: l.id, name: l.name })),
    []
  );

  const genericCheckboxOptions: readonly FormCheckboxOption[] = useMemo(
    () => getLanguageCheckboxOptions("GENERIC"),
    []
  );

  // uniqueSlug boolean variable autofilled by debounced query
  const [uniqueSlug, setUniqueSlug] = useState<boolean | null>(null);
  const [isCheckingSlug, setIsCheckingSlug] = useState(false);

  const [validationErrors, setValidationErrors] = useState<string[] | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const form = useAppForm({
    defaultValues: {
      title: storeValues.title ?? "",
      slug: storeValues.slug ?? "",
      problemType: storeValues.problemType ?? "GENERIC",
      difficulty: storeValues.difficulty ?? "EASY",
      supportedLanguageIds:
        storeValues.supportedLanguageIds?.length > 0
          ? storeValues.supportedLanguageIds
          : ["javascript-node-20", "python-3.12", "java-21"],
      statement: storeValues.statement ?? "",
      topics: storeValues.topics ?? [],
      hints: storeValues.hints ?? [],
    },
    validators: {
      onChange: statementStepSchema,
    },
    onSubmit: async ({ value }) => {
      await handleSubmit(value);
    },
  });

  // Sync form state if editingProblemId is set or changed
  useEffect(() => {
    if (editingProblemId) {
      form.reset({
        title: storeValues.title ?? "",
        slug: storeValues.slug ?? "",
        problemType: storeValues.problemType ?? "GENERIC",
        difficulty: storeValues.difficulty ?? "EASY",
        supportedLanguageIds:
          storeValues.supportedLanguageIds?.length > 0
            ? storeValues.supportedLanguageIds
            : ["javascript-node-20", "python-3.12", "java-21"],
        statement: storeValues.statement ?? "",
        topics: storeValues.topics ?? [],
        hints: storeValues.hints ?? [],
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [editingProblemId]);

  // Watch slug reactively via store and trigger debounced query to set uniqueSlug
  const slugValue = useStore(form.store, (state) => state.values.slug);

  useEffect(() => {
    const trimmed = slugValue?.trim();
    const isValidSlugPattern = trimmed
      ? /^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(trimmed)
      : false;

    const timer = setTimeout(async () => {
      if (!trimmed || !isValidSlugPattern) {
        setUniqueSlug(null);
        storeSetUniqueSlug(null);
        setIsCheckingSlug(false);
        return;
      }

      setIsCheckingSlug(true);
      try {
        const isAvailable = await codingProblemService.checkSlug(
          trimmed,
          editingProblemId || undefined
        );
        setUniqueSlug(isAvailable);
        storeSetUniqueSlug(isAvailable);
      } catch (err) {
        console.error("Failed to check slug availability:", err);
        setUniqueSlug(null);
        storeSetUniqueSlug(null);
      } finally {
        setIsCheckingSlug(false);
      }
    }, 350);

    return () => clearTimeout(timer);
  }, [slugValue, editingProblemId, storeSetUniqueSlug]);

  // Generate slug from title
  const handleGenerateSlug = useCallback(() => {
    const titleVal = form.state.values.title;
    if (!titleVal || !titleVal.trim()) {
      toast.add({
        title: "Title Required",
        description: "Please enter a problem title first to generate a slug.",
        type: "info",
      });
      return;
    }
    const generated = titleVal
      .toLowerCase()
      .trim()
      .replace(/[^\w\s-]/g, "")
      .replace(/[\s_-]+/g, "-")
      .replace(/^-+|-+$/g, "");

    form.setFieldValue("slug", generated);
  }, [form]);

  // Validate form with Zod schema and update store if valid
  const handleSubmit = useCallback(
    async (formValues: StatementStepValues) => {
      setIsSubmitting(true);
      setValidationErrors(null);

      // Clean empty hint entries before validation
      const cleanedValues: StatementStepValues = {
        ...formValues,
        hints: formValues.hints.filter((h) => h.trim().length > 0),
      };

      // Check uniqueSlug constraint if already evaluated
      if (uniqueSlug === false) {
        setIsSubmitting(false);
        const errorMsg = `Problem slug '${cleanedValues.slug}' is already taken. Please choose a unique slug.`;
        setValidationErrors([errorMsg]);
        window.scrollTo({ top: 0, behavior: "smooth" });
        return;
      }

      const result = statementStepSchema.safeParse(cleanedValues);

      if (!result.success) {
        setIsSubmitting(false);
        const errorList = result.error.issues.map((issue) => {
          const field = issue.path.join(".");
          return field ? `${field}: ${issue.message}` : issue.message;
        });
        setValidationErrors(errorList);
        window.scrollTo({ top: 0, behavior: "smooth" });
        return;
      }

      // Valid: Update store state
      setValues({
        ...result.data,
        languages: result.data.supportedLanguageIds,
      });

      toast.add({
        title: "Statement Details Saved",
        description: "Moving to Step 2: Solutions and Validation.",
        type: "success",
      });

      setIsSubmitting(false);

      if (onNext) {
        onNext();
      } else {
        setCurrentStep(2);
      }
    },
    [uniqueSlug, setValues, setCurrentStep, onNext]
  );

  // Reset form to defaults
  const handleReset = useCallback(() => {
    form.reset();
    setValidationErrors(null);
    setUniqueSlug(null);
    storeSetUniqueSlug(null);
    toast.add({
      title: "Form Reset",
      description: "Statement form fields have been reset to defaults.",
      type: "info",
    });
  }, [form, storeSetUniqueSlug]);

  return (
    <div
      className={cn(
        "mx-auto w-full max-w-6xl px-4 py-8 sm:px-6 md:px-8 lg:px-12",
        className
      )}
    >
      {/* Top Validation Errors Banner */}
      {validationErrors && validationErrors.length > 0 && (
        <div className="mb-8 rounded-xl border border-destructive/40 bg-destructive/10 p-4 shadow-2xs">
          <div className="flex items-center gap-2 text-xs font-semibold text-destructive">
            <AlertCircle className="size-4 shrink-0" />
            <span>Please correct the following errors before continuing:</span>
          </div>
          <ul className="mt-2 list-inside list-disc space-y-1 text-xs text-destructive/90">
            {validationErrors.map((err, i) => (
              <li key={i}>{err}</li>
            ))}
          </ul>
        </div>
      )}

      {/* Main Vertical Form Layout */}
      <form
        onSubmit={(e) => {
          e.preventDefault();
          e.stopPropagation();
          handleSubmit(form.state.values);
        }}
        className="w-full"
      >
        <FieldGroup className="gap-7">
          {/* Header */}
          <div>
            <FieldLegend className="text-xl font-bold tracking-tight text-foreground">
              Problem Statement
            </FieldLegend>
            <FieldDescription className="mt-1 text-xs text-muted-foreground">
              Configure the problem title, unique slug, difficulty rating, execution languages, detailed Markdown statement, topic tags, and progressive hints.
            </FieldDescription>
          </div>

          <FieldSeparator />

          {/* 1. Title */}
          <FieldSet className="gap-2">
            <form.AppField
              name="title"
              children={() => (
                <FormText
                  label="Title"
                  placeholder="e.g. Two Sum"
                  required
                />
              )}
            />
          </FieldSet>

          {/* 2. Slug */}
          <FieldSet className="gap-2">
            <form.AppField
              name="slug"
              children={() => (
                <FormText
                  label="Slug"
                  placeholder="e.g. two-sum"
                  required
                  endAddon={
                    isCheckingSlug || uniqueSlug !== null ? (
                      <InputGroupAddon align="inline-end" className="pr-3">
                        {isCheckingSlug ? (
                          <span className="flex items-center gap-1.5 font-mono text-[11px] text-muted-foreground">
                            <Loader2 className="size-3.5 animate-spin text-primary" />
                            <span>Checking...</span>
                          </span>
                        ) : uniqueSlug === true ? (
                          <span className="flex items-center gap-1.5 font-mono text-[11px] font-medium text-emerald-600 dark:text-emerald-400">
                            <CheckCircle2 className="size-3.5 text-emerald-500" />
                            <span>Available</span>
                          </span>
                        ) : uniqueSlug === false ? (
                          <span className="flex items-center gap-1.5 font-mono text-[11px] font-medium text-destructive">
                            <XCircle className="size-3.5 text-destructive" />
                            <span>Taken</span>
                          </span>
                        ) : null}
                      </InputGroupAddon>
                    ) : null
                  }
                  labelAction={
                    <Button
                      type="button"
                      variant="ghost"
                      size="xs"
                      onClick={handleGenerateSlug}
                      className="h-5.5 cursor-pointer gap-1 px-2 text-[11px] text-primary"
                    >
                      <Sparkles className="size-3" /> Generate Slug
                    </Button>
                  }
                />
              )}
            />
          </FieldSet>

          {/* 3. Difficulty */}
          <FieldSet className="gap-2">
            <form.AppField
              name="difficulty"
              children={() => (
                <DifficultySelector label="Difficulty" required />
              )}
            />
          </FieldSet>

          {/* 4. Problem Type: Generic vs Database */}
          <FieldSet className="gap-2">
            <form.AppField
              name="problemType"
              children={() => (
                <ProblemTypeSelector
                  label="Problem Type"
                  required
                  onTypeChange={(newType) => {
                    if (newType === "DATABASE") {
                      const defaultDb = databaseLanguages[0]?.id || "postgresql-16";
                      form.setFieldValue("supportedLanguageIds", [defaultDb]);
                    } else {
                      form.setFieldValue("supportedLanguageIds", [
                        "javascript-node-20",
                        "python-3.12",
                        "java-21",
                      ]);
                    }
                  }}
                />
              )}
            />
          </FieldSet>

          {/* 5. Supported Languages: Checkbox for Generic, Radio for Database */}
          <form.Subscribe
            selector={(state) => state.values.problemType}
            children={(problemType) => (
              <FieldSet className="gap-2">
                {problemType === "DATABASE" ? (
                  <form.AppField
                    name="supportedLanguageIds"
                    children={() => (
                      <DatabaseLanguageRadio
                        label="Database Engine"
                        languages={databaseLanguages}
                        required
                      />
                    )}
                  />
                ) : (
                  <form.AppField
                    name="supportedLanguageIds"
                    children={() => (
                      <FormCheckboxGroup
                        label="Supported Languages"
                        options={genericCheckboxOptions}
                        gridClassName="grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5"
                        required
                      />
                    )}
                  />
                )}
              </FieldSet>
            )}
          />

          {/* 6. Statement */}
          <FieldSet className="gap-2">
            <form.AppField
              name="statement"
              children={() => (
                <FormMarkdown
                  label="Statement"
                  placeholder="## Description&#10;&#10;Given an array of integers `nums` and an integer `target`, return indices of the two numbers such that they add up to `target`.&#10;&#10;### Examples&#10;&#10;**Input:** nums = [2,7,11,15], target = 9&#10;**Output:** [0,1]&#10;&#10;### Constraints&#10;&#10;- $2 \le \text{nums.length} \le 10^4$&#10;- $-10^9 \le \text{nums}[i] \le 10^9$"
                  height={560}
                  showSessionImages={false}
                  required
                />
              )}
            />
          </FieldSet>

          {/* 7. Topics */}
          <FieldSet className="gap-2">
            <form.AppField
              name="topics"
              children={() => (
                <FormTags
                  label="Topics"
                  popularOptions={POPULAR_TOPIC_TAGS}
                  placeholder="Type a topic and press Enter..."
                  required
                />
              )}
            />
          </FieldSet>

          {/* 8. Hints */}
          <FieldSet className="gap-2">
            <form.AppField
              name="hints"
              children={() => (
                <FormHints
                  label="Hints"
                  placeholder="Provide progressive guidance for learners when stuck..."
                />
              )}
            />
          </FieldSet>

          {/* Sticky Bottom Actions Bar */}
          <div className="sticky bottom-4 z-20 flex items-center justify-between rounded-xl border border-border/80 bg-background/95 p-4 shadow-lg backdrop-blur-md">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleReset}
              className="cursor-pointer gap-1.5 text-xs text-muted-foreground hover:text-foreground"
            >
              <RotateCcw className="size-3.5" />
              <span>Reset</span>
            </Button>

            <Button
              type="submit"
              size="sm"
              disabled={isSubmitting || isCheckingSlug}
              className="cursor-pointer gap-1.5 px-6 text-xs font-semibold shadow-xs"
            >
              {isSubmitting ? (
                <Loader2 className="size-3.5 animate-spin" />
              ) : (
                <Check className="size-3.5" />
              )}
              <span>
                {isSubmitting ? "Validating..." : "Next: Solutions →"}
              </span>
            </Button>
          </div>
        </FieldGroup>
      </form>
    </div>
  );
}

export default StatementStep;
