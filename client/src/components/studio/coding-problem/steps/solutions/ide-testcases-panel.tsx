import { useState } from "react"
import {
  Plus,
  Trash2,
  Copy,
  ChevronUp,
  ChevronDown,
  CheckCircle2,
  XCircle,
  Clock,
  Cpu,
  Play,
  Loader2,
  Terminal,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Textarea } from "@/components/ui/textarea"
import { MonacoEditor } from "@/components/monaco-editor"
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip"
import {
  ResizablePanelGroup,
  ResizablePanel,
  ResizableHandle,
} from "@/components/ui/resizable"
import { cn } from "@/lib/utils"
import { useCodingProblemStore, type TestResultItem } from "../../store"
import type { TestCase } from "../../types"

export interface IdeTestcasesPanelProps {
  onRunTestcases: () => void
  className?: string
}

export function IdeTestcasesPanel({
  onRunTestcases,
  className,
}: IdeTestcasesPanelProps) {
  const values = useCodingProblemStore((state) => state.values)
  const isTesting = useCodingProblemStore((state) => state.isTesting)
  const testResults = useCodingProblemStore((state) => state.testResults)
  const actions = useCodingProblemStore((state) => state.actions)

  const testCases = values.testCases || []
  const problemType = values.problemType

  const [activeCaseTab, setActiveCaseTab] = useState(0)
  const [activeResultIndex, setActiveResultIndex] = useState(0)

  const activeIndex = Math.min(activeCaseTab, Math.max(0, testCases.length - 1))
  const activeCase = testCases[activeIndex]

  // Testcase CRUD operations (synced directly with Zustand store)
  const handleAddTestCase = () => {
    const nextNumber = testCases.length + 1
    const newCase: TestCase = {
      id: `case-${Date.now()}`,
      title: `Case ${nextNumber}`,
      input: "",
    }
    const updated = [...testCases, newCase]
    actions.setTestCases(updated)
    setActiveCaseTab(updated.length - 1)
  }

  const handleDuplicateTestCase = (index: number) => {
    const target = testCases[index]
    if (!target) return

    const duplicated: TestCase = {
      ...target,
      id: `case-${Date.now()}`,
    }
    const updated = [...testCases]
    updated.splice(index + 1, 0, duplicated)
    const reindexed = updated.map((tc, idx) => ({
      ...tc,
      title: `Case ${idx + 1}`,
    }))
    actions.setTestCases(reindexed)
    setActiveCaseTab(index + 1)
  }

  const handleRemoveTestCase = (index: number) => {
    if (testCases.length <= 1) return
    const filtered = testCases.filter((_, i) => i !== index)
    const reindexed = filtered.map((tc, idx) => ({
      ...tc,
      title: `Case ${idx + 1}`,
    }))
    actions.setTestCases(reindexed)
    setActiveCaseTab(Math.max(0, Math.min(activeCaseTab, reindexed.length - 1)))
  }

  const handleMoveTestCase = (fromIndex: number, direction: -1 | 1) => {
    const toIndex = fromIndex + direction
    if (toIndex < 0 || toIndex >= testCases.length) return

    const updated = [...testCases]
    const item = updated[fromIndex]
    updated[fromIndex] = updated[toIndex]
    updated[toIndex] = item

    const reindexed = updated.map((tc, idx) => ({
      ...tc,
      title: `Case ${idx + 1}`,
    }))
    actions.setTestCases(reindexed)
    setActiveCaseTab(toIndex)
  }

  const handleUpdateInput = (val: string) => {
    if (!activeCase) return
    const updated = [...testCases]
    updated[activeIndex] = {
      ...activeCase,
      input: val,
    }
    actions.setTestCases(updated)
  }

  const currentResult: TestResultItem | undefined =
    testResults && testResults[activeResultIndex]
      ? testResults[activeResultIndex]
      : testResults?.[0]

  const allPassed =
    testResults && testResults.length > 0
      ? testResults.every((r) => r.status === "PASSED")
      : false

  return (
    <div
      className={cn("h-full w-full overflow-hidden bg-background", className)}
    >
      <ResizablePanelGroup orientation="vertical" className="h-full w-full">
        {/* Top Pane: Test Cases (Matching Statement Specification) */}
        <ResizablePanel
          defaultSize={55}
          minSize={25}
          className="flex flex-col overflow-hidden"
        >
          {/* Header Bar */}
          <div className="flex h-9 shrink-0 items-center justify-between border-b border-border/80 bg-muted/20 px-3 select-none">
            {/* Case Tabs Strip */}
            <div className="no-scrollbar flex min-w-0 items-center gap-1 overflow-x-auto py-1">
              {testCases.map((tc, idx) => {
                const isActive = idx === activeIndex
                return (
                  <button
                    key={tc.id || idx}
                    type="button"
                    onClick={() => setActiveCaseTab(idx)}
                    className={cn(
                      "flex h-7 shrink-0 cursor-pointer items-center gap-1.5 rounded-md px-2.5 text-xs font-medium transition-colors",
                      isActive
                        ? "border border-border/80 bg-background text-foreground shadow-2xs"
                        : "text-muted-foreground hover:bg-muted/60 hover:text-foreground"
                    )}
                  >
                    <span>{tc.title || `Case ${idx + 1}`}</span>
                  </button>
                )
              })}

              <Tooltip>
                <TooltipTrigger
                  render={
                    <button
                      type="button"
                      onClick={handleAddTestCase}
                      className="flex size-6 shrink-0 cursor-pointer items-center justify-center rounded-md border border-dashed border-border/80 text-muted-foreground transition-colors hover:border-foreground hover:text-foreground"
                    >
                      <Plus className="size-3" />
                    </button>
                  }
                />
                <TooltipContent>Add Test Case</TooltipContent>
              </Tooltip>
            </div>

            {/* Case Actions */}
            {activeCase && (
              <div className="flex shrink-0 items-center gap-0.5 pl-2">
                <Tooltip>
                  <TooltipTrigger
                    render={
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon-xs"
                        disabled={activeIndex === 0}
                        onClick={() => handleMoveTestCase(activeIndex, -1)}
                        className="h-6 w-6 cursor-pointer text-muted-foreground hover:text-foreground disabled:opacity-30"
                      >
                        <ChevronUp className="size-3.5" />
                      </Button>
                    }
                  />
                  <TooltipContent>Move Earlier</TooltipContent>
                </Tooltip>

                <Tooltip>
                  <TooltipTrigger
                    render={
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon-xs"
                        disabled={activeIndex === testCases.length - 1}
                        onClick={() => handleMoveTestCase(activeIndex, 1)}
                        className="h-6 w-6 cursor-pointer text-muted-foreground hover:text-foreground disabled:opacity-30"
                      >
                        <ChevronDown className="size-3.5" />
                      </Button>
                    }
                  />
                  <TooltipContent>Move Later</TooltipContent>
                </Tooltip>

                <Tooltip>
                  <TooltipTrigger
                    render={
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon-xs"
                        onClick={() => handleDuplicateTestCase(activeIndex)}
                        className="h-6 w-6 cursor-pointer text-muted-foreground hover:text-foreground"
                      >
                        <Copy className="size-3.5" />
                      </Button>
                    }
                  />
                  <TooltipContent>Duplicate Case</TooltipContent>
                </Tooltip>

                {testCases.length > 1 && (
                  <Tooltip>
                    <TooltipTrigger
                      render={
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon-xs"
                          onClick={() => handleRemoveTestCase(activeIndex)}
                          className="h-6 w-6 cursor-pointer text-destructive hover:bg-destructive/10"
                        >
                          <Trash2 className="size-3.5" />
                        </Button>
                      }
                    />
                    <TooltipContent>Delete Case</TooltipContent>
                  </Tooltip>
                )}
              </div>
            )}
          </div>

          {/* Test Case Input Canvas */}
          <div className="min-h-0 w-full flex-1 overflow-hidden bg-background p-3">
            {activeCase ? (
              <div className="flex h-full flex-col gap-1.5">
                <div className="flex items-center justify-between">
                  <span className="font-mono text-[11px] font-semibold text-muted-foreground">
                    {problemType === "DATABASE"
                      ? "SQL Init Script (DDL / DML)"
                      : "Standard Input (stdin)"}
                  </span>
                  <span className="text-[10px] text-muted-foreground">
                    Synced with Statement
                  </span>
                </div>

                <div className="min-h-0 w-full flex-1 overflow-hidden rounded-md border border-border/70 bg-card">
                  {problemType === "DATABASE" ? (
                    <MonacoEditor
                      key={`tc-sql-${activeCase.id}`}
                      language="sql"
                      value={activeCase.input}
                      onChange={(val) => handleUpdateInput(val)}
                      height="100%"
                      width="100%"
                      bordered={false}
                      placeholder="-- CREATE TABLE Person (...)&#10;-- INSERT INTO Person VALUES (...);"
                    />
                  ) : (
                    <Textarea
                      value={activeCase.input}
                      onChange={(e) => handleUpdateInput(e.target.value)}
                      placeholder="[2,7,11,15]&#10;9"
                      className="h-full w-full resize-none border-0 bg-transparent p-2.5 font-mono text-xs focus-visible:ring-0"
                    />
                  )}
                </div>
              </div>
            ) : (
              <div className="flex h-full items-center justify-center text-xs text-muted-foreground">
                No test cases defined. Click &quot;+&quot; to add one.
              </div>
            )}
          </div>
        </ResizablePanel>

        <ResizableHandle withHandle />

        {/* Bottom Pane: Verification & Execution Console */}
        <ResizablePanel
          defaultSize={45}
          minSize={20}
          className="flex flex-col overflow-hidden bg-background"
        >
          {/* Console Header Bar */}
          <div className="flex h-9 shrink-0 items-center justify-between border-b border-border/80 bg-muted/20 px-3 select-none">
            <div className="flex items-center gap-2">
              <Terminal className="size-3.5 text-primary" />
              <span className="text-xs font-semibold text-foreground">
                Verification Console
              </span>

              {testResults && (
                <Badge
                  variant="outline"
                  className={cn(
                    "font-mono text-[10px]",
                    allPassed
                      ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
                      : "border-destructive/30 bg-destructive/10 text-destructive"
                  )}
                >
                  {allPassed ? (
                    <span className="flex items-center gap-1">
                      <CheckCircle2 className="size-3" />
                      Passed ({testResults.length}/{testCases.length})
                    </span>
                  ) : (
                    <span className="flex items-center gap-1">
                      <XCircle className="size-3" />
                      Failed
                    </span>
                  )}
                </Badge>
              )}
            </div>

            {/* Quick Run Trigger */}
            <Button
              type="button"
              variant="ghost"
              size="xs"
              disabled={isTesting}
              onClick={onRunTestcases}
              className="h-6 cursor-pointer gap-1 px-2 text-[11px] text-primary hover:bg-primary/10"
            >
              {isTesting ? (
                <Loader2 className="size-3 animate-spin" />
              ) : (
                <Play className="size-3 fill-current" />
              )}
              <span>{isTesting ? "Testing..." : "Run"}</span>
            </Button>
          </div>

          {/* Console Body Canvas */}
          <div className="min-h-0 w-full flex-1 overflow-hidden">
            {isTesting ? (
              <div className="flex h-full flex-col items-center justify-center gap-2 text-xs text-muted-foreground">
                <Loader2 className="size-5 animate-spin text-primary" />
                <span>
                  Executing reference solution against sample testcases...
                </span>
              </div>
            ) : testResults && testResults.length > 0 ? (
              <div className="flex h-full flex-col overflow-hidden">
                {/* Result Case Tabs */}
                <div className="no-scrollbar flex shrink-0 items-center gap-1 overflow-x-auto border-b border-border/60 bg-muted/10 px-3 py-1.5">
                  {testResults.map((r, idx) => {
                    const isPassed = r.status === "PASSED"
                    const isSelected = idx === activeResultIndex

                    return (
                      <button
                        key={r.testCaseId || idx}
                        type="button"
                        onClick={() => setActiveResultIndex(idx)}
                        className={cn(
                          "flex h-6 shrink-0 cursor-pointer items-center gap-1.5 rounded px-2 text-[11px] font-medium transition-colors",
                          isSelected
                            ? "border border-border/80 bg-card text-foreground shadow-2xs"
                            : "text-muted-foreground hover:bg-muted/50 hover:text-foreground"
                        )}
                      >
                        {isPassed ? (
                          <CheckCircle2 className="size-3 text-emerald-500" />
                        ) : (
                          <XCircle className="size-3 text-destructive" />
                        )}
                        <span>Case {r.caseIndex}</span>
                        <span className="font-mono text-[9px] opacity-70">
                          {r.runtimeMs}ms
                        </span>
                      </button>
                    )
                  })}
                </div>

                {/* Selected Case Result Details */}
                {currentResult && (
                  <div className="min-h-0 flex-1 space-y-3 overflow-y-auto p-3 font-mono text-xs">
                    {/* Execution Metrics Pill Strip */}
                    <div className="flex items-center gap-3 text-[11px] text-muted-foreground">
                      <div className="flex items-center gap-1">
                        <Clock className="size-3 text-primary" />
                        <span>Runtime: {currentResult.runtimeMs} ms</span>
                      </div>
                      <div className="flex items-center gap-1">
                        <Cpu className="size-3 text-primary" />
                        <span>Memory: {currentResult.memoryMb} MB</span>
                      </div>
                    </div>

                    {/* Output Section */}
                    <div>
                      <div className="mb-1 text-[10px] font-semibold tracking-wider text-muted-foreground uppercase">
                        Output
                      </div>
                      <pre className="rounded-md border border-border/70 bg-muted/30 p-2 text-[11px] whitespace-pre-wrap text-foreground">
                        {currentResult.actualOutput}
                      </pre>
                    </div>

                    {/* Execution Logs */}
                    {currentResult.logs && (
                      <div>
                        <div className="mb-1 text-[10px] font-semibold tracking-wider text-muted-foreground uppercase">
                          Validator Logs
                        </div>
                        <pre className="rounded-md border border-border/70 bg-muted/30 p-2 text-[11px] whitespace-pre-wrap text-muted-foreground">
                          {currentResult.logs}
                        </pre>
                      </div>
                    )}
                  </div>
                )}
              </div>
            ) : (
              /* Idle Empty State */
              <div className="flex h-full flex-col items-center justify-center gap-3 p-5 text-center text-xs text-muted-foreground">
                <Terminal className="size-8 text-muted-foreground opacity-30" />
                <div className="max-w-xs space-y-1">
                  <p className="font-medium text-foreground">
                    Verification Console Ready
                  </p>
                  <p className="text-[11px] text-muted-foreground">
                    Run the reference solution to verify validator assertions
                    across all sample test cases.
                  </p>
                </div>
                <Button
                  type="button"
                  size="xs"
                  variant="outline"
                  onClick={onRunTestcases}
                  className="cursor-pointer gap-1.5 shadow-2xs"
                >
                  <Play className="size-3 fill-current" />
                  <span>Run Reference Solution</span>
                </Button>
              </div>
            )}
          </div>
        </ResizablePanel>
      </ResizablePanelGroup>
    </div>
  )
}
