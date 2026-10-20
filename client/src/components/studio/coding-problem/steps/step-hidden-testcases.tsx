import {
  useState,
  useImperativeHandle,
  forwardRef,
  useCallback,
} from "react";
import {
  AlertCircle,
  ArrowLeft,
  ArrowRight,
  ShieldCheck,
} from "lucide-react";
import { toast } from "@/components/ui/toast";
import { Button } from "@/components/ui/button";
import {
  ResizablePanelGroup,
  ResizablePanel,
  ResizableHandle,
} from "@/components/ui/resizable";
import { cn } from "@/lib/utils";
import { useCodingProblemStore } from "../store";
import { GeneratorPanel } from "./hidden-testcases/generator-panel";
import { HiddenTestcasesPanel } from "./hidden-testcases/hidden-testcases-panel";

export interface StepHiddenTestcasesHandle {
  submit: () => Promise<boolean>;
}

export interface StepHiddenTestcasesProps {
  onBack?: () => void;
  onSaved?: () => void;
  className?: string;
}

export const StepHiddenTestcases = forwardRef<
  StepHiddenTestcasesHandle,
  StepHiddenTestcasesProps
>(function StepHiddenTestcases({ onBack, onSaved, className }, ref) {
  const values = useCodingProblemStore((state) => state.values);
  const sampleTestcases = useCodingProblemStore(
    (state) => state.sampleTestcases
  );
  const hiddenTestcases = useCodingProblemStore(
    (state) => state.hiddenTestcases
  );
  const actions = useCodingProblemStore((state) => state.actions);

  const [validationError, setValidationError] = useState<string | null>(null);

  const isDatabase = values.problemType === "DATABASE";

  // =========================================================================
  // Validate and Submit to Backend API (POST /api/coding-problem)
  // =========================================================================
  const validateAndSave = useCallback(async (): Promise<boolean> => {
    setValidationError(null);

    // 1. Validate Hidden Testcases Count (At least 1 required, max 20)
    if (!hiddenTestcases || hiddenTestcases.length === 0) {
      const msg =
        "At least one hidden testcase is required to evaluate learner submissions.";
      setValidationError(msg);
      toast.add({
        title: "Missing Hidden Testcase",
        description: msg,
        type: "error",
      });
      return false;
    }

    if (hiddenTestcases.length > 20) {
      const msg = "A maximum of 20 hidden testcases is allowed.";
      setValidationError(msg);
      toast.add({
        title: "Too Many Hidden Testcases",
        description: msg,
        type: "error",
      });
      return false;
    }

    // 2. Validate Every Hidden Testcase Content
    for (let i = 0; i < hiddenTestcases.length; i++) {
      const tc = hiddenTestcases[i];
      const hasContent = isDatabase
        ? Boolean(tc.dbInit && tc.dbInit.trim().length > 0)
        : Boolean(tc.stdin && tc.stdin.trim().length > 0);

      if (!hasContent) {
        actions.setActiveHiddenTestcaseIndex(i);
        const msg = isDatabase
          ? `Hidden testcase ${i + 1} is missing database initialization SQL (dbInit).`
          : `Hidden testcase ${i + 1} is missing standard input (stdin).`;
        setValidationError(msg);
        toast.add({
          title: "Incomplete Hidden Testcase",
          description: msg,
          type: "error",
        });
        return false;
      }
    }

    // 3. Step 3 Validated: Advance to Step 4 (Review & Submit)
    toast.add({
      title: "Hidden Testcases Verified",
      description: "Proceeding to Step 4: Review & Submit.",
      type: "success",
    });

    if (onSaved) onSaved();
    actions.setCurrentStep(4);
    return true;
  }, [
    hiddenTestcases,
    isDatabase,
    actions,
    onSaved,
  ]);

  useImperativeHandle(
    ref,
    () => ({
      submit: validateAndSave,
    }),
    [validateAndSave]
  );

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

      {/* Main Workspace: Split Panes (Left: Testcase Generator, Right: Hidden Testcases Manager) */}
      <div className="min-h-0 w-full flex-1 overflow-hidden">
        <ResizablePanelGroup orientation="horizontal" className="h-full w-full">
          {/* Left Panel: Generator Workspace */}
          <ResizablePanel
            defaultSize={58}
            minSize={35}
            className="flex flex-col overflow-hidden"
          >
            <GeneratorPanel />
          </ResizablePanel>

          <ResizableHandle withHandle />

          {/* Right Panel: Hidden Testcases Manager */}
          <ResizablePanel
            defaultSize={42}
            minSize={25}
            className="flex flex-col overflow-hidden border-l border-border/80"
          >
            <HiddenTestcasesPanel />
          </ResizablePanel>
        </ResizablePanelGroup>
      </div>

      {/* Bottom Save & Submit Bar */}
      <div className="flex h-11 shrink-0 items-center justify-between border-t border-border/80 bg-background/95 px-4 backdrop-blur-xs">
        {/* Left: Summary Info */}
        <div className="flex items-center gap-2 text-xs text-muted-foreground">
          <ShieldCheck className="size-3.5 text-emerald-500" />
          <span>
            {values.problemType} • {sampleTestcases.length} Sample Case(s) •{" "}
            {hiddenTestcases.length} Hidden Case(s)
          </span>
        </div>

        {/* Right: Back to Step 2 & Save Button */}
        <div className="flex items-center gap-2">
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => {
              if (onBack) onBack();
              else actions.setCurrentStep(2);
            }}
            className="cursor-pointer gap-1.5 text-xs text-muted-foreground hover:text-foreground"
          >
            <ArrowLeft className="size-3.5" />
            <span>Back to Solutions</span>
          </Button>

          <Button
            type="button"
            size="sm"
            onClick={() => validateAndSave()}
            className="cursor-pointer gap-1.5 bg-primary px-5 text-xs font-semibold text-primary-foreground shadow-xs hover:bg-primary/90"
          >
            <span>Next: Review & Submit</span>
            <ArrowRight className="size-3.5" />
          </Button>
        </div>
      </div>
    </div>
  );
});

export default StepHiddenTestcases;
