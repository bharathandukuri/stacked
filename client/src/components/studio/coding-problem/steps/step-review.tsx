import { useState, useMemo, useCallback } from "react";
import { useNavigate } from "@tanstack/react-router";
import {
  ArrowLeft,
  Check,
  Loader2,
  Sparkles,
  Code2,
  FileText,
  FlaskConical,
  ShieldCheck,
  Terminal,
  HelpCircle,
  Clock,
  Cpu,
  Lock,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { toast } from "@/components/ui/toast";
import { MarkdownPreview } from "@/components/markdown-preview/markdown-preview";
import { MonacoEditor } from "@/components/monaco-editor";
import { LineNumberedViewer } from "@/components/line-numbered-viewer";
import { cn } from "@/lib/utils";
import { useCodingProblemStore } from "../store";
import {
  getLanguageById,
  resolveReferenceSolutionFileName,
  resolveVerificationScriptFileName,
  resolveGeneratorScriptFileName,
} from "@/config/languages";
import {
  codingProblemService,
  buildCreateCodingProblemRequest,
} from "@/services/coding-problem-service";

export interface StepReviewProps {
  onBack?: () => void;
  onSuccess?: () => void;
  className?: string;
}

export function StepReview({ onBack, onSuccess, className }: StepReviewProps) {
  const navigate = useNavigate();

  const values = useCodingProblemStore((state) => state.values);
  const starterCodes = useCodingProblemStore((state) => state.starterCodes);
  const referenceSolutionLanguage = useCodingProblemStore(
    (state) => state.referenceSolutionLanguage
  );
  const referenceSolution = useCodingProblemStore(
    (state) => state.referenceSolution
  );
  const requireVerificationScript = useCodingProblemStore(
    (state) => state.requireVerificationScript
  );
  const verificationScriptLanguage = useCodingProblemStore(
    (state) => state.verificationScriptLanguage
  );
  const verificationScript = useCodingProblemStore(
    (state) => state.verificationScript
  );
  const sampleTestcases = useCodingProblemStore(
    (state) => state.sampleTestcases
  );
  const hiddenTestcases = useCodingProblemStore(
    (state) => state.hiddenTestcases
  );
  const generatorLanguage = useCodingProblemStore(
    (state) => state.generatorLanguage
  );
  const generatorScript = useCodingProblemStore(
    (state) => state.generatorScript
  );
  const generatorStdin = useCodingProblemStore(
    (state) => state.generatorStdin
  );
  const generatorDelimiter = useCodingProblemStore(
    (state) => state.generatorDelimiter
  );
  const executionConstraints = useCodingProblemStore(
    (state) => state.executionConstraints
  );
  const uploadedImageIds = useCodingProblemStore(
    (state) => state.uploadedImageIds
  );
  const isSaving = useCodingProblemStore((state) => state.isSaving);
  const mode = useCodingProblemStore((state) => state.mode);
  const editingProblemId = useCodingProblemStore(
    (state) => state.editingProblemId
  );
  const actions = useCodingProblemStore((state) => state.actions);

  const [activeTab, setActiveTab] = useState<
    "statement" | "solutions" | "testcases" | "constraints"
  >("statement");

  // Unified Code Inspector state
  const [codeInspectorTab, setCodeInspectorTab] = useState<
    "reference" | "starter" | "verification" | "generator"
  >("reference");

  const [selectedStarterLang, setSelectedStarterLang] = useState<string>(
    values.supportedLanguageIds[0] || "javascript-node-20"
  );

  const [testcaseReviewMode, setTestcaseReviewMode] = useState<
    "sample" | "hidden"
  >("sample");
  const [selectedSampleIndex, setSelectedSampleIndex] = useState(0);
  const [selectedHiddenIndex, setSelectedHiddenIndex] = useState(0);

  const isDatabase = values.problemType === "DATABASE";
  const supportedLanguages = useMemo(
    () =>
      values.supportedLanguageIds.length > 0
        ? values.supportedLanguageIds
        : ["javascript-node-20"],
    [values.supportedLanguageIds]
  );

  // Compute active code and language for the single unified Monaco editor
  const { activeCodeValue, activeCodeLang, activeFileName, activeBadgeName } =
    useMemo(() => {
      if (codeInspectorTab === "starter") {
        const langConfig = getLanguageById(selectedStarterLang);
        return {
          activeCodeValue: starterCodes[selectedStarterLang] || "",
          activeCodeLang: langConfig?.monacoLanguage || "python",
          activeFileName: resolveReferenceSolutionFileName(selectedStarterLang),
          activeBadgeName: langConfig?.name || selectedStarterLang,
        };
      }

      if (codeInspectorTab === "verification") {
        const langConfig = getLanguageById(verificationScriptLanguage);
        return {
          activeCodeValue: verificationScript || "",
          activeCodeLang: langConfig?.monacoLanguage || "python",
          activeFileName: resolveVerificationScriptFileName(
            verificationScriptLanguage
          ),
          activeBadgeName: langConfig?.name || verificationScriptLanguage,
        };
      }

      if (codeInspectorTab === "generator") {
        const langConfig = getLanguageById(generatorLanguage);
        return {
          activeCodeValue: generatorScript || "",
          activeCodeLang: langConfig?.monacoLanguage || "python",
          activeFileName: resolveGeneratorScriptFileName(generatorLanguage),
          activeBadgeName: langConfig?.name || generatorLanguage,
        };
      }

      // Default: "reference"
      const langConfig = getLanguageById(referenceSolutionLanguage);
      return {
        activeCodeValue: referenceSolution || "",
        activeCodeLang: langConfig?.monacoLanguage || "python",
        activeFileName: resolveReferenceSolutionFileName(
          referenceSolutionLanguage
        ),
        activeBadgeName: langConfig?.name || referenceSolutionLanguage,
      };
    }, [
      codeInspectorTab,
      selectedStarterLang,
      starterCodes,
      verificationScript,
      verificationScriptLanguage,
      generatorScript,
      generatorLanguage,
      referenceSolution,
      referenceSolutionLanguage,
    ]);

  // =========================================================================
  // Final Submission to POST /api/coding-problem
  // =========================================================================
  const handleSubmitProblem = useCallback(async () => {
    // 1. Validation Checks
    if (!values.title?.trim()) {
      actions.setCurrentStep(1);
      toast.add({
        title: "Validation Error",
        description: "Problem title is required.",
        type: "error",
      });
      return;
    }

    if (!values.statement?.trim()) {
      actions.setCurrentStep(1);
      toast.add({
        title: "Validation Error",
        description: "Problem statement cannot be empty.",
        type: "error",
      });
      return;
    }

    if (!referenceSolution || !referenceSolution.trim()) {
      actions.setCurrentStep(2);
      toast.add({
        title: "Validation Error",
        description: "Reference solution is required.",
        type: "error",
      });
      return;
    }

    if (
      requireVerificationScript &&
      (!verificationScript || !verificationScript.trim())
    ) {
      actions.setCurrentStep(2);
      toast.add({
        title: "Validation Error",
        description: "Verification script cannot be empty when enabled.",
        type: "error",
      });
      return;
    }

    if (!sampleTestcases || sampleTestcases.length === 0) {
      actions.setCurrentStep(2);
      toast.add({
        title: "Validation Error",
        description: "At least one sample testcase is required.",
        type: "error",
      });
      return;
    }

    if (!hiddenTestcases || hiddenTestcases.length === 0) {
      actions.setCurrentStep(3);
      toast.add({
        title: "Validation Error",
        description: "At least one hidden testcase is required.",
        type: "error",
      });
      return;
    }

    // 2. Submit to backend
    actions.setIsSaving(true);
    try {
      const payload = buildCreateCodingProblemRequest({
        values: {
          title: values.title,
          slug: values.slug,
          problemType: values.problemType,
          difficulty: values.difficulty,
          supportedLanguageIds: supportedLanguages,
          statement: values.statement,
          topics: values.topics,
          hints: values.hints,
        },
        starterCodes,
        referenceSolutionLanguage,
        referenceSolution,
        requireVerificationScript,
        verificationScriptLanguage,
        verificationScript,
        sampleTestcases,
        hiddenTestcases,
        executionConstraints,
        generatorLanguage,
        generatorScript,
        generatorStdin,
        generatorDelimiter,
        imageFileIds: uploadedImageIds,
      });

      if (mode === "edit" && editingProblemId) {
        const updated = await codingProblemService.updateCodingProblem(
          editingProblemId,
          payload
        );

        toast.add({
          title: "Problem Updated Successfully!",
          description: `"${updated.title}" has been updated and re-verified.`,
          type: "success",
        });
      } else {
        const created = await codingProblemService.createCodingProblem(payload);

        toast.add({
          title: "Problem Created Successfully!",
          description: `"${created.title}" has been saved as Unpublished.`,
          type: "success",
        });
      }

      // Clear all state from memory as requested
      actions.resetValues();

      if (onSuccess) {
        onSuccess();
      } else {
        // Navigate to admin coding problem management list
        navigate({ to: "/admin/coding-problem" });
      }
    } catch (err: unknown) {
      const error = err as {
        response?: { data?: { message?: string } };
        message?: string;
      };
      const isEdit = mode === "edit";
      const msg =
        error.response?.data?.message ||
        error.message ||
        `Failed to ${isEdit ? "update" : "create"} coding problem. Please verify code syntax and testcase constraints.`;
      toast.add({
        title: isEdit ? "Update Failed" : "Creation Failed",
        description: msg,
        type: "error",
      });
    } finally {
      actions.setIsSaving(false);
    }
  }, [
    values,
    referenceSolution,
    requireVerificationScript,
    verificationScript,
    sampleTestcases,
    hiddenTestcases,
    actions,
    supportedLanguages,
    starterCodes,
    referenceSolutionLanguage,
    verificationScriptLanguage,
    executionConstraints,
    generatorLanguage,
    generatorScript,
    generatorStdin,
    generatorDelimiter,
    uploadedImageIds,
    mode,
    editingProblemId,
    onSuccess,
    navigate,
  ]);

  return (
    <div
      className={cn(
        "flex h-full w-full flex-col overflow-hidden bg-background select-none",
        className
      )}
    >
      {/* 1. Header Overview Banner */}
      <div className="flex shrink-0 flex-col border-b border-border/80 bg-muted/20 px-6 py-3.5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <span className="flex size-9 items-center justify-center rounded-lg border border-primary/30 bg-primary/10 text-primary shadow-2xs">
              <Sparkles className="size-4" />
            </span>
            <div>
              <div className="flex items-center gap-2">
                <Badge
                  variant="outline"
                  className={cn(
                    "font-mono text-[11px] uppercase font-semibold",
                    values.difficulty === "EASY"
                      ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
                      : values.difficulty === "MEDIUM"
                        ? "border-amber-500/30 bg-amber-500/10 text-amber-600 dark:text-amber-400"
                        : "border-rose-500/30 bg-rose-500/10 text-rose-600 dark:text-rose-400"
                  )}
                >
                  {values.difficulty}
                </Badge>
                <Badge
                  variant="secondary"
                  className="font-mono text-[11px] text-muted-foreground"
                >
                  {values.problemType}
                </Badge>
              </div>
              <h2 className="mt-1 text-base font-bold tracking-tight text-foreground sm:text-lg">
                {values.title || "Untitled Problem"}
              </h2>
            </div>
          </div>

          {/* Quick Metrics */}
          <div className="flex items-center gap-2.5 text-xs text-muted-foreground">
            <div className="flex items-center gap-1.5 rounded-md border border-border/70 bg-background px-2.5 py-1">
              <Code2 className="size-3.5 text-primary" />
              <span>{supportedLanguages.length} Languages</span>
            </div>
            <div className="flex items-center gap-1.5 rounded-md border border-border/70 bg-background px-2.5 py-1">
              <FlaskConical className="size-3.5 text-primary" />
              <span>
                {sampleTestcases.length} Sample • {hiddenTestcases.length} Hidden
              </span>
            </div>
          </div>
        </div>

        {/* Topics & Hints Strip */}
        <div className="mt-2.5 flex flex-wrap items-center gap-1.5 text-xs">
          <span className="text-[11px] font-medium text-muted-foreground">
            Topics:
          </span>
          {values.topics.map((t) => (
            <Badge
              key={t}
              variant="outline"
              className="text-[10px] text-muted-foreground"
            >
              {t}
            </Badge>
          ))}
          {values.hints && values.hints.length > 0 && (
            <span className="ml-2 flex items-center gap-1 text-[11px] text-muted-foreground">
              <HelpCircle className="size-3" />
              <span>{values.hints.length} hint(s) provided</span>
            </span>
          )}
        </div>
      </div>

      {/* 2. Main Navigation Tabs */}
      <div className="flex h-10 shrink-0 items-center border-b border-border/80 bg-muted/10 px-6">
        <div className="flex items-center gap-1">
          <button
            type="button"
            onClick={() => setActiveTab("statement")}
            className={cn(
              "flex h-7 cursor-pointer items-center gap-1.5 rounded-md px-3 text-xs font-medium transition-colors",
              activeTab === "statement"
                ? "bg-background text-foreground shadow-2xs font-semibold"
                : "text-muted-foreground hover:bg-muted/60 hover:text-foreground"
            )}
          >
            <FileText className="size-3.5" />
            <span>Problem Statement</span>
          </button>

          <button
            type="button"
            onClick={() => setActiveTab("solutions")}
            className={cn(
              "flex h-7 cursor-pointer items-center gap-1.5 rounded-md px-3 text-xs font-medium transition-colors",
              activeTab === "solutions"
                ? "bg-background text-foreground shadow-2xs font-semibold"
                : "text-muted-foreground hover:bg-muted/60 hover:text-foreground"
            )}
          >
            <Code2 className="size-3.5" />
            <span>Solutions & Code</span>
          </button>

          <button
            type="button"
            onClick={() => setActiveTab("testcases")}
            className={cn(
              "flex h-7 cursor-pointer items-center gap-1.5 rounded-md px-3 text-xs font-medium transition-colors",
              activeTab === "testcases"
                ? "bg-background text-foreground shadow-2xs font-semibold"
                : "text-muted-foreground hover:bg-muted/60 hover:text-foreground"
            )}
          >
            <FlaskConical className="size-3.5" />
            <span>Testcases</span>
            <Badge
              variant="secondary"
              className="h-4 px-1 text-[9px] font-mono leading-none"
            >
              {sampleTestcases.length + hiddenTestcases.length}
            </Badge>
          </button>

          <button
            type="button"
            onClick={() => setActiveTab("constraints")}
            className={cn(
              "flex h-7 cursor-pointer items-center gap-1.5 rounded-md px-3 text-xs font-medium transition-colors",
              activeTab === "constraints"
                ? "bg-background text-foreground shadow-2xs font-semibold"
                : "text-muted-foreground hover:bg-muted/60 hover:text-foreground"
            )}
          >
            <Clock className="size-3.5" />
            <span>Execution Constraints</span>
          </button>
        </div>
      </div>

      {/* 3. Tab Body Workspace */}
      <div className="min-h-0 w-full flex-1 overflow-auto p-6 bg-background">
        {/* TAB 1: STATEMENT PREVIEW */}
        {activeTab === "statement" && (
          <div className="mx-auto max-w-4xl space-y-4">
            <div className="rounded-xl border border-border/80 bg-card p-6 shadow-xs">
              <h3 className="mb-4 text-xl font-bold tracking-tight text-foreground">
                {values.title}
              </h3>
              <MarkdownPreview
                content={values.statement}
                className="leading-relaxed"
              />
            </div>

            {values.hints && values.hints.length > 0 && (
              <div className="rounded-xl border border-border/80 bg-muted/20 p-4 space-y-2">
                <p className="text-xs font-semibold text-foreground flex items-center gap-1.5">
                  <HelpCircle className="size-3.5 text-primary" />
                  <span>Learner Hints ({values.hints.length})</span>
                </p>
                <div className="space-y-1.5">
                  {values.hints.map((hint, idx) => (
                    <div
                      key={idx}
                      className="rounded-md border border-border/60 bg-background p-2.5 text-xs text-muted-foreground"
                    >
                      <span className="font-semibold text-foreground mr-1.5">
                        Hint {idx + 1}:
                      </span>
                      {hint}
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {/* TAB 2: SOLUTIONS & CODE (Single Unified Monaco Inspector) */}
        {activeTab === "solutions" && (
          <div className="mx-auto max-w-5xl space-y-4">
            {/* Unified Code Inspector Card */}
            <div className="rounded-xl border border-border/80 bg-card overflow-hidden shadow-xs">
              {/* Header Selector Strip */}
              <div className="flex h-11 items-center justify-between border-b border-border/80 bg-muted/30 px-3">
                <div className="flex items-center gap-1">
                  {/* Option 1: Reference Solution */}
                  <button
                    type="button"
                    onClick={() => setCodeInspectorTab("reference")}
                    className={cn(
                      "flex h-7 cursor-pointer items-center gap-1.5 rounded-md px-2.5 text-xs font-medium transition-colors",
                      codeInspectorTab === "reference"
                        ? "bg-background text-foreground shadow-2xs font-semibold"
                        : "text-muted-foreground hover:bg-muted/60 hover:text-foreground"
                    )}
                  >
                    <Code2 className="size-3.5 text-primary" />
                    <span>Reference Solution</span>
                  </button>

                  {/* Option 2: Starter Code */}
                  <button
                    type="button"
                    onClick={() => setCodeInspectorTab("starter")}
                    className={cn(
                      "flex h-7 cursor-pointer items-center gap-1.5 rounded-md px-2.5 text-xs font-medium transition-colors",
                      codeInspectorTab === "starter"
                        ? "bg-background text-foreground shadow-2xs font-semibold"
                        : "text-muted-foreground hover:bg-muted/60 hover:text-foreground"
                    )}
                  >
                    <Terminal className="size-3.5 text-primary" />
                    <span>Starter Code</span>
                  </button>

                  {/* Option 3: Verification Script */}
                  {requireVerificationScript && (
                    <button
                      type="button"
                      onClick={() => setCodeInspectorTab("verification")}
                      className={cn(
                        "flex h-7 cursor-pointer items-center gap-1.5 rounded-md px-2.5 text-xs font-medium transition-colors",
                        codeInspectorTab === "verification"
                          ? "bg-background text-foreground shadow-2xs font-semibold"
                          : "text-muted-foreground hover:bg-muted/60 hover:text-foreground"
                      )}
                    >
                      <ShieldCheck className="size-3.5 text-emerald-500" />
                      <span>Verification Script</span>
                    </button>
                  )}

                  {/* Option 4: Generator Script */}
                  {generatorScript.trim().length > 0 && (
                    <button
                      type="button"
                      onClick={() => setCodeInspectorTab("generator")}
                      className={cn(
                        "flex h-7 cursor-pointer items-center gap-1.5 rounded-md px-2.5 text-xs font-medium transition-colors",
                        codeInspectorTab === "generator"
                          ? "bg-background text-foreground shadow-2xs font-semibold"
                          : "text-muted-foreground hover:bg-muted/60 hover:text-foreground"
                      )}
                    >
                      <Terminal className="size-3.5 text-amber-500" />
                      <span>Generator Script</span>
                    </button>
                  )}
                </div>

                {/* Right: Language Badge / Selector & Filename */}
                <div className="flex items-center gap-2">
                  {codeInspectorTab === "starter" && !isDatabase ? (
                    <Select
                      value={selectedStarterLang}
                      onValueChange={(val) => {
                        if (val) setSelectedStarterLang(val);
                      }}
                    >
                      <SelectTrigger className="h-7 cursor-pointer gap-1.5 border-border/80 bg-background px-2.5 font-mono text-xs">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent align="end">
                        {supportedLanguages.map((langId) => (
                          <SelectItem
                            key={langId}
                            value={langId}
                            className="font-mono text-xs cursor-pointer"
                          >
                            {getLanguageById(langId)?.name || langId}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  ) : (
                    <Badge variant="outline" className="font-mono text-[10px]">
                      {activeBadgeName}
                    </Badge>
                  )}

                  <span className="font-mono text-[11px] text-muted-foreground hidden sm:inline">
                    {activeFileName}
                  </span>
                </div>
              </div>

              {/* Single High-Performance Monaco Editor Instance */}
              <div className="h-[460px] w-full">
                <MonacoEditor
                  language={activeCodeLang}
                  value={activeCodeValue}
                  readOnly
                  height="100%"
                  width="100%"
                  bordered={false}
                  options={{
                    minimap: { enabled: false },
                    lineNumbers: "on",
                    scrollBeyondLastLine: false,
                    fontSize: 12,
                    wordWrap: "on",
                    tabSize: 4,
                  }}
                />
              </div>
            </div>
          </div>
        )}

        {/* TAB 3: TESTCASES (Sample & Hidden with LineNumberedViewer) */}
        {activeTab === "testcases" && (
          <div className="mx-auto max-w-5xl space-y-4">
            <div className="flex items-center gap-1 rounded-md border border-border/80 bg-muted/30 p-1 text-xs">
              <button
                type="button"
                onClick={() => setTestcaseReviewMode("sample")}
                className={cn(
                  "flex h-7 cursor-pointer items-center gap-1.5 rounded-sm px-3 text-xs font-medium transition-colors",
                  testcaseReviewMode === "sample"
                    ? "bg-background text-foreground shadow-2xs font-semibold"
                    : "text-muted-foreground hover:text-foreground"
                )}
              >
                <FlaskConical className="size-3.5 text-primary" />
                <span>Sample Testcases</span>
                <Badge
                  variant="secondary"
                  className="h-4 px-1 text-[9px] font-mono leading-none"
                >
                  {sampleTestcases.length}
                </Badge>
              </button>

              <button
                type="button"
                onClick={() => setTestcaseReviewMode("hidden")}
                className={cn(
                  "flex h-7 cursor-pointer items-center gap-1.5 rounded-sm px-3 text-xs font-medium transition-colors",
                  testcaseReviewMode === "hidden"
                    ? "bg-background text-foreground shadow-2xs font-semibold"
                    : "text-muted-foreground hover:text-foreground"
                )}
              >
                <Lock className="size-3.5 text-amber-500" />
                <span>Hidden Testcases</span>
                <Badge
                  variant="secondary"
                  className="h-4 px-1 text-[9px] font-mono leading-none"
                >
                  {hiddenTestcases.length}
                </Badge>
              </button>
            </div>

            {/* Sample Testcases Review */}
            {testcaseReviewMode === "sample" && (
              <div className="rounded-xl border border-border/80 bg-card p-4 space-y-3">
                <div className="flex items-center justify-between border-b border-border/60 pb-2">
                  <div className="no-scrollbar flex items-center gap-1.5 overflow-x-auto">
                    {sampleTestcases.map((tc, idx) => (
                      <button
                        key={tc.id || idx}
                        type="button"
                        onClick={() => setSelectedSampleIndex(idx)}
                        className={cn(
                          "flex h-6 cursor-pointer items-center rounded-md px-2.5 font-mono text-[11px] transition-colors",
                          idx === selectedSampleIndex
                            ? "bg-foreground text-background font-semibold"
                            : "bg-muted text-muted-foreground hover:text-foreground"
                        )}
                      >
                        Case {idx + 1}
                      </button>
                    ))}
                  </div>

                  <span className="font-mono text-[10px] text-muted-foreground">
                    {isDatabase ? "dbInit (SQL)" : "stdin"}
                  </span>
                </div>

                <div className="overflow-hidden rounded-lg border border-border/70 bg-background">
                  <LineNumberedViewer
                    content={
                      (isDatabase
                        ? sampleTestcases[selectedSampleIndex]?.dbInit
                        : sampleTestcases[selectedSampleIndex]?.stdin) || ""
                    }
                    language={isDatabase ? "sql" : "plaintext"}
                    maxHeight="360px"
                    showLineNumbers={true}
                    emptyText={
                      isDatabase
                        ? "(Empty database initialization)"
                        : "(Empty standard input)"
                    }
                  />
                </div>
              </div>
            )}

            {/* Hidden Testcases Review */}
            {testcaseReviewMode === "hidden" && (
              <div className="rounded-xl border border-border/80 bg-card p-4 space-y-3">
                <div className="flex items-center justify-between border-b border-border/60 pb-2">
                  <div className="no-scrollbar flex items-center gap-1.5 overflow-x-auto">
                    {hiddenTestcases.map((tc, idx) => (
                      <button
                        key={tc.id || idx}
                        type="button"
                        onClick={() => setSelectedHiddenIndex(idx)}
                        className={cn(
                          "flex h-6 cursor-pointer items-center rounded-md px-2.5 font-mono text-[11px] transition-colors",
                          idx === selectedHiddenIndex
                            ? "bg-foreground text-background font-semibold"
                            : "bg-muted text-muted-foreground hover:text-foreground"
                        )}
                      >
                        Case {idx + 1}
                      </button>
                    ))}
                  </div>

                  <span className="font-mono text-[10px] text-muted-foreground">
                    {isDatabase ? "dbInit (SQL)" : "stdin"}
                  </span>
                </div>

                <div className="overflow-hidden rounded-lg border border-border/70 bg-background">
                  <LineNumberedViewer
                    content={
                      (isDatabase
                        ? hiddenTestcases[selectedHiddenIndex]?.dbInit
                        : hiddenTestcases[selectedHiddenIndex]?.stdin) || ""
                    }
                    language={isDatabase ? "sql" : "plaintext"}
                    maxHeight="360px"
                    showLineNumbers={true}
                    emptyText={
                      isDatabase
                        ? "(Empty database initialization)"
                        : "(Empty standard input)"
                    }
                  />
                </div>
              </div>
            )}
          </div>
        )}

        {/* TAB 4: EXECUTION CONSTRAINTS */}
        {activeTab === "constraints" && (
          <div className="mx-auto max-w-4xl space-y-4">
            <div className="rounded-xl border border-border/80 bg-card overflow-hidden shadow-xs">
              <table className="w-full text-left text-xs">
                <thead className="border-b border-border/80 bg-muted/30 font-semibold text-muted-foreground">
                  <tr>
                    <th className="p-3 pl-4">Language</th>
                    <th className="p-3">Time Limit</th>
                    <th className="p-3">Memory Limit</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border/60">
                  {supportedLanguages.map((langId) => {
                    const lang = getLanguageById(langId);
                    const limits = executionConstraints[langId] || {};
                    return (
                      <tr key={langId} className="hover:bg-muted/10">
                        <td className="p-3 pl-4 font-mono font-medium text-foreground">
                          {lang?.name || langId}
                        </td>
                        <td className="p-3 font-mono text-muted-foreground">
                          <span className="flex items-center gap-1">
                            <Clock className="size-3 text-primary" />
                            <span>{limits.timeLimitMs || 2000} ms</span>
                          </span>
                        </td>
                        <td className="p-3 font-mono text-muted-foreground">
                          <span className="flex items-center gap-1">
                            <Cpu className="size-3 text-primary" />
                            <span>
                              {Math.round(
                                (limits.memoryLimitKb || 262144) / 1024
                              )}{" "}
                              MB
                            </span>
                          </span>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>

      {/* 4. Bottom Final Submit Bar */}
      <div className="flex h-11 shrink-0 items-center justify-between border-t border-border/80 bg-background/95 px-4 backdrop-blur-xs">
        {/* Left: Summary Info */}
        <div className="flex items-center gap-2 text-xs text-muted-foreground">
          <ShieldCheck className="size-3.5 text-emerald-500" />
          <span>All 4 Steps Verified • Ready to Create Problem</span>
        </div>

        {/* Right: Back to Step 3 & Final Submit */}
        <div className="flex items-center gap-2">
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => {
              if (onBack) onBack();
              else actions.setCurrentStep(3);
            }}
            className="cursor-pointer gap-1.5 text-xs text-muted-foreground hover:text-foreground"
          >
            <ArrowLeft className="size-3.5" />
            <span>Back to Hidden Testcases</span>
          </Button>

          <Button
            type="button"
            size="sm"
            disabled={isSaving}
            onClick={handleSubmitProblem}
            className="cursor-pointer gap-1.5 bg-primary px-6 text-xs font-semibold text-primary-foreground shadow-xs hover:bg-primary/90"
          >
            {isSaving ? (
              <Loader2 className="size-3.5 animate-spin" />
            ) : (
              <Check className="size-3.5" />
            )}
            <span>
              {isSaving
                ? mode === "edit"
                  ? "Updating Problem..."
                  : "Creating Problem..."
                : mode === "edit"
                  ? "Submit & Update Problem"
                  : "Submit & Create Problem"}
            </span>
          </Button>
        </div>
      </div>
    </div>
  );
}

export default StepReview;
