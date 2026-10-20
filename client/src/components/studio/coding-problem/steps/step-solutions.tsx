import {
  useState,
  useImperativeHandle,
  forwardRef,
  useCallback,
  useMemo,
} from "react";
import { AlertCircle, ArrowLeft, ArrowRight } from "lucide-react";
import { toast } from "@/components/ui/toast";
import { Button } from "@/components/ui/button";
import {
  ResizablePanelGroup,
  ResizablePanel,
  ResizableHandle,
} from "@/components/ui/resizable";
import { cn } from "@/lib/utils";
import { useCodingProblemStore } from "../store";
import { IdeToolbar } from "./solutions/ide-toolbar";
import { IdeEditorsPanel } from "./solutions/ide-editors-panel";
import { IdeTestcasesPanel } from "./solutions/ide-testcases-panel";

export interface StepSolutionsHandle {
  submit: () => Promise<boolean>;
}

export interface StepSolutionsProps {
  onBack?: () => void;
  onNext?: () => void;
  onSaved?: () => void;
  className?: string;
}

export const StepSolutions = forwardRef<
  StepSolutionsHandle,
  StepSolutionsProps
>(function StepSolutions({ onBack, onSaved, className }, ref) {
  const values = useCodingProblemStore((state) => state.values);
  const referenceSolution = useCodingProblemStore(
    (state) => state.referenceSolution
  );
  const requireVerificationScript = useCodingProblemStore(
    (state) => state.requireVerificationScript
  );
  const verificationScript = useCodingProblemStore(
    (state) => state.verificationScript
  );
  const sampleTestcases = useCodingProblemStore(
    (state) => state.sampleTestcases
  );
  const actions = useCodingProblemStore((state) => state.actions);

  const [validationError, setValidationError] = useState<string | null>(null);

  const isDatabase = values.problemType === "DATABASE";
  const supportedLanguages = useMemo(
    () =>
      values.supportedLanguageIds.length > 0
        ? values.supportedLanguageIds
        : values.languages.length > 0
          ? values.languages
          : ["javascript-node-20"],
    [values.supportedLanguageIds, values.languages]
  );

  // =========================================================================
  // Validate and Submit to Backend API
  // =========================================================================
  const validateAndSave = useCallback(async (): Promise<boolean> => {
    setValidationError(null);

    // 1. Validate Reference Solution
    if (!referenceSolution || !referenceSolution.trim()) {
      actions.setActiveEditorMode("reference");
      const msg = "Reference solution is required before saving.";
      setValidationError(msg);
      toast.add({
        title: "Missing Reference Solution",
        description: msg,
        type: "error",
      });
      return false;
    }

    // 2. Validate Verification Script if enabled
    if (requireVerificationScript) {
      if (!verificationScript || !verificationScript.trim()) {
        actions.setActiveEditorMode("verification");
        const msg =
          "Verification script source code cannot be empty when verification script is enabled.";
        setValidationError(msg);
        toast.add({
          title: "Missing Verification Script",
          description: msg,
          type: "error",
        });
        return false;
      }
    }

    // 3. Validate Sample Testcases (Starter code is optional)
    if (!sampleTestcases || sampleTestcases.length === 0) {
      actions.setActiveTestcaseTab("testcases");
      const msg = "At least one sample testcase is required.";
      setValidationError(msg);
      toast.add({
        title: "Missing Sample Testcase",
        description: msg,
        type: "error",
      });
      return false;
    }

    for (let i = 0; i < sampleTestcases.length; i++) {
      const tc = sampleTestcases[i];
      const hasContent = isDatabase
        ? Boolean(tc.dbInit?.trim())
        : Boolean(tc.stdin?.trim());

      if (!hasContent) {
        actions.setActiveTestcaseTab("testcases");
        actions.setActiveTestcaseIndex(i);
        const msg = isDatabase
          ? `Sample testcase ${i + 1} is missing database initialization SQL (dbInit).`
          : `Sample testcase ${i + 1} is missing standard input (stdin).`;
        setValidationError(msg);
        toast.add({
          title: "Incomplete Testcase",
          description: msg,
          type: "error",
        });
        return false;
      }
    }

    // 4. Step 2 Validated: Advance to Step 3 (Hidden Testcases)
    toast.add({
      title: "Solutions & Validation Verified",
      description: "Proceeding to Step 3: Hidden Testcases.",
      type: "success",
    });

    if (onSaved) onSaved();
    actions.setCurrentStep(3);
    return true;
  }, [
    isDatabase,
    referenceSolution,
    requireVerificationScript,
    verificationScript,
    sampleTestcases,
    actions,
    onSaved,
  ]);

  useImperativeHandle(
    ref,
    () => ({
      submit: () => validateAndSave(),
    }),
    [validateAndSave]
  );

  // =========================================================================
  // Run Button Handler
  // =========================================================================
  const handleRunSampleTests = async () => {
    if (sampleTestcases.length === 0) {
      toast.add({
        title: "Testcases Required",
        description: "Please add at least one sample testcase.",
        type: "error",
      });
      return;
    }

    await actions.runSampleTestcases();
    toast.add({
      title: "Test Run Completed",
      description: `Executed against ${sampleTestcases.length} sample testcase(s).`,
      type: "success",
    });
  };

  return (
    <div
      className={cn(
        "flex h-full w-full flex-col overflow-hidden bg-background select-none",
        className
      )}
    >
      {/* Optional Validation Error Banner */}
      {validationError && (
        <div className="flex shrink-0 items-center justify-between border-b border-destructive/30 bg-destructive/10 px-4 py-2 text-xs font-medium text-destructive">
          <div className="flex items-center gap-2">
            <AlertCircle className="size-4 shrink-0" />
            <span>{validationError}</span>
          </div>
          <button
            type="button"
            onClick={() => setValidationError(null)}
            className="cursor-pointer text-[11px] underline opacity-80 hover:opacity-100"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Workbench Header Toolbar */}
      <IdeToolbar onRunTestcases={handleRunSampleTests} />

      {/* Main IDE Workspace: Split Panes (Left: Code Editors, Right: Testcases & Results) */}
      <div className="min-h-0 w-full flex-1 overflow-hidden">
        <ResizablePanelGroup orientation="horizontal" className="h-full w-full">
          {/* Left Panel: Code Editors */}
          <ResizablePanel
            defaultSize={58}
            minSize={30}
            className="flex flex-col overflow-hidden"
          >
            <IdeEditorsPanel />
          </ResizablePanel>

          <ResizableHandle withHandle />

          {/* Right Panel: Testcases and Results */}
          <ResizablePanel
            defaultSize={42}
            minSize={25}
            className="flex flex-col overflow-hidden border-l border-border/80"
          >
            <IdeTestcasesPanel onRunTestcases={handleRunSampleTests} />
          </ResizablePanel>
        </ResizablePanelGroup>
      </div>

      {/* Bottom Navigation & Next Bar */}
      <div className="flex h-11 shrink-0 items-center justify-between border-t border-border/80 bg-background/95 px-4 backdrop-blur-xs">
        <div className="flex items-center gap-2 text-xs text-muted-foreground">
          <span className="flex size-2 rounded-full bg-emerald-500" />
          <span>
            {values.problemType} Problem • {supportedLanguages.length} Language(s)
          </span>
        </div>

        <div className="flex items-center gap-2">
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => {
              if (onBack) onBack();
              else actions.setCurrentStep(1);
            }}
            className="cursor-pointer gap-1.5 text-xs text-muted-foreground hover:text-foreground"
          >
            <ArrowLeft className="size-3.5" />
            <span>Back to Statement</span>
          </Button>

          <Button
            type="button"
            size="sm"
            onClick={() => validateAndSave()}
            className="cursor-pointer gap-1.5 bg-primary px-5 text-xs font-semibold text-primary-foreground shadow-xs hover:bg-primary/90"
          >
            <span>Next: Hidden Testcases</span>
            <ArrowRight className="size-3.5" />
          </Button>
        </div>
      </div>
    </div>
  );
});

export default StepSolutions;
