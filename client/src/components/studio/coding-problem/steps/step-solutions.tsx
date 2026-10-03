import { useState, useImperativeHandle, forwardRef } from "react"
import { AlertCircle } from "lucide-react"
import { toast } from "@/components/ui/toast"
import {
  ResizablePanelGroup,
  ResizablePanel,
  ResizableHandle,
} from "@/components/ui/resizable"
import { cn } from "@/lib/utils"
import { useCodingProblemStore } from "../store"
import { getLanguageById } from "@/config/languages"
import { IdeToolbar } from "./solutions/ide-toolbar"
import { IdeEditorsPanel } from "./solutions/ide-editors-panel"
import { IdeTestcasesPanel } from "./solutions/ide-testcases-panel"

export interface StepSolutionsHandle {
  submit: () => boolean
}

export interface StepSolutionsProps {
  onBack?: () => void
  onSaved?: () => void
  className?: string
}

export const StepSolutions = forwardRef<
  StepSolutionsHandle,
  StepSolutionsProps
>(function StepSolutions({ onSaved, className }, ref) {
  const values = useCodingProblemStore((state) => state.values)
  const actions = useCodingProblemStore((state) => state.actions)

  const activeCodeLanguage = useCodingProblemStore(
    (state) =>
      state.activeCodeLanguage || values.languages[0] || "javascript-node-20"
  )
  const referenceSolutions = useCodingProblemStore(
    (state) => state.referenceSolutions
  )
  const solutionValidators = useCodingProblemStore(
    (state) => state.solutionValidators
  )
  const databaseSolution = useCodingProblemStore(
    (state) => state.databaseSolution
  )

  const [validationError, setValidationError] = useState<string | null>(null)

  const isDatabaseProblem = values.problemType === "DATABASE"
  const selectedLanguages =
    values.languages.length > 0 ? values.languages : ["javascript-node-20"]

  // Validation on publish/save
  const validateAndSave = (): boolean => {
    setValidationError(null)

    if (isDatabaseProblem) {
      if (!databaseSolution.trim()) {
        const msg = "Reference SQL query is required for database problems."
        setValidationError(msg)
        toast.add({
          title: "Missing Reference Solution",
          description: msg,
          type: "error",
        })
        return false
      }
    } else {
      // Generic: ensure every selected language has both a solution and validator
      for (const langId of selectedLanguages) {
        const langName = getLanguageById(langId)?.name || langId
        const sol = referenceSolutions[langId]?.trim()
        const val = solutionValidators[langId]?.trim()

        if (!sol) {
          actions.setActiveCodeLanguage(langId)
          const msg = `Reference solution is missing for ${langName}. Please provide a solution.`
          setValidationError(msg)
          toast.add({
            title: "Missing Solution",
            description: msg,
            type: "error",
          })
          return false
        }

        if (!val) {
          actions.setActiveCodeLanguage(langId)
          const msg = `Solution validator is missing for ${langName}. Please provide a validator script.`
          setValidationError(msg)
          toast.add({
            title: "Missing Validator",
            description: msg,
            type: "error",
          })
          return false
        }
      }
    }

    toast.add({
      title: "Problem Published Successfully",
      description:
        "All problem statement details, code templates, reference solutions, and validators have been validated.",
      type: "success",
    })

    onSaved?.()
    return true
  }

  useImperativeHandle(
    ref,
    () => ({
      submit: () => validateAndSave(),
    }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [
      selectedLanguages,
      referenceSolutions,
      solutionValidators,
      databaseSolution,
      isDatabaseProblem,
    ]
  )

  const handleRunSampleTests = async () => {
    await actions.runSampleTestcases(activeCodeLanguage)
    toast.add({
      title: "Test Run Completed",
      description: `Executed against ${values.testCases.length} sample test case(s).`,
      type: "success",
    })
  }

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
      <IdeToolbar
        onRunTestcases={handleRunSampleTests}
        onResetReferenceSolution={() =>
          actions.resetReferenceSolutionToStarter(activeCodeLanguage)
        }
      />

      {/* Main IDE Workspace: Split Panes (Left: Editors, Right: Testcases & Console) */}
      <div className="min-h-0 w-full flex-1 overflow-hidden">
        <ResizablePanelGroup orientation="horizontal" className="h-full w-full">
          {/* Left Panel: Code Editors */}
          <ResizablePanel
            defaultSize={60}
            minSize={35}
            className="flex flex-col overflow-hidden"
          >
            <IdeEditorsPanel />
          </ResizablePanel>

          <ResizableHandle withHandle />

          {/* Right Panel: Testcases & Verification Console */}
          <ResizablePanel
            defaultSize={40}
            minSize={25}
            className="flex flex-col overflow-hidden border-l border-border/80"
          >
            <IdeTestcasesPanel onRunTestcases={handleRunSampleTests} />
          </ResizablePanel>
        </ResizablePanelGroup>
      </div>
    </div>
  )
})

export default StepSolutions
