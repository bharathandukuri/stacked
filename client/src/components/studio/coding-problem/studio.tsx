import { useEffect } from "react";
import { StudioNavbar } from "./components/studio-navbar";
import StatementStep from "./steps/statement-step";
import StepSolutions from "./steps/step-solutions";
import StepHiddenTestcases from "./steps/step-hidden-testcases";
import StepReview from "./steps/step-review";
import { useCodingProblemStore } from "./store";
import { useCodingProblem } from "@/hooks/use-coding-problems";
import { Loader2, AlertCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useNavigate } from "@tanstack/react-router";

export interface CodingProblemStudioProps {
  mode?: "create" | "edit";
  problemId?: string;
}

export function CodingProblemStudio({
  mode = "create",
  problemId,
}: CodingProblemStudioProps) {
  const navigate = useNavigate();
  const currentStep = useCodingProblemStore((state) => state.currentStep);
  const title = useCodingProblemStore((state) => state.values.title);
  const currentMode = useCodingProblemStore((state) => state.mode);
  const editingProblemId = useCodingProblemStore(
    (state) => state.editingProblemId
  );
  const actions = useCodingProblemStore((state) => state.actions);

  // If edit mode, fetch existing problem details
  const isEditMode = mode === "edit" && Boolean(problemId);
  const {
    data: problem,
    isLoading,
    isError,
  } = useCodingProblem(isEditMode ? problemId : undefined);

  // Hydrate problem when query succeeds in edit mode
  useEffect(() => {
    if (isEditMode && problem) {
      actions.hydrateProblem(problem);
    }
  }, [isEditMode, problem, actions]);

  // Reset store if switching to create mode while store was in edit mode
  useEffect(() => {
    if (mode === "create" && (currentMode === "edit" || editingProblemId)) {
      actions.resetStore();
    }
  }, [mode, currentMode, editingProblemId, actions]);

  // Loading state for edit mode
  if (isEditMode && isLoading) {
    return (
      <div className="flex h-screen flex-col items-center justify-center gap-3 bg-background text-foreground">
        <Loader2 className="size-8 animate-spin text-primary" />
        <span className="text-sm font-medium text-muted-foreground">
          Loading coding problem details...
        </span>
      </div>
    );
  }

  // Error state for edit mode
  if (isEditMode && isError) {
    return (
      <div className="flex h-screen flex-col items-center justify-center gap-4 bg-background p-6 text-foreground">
        <AlertCircle className="size-10 text-destructive" />
        <div className="text-center">
          <h2 className="text-lg font-bold text-foreground">
            Failed to Load Problem
          </h2>
          <p className="mt-1 text-sm text-muted-foreground">
            The requested coding problem could not be found or loaded.
          </p>
        </div>
        <Button
          variant="outline"
          size="sm"
          onClick={() => navigate({ to: "/admin/coding-problem" })}
          className="cursor-pointer"
        >
          Return to Problem List
        </Button>
      </div>
    );
  }

  return (
    <div className="flex h-screen flex-col overflow-hidden bg-background text-foreground">
      {/* Top Navbar: Display-Only Status & Progress Stepper */}
      <StudioNavbar
        mode={mode}
        problemId={problemId}
        problemTitle={title}
        currentStep={currentStep}
      />

      {/* Main Multi-Step Workspace */}
      <main className="min-h-0 w-full flex-1 overflow-hidden">
        {currentStep === 1 && (
          <div className="h-full w-full overflow-y-auto">
            <StatementStep />
          </div>
        )}
        {currentStep === 2 && (
          <div className="h-full w-full overflow-hidden">
            <StepSolutions />
          </div>
        )}
        {currentStep === 3 && (
          <div className="h-full w-full overflow-hidden">
            <StepHiddenTestcases />
          </div>
        )}
        {currentStep === 4 && (
          <div className="h-full w-full overflow-hidden">
            <StepReview />
          </div>
        )}
      </main>
    </div>
  );
}

export default CodingProblemStudio;
