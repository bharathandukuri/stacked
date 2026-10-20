import { useState } from "react";
import {
  Plus,
  X,
  Lock,
  Play,
  Loader2,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Clock,
  Cpu,
  Terminal,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { MonacoEditor } from "@/components/monaco-editor";
import { LineNumberedViewer } from "@/components/line-numbered-viewer";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { cn } from "@/lib/utils";
import { useCodingProblemStore, type TestResultItem } from "../../store";

export interface HiddenTestcasesPanelProps {
  className?: string;
}

export function HiddenTestcasesPanel({ className }: HiddenTestcasesPanelProps) {
  const values = useCodingProblemStore((state) => state.values);
  const hiddenTestcases = useCodingProblemStore(
    (state) => state.hiddenTestcases
  );
  const activeHiddenTestcaseIndex = useCodingProblemStore(
    (state) => state.activeHiddenTestcaseIndex
  );
  const activeHiddenTab = useCodingProblemStore(
    (state) => state.activeHiddenTab
  );
  const isTestingHidden = useCodingProblemStore(
    (state) => state.isTestingHidden
  );
  const hiddenTestResults = useCodingProblemStore(
    (state) => state.hiddenTestResults
  );
  const actions = useCodingProblemStore((state) => state.actions);

  const isDatabase = values.problemType === "DATABASE";
  const safeIndex = Math.min(
    activeHiddenTestcaseIndex,
    Math.max(0, hiddenTestcases.length - 1)
  );
  const activeCase = hiddenTestcases[safeIndex];
  const isMaxReached = hiddenTestcases.length >= 20;

  // Selected result in Results tab
  const [selectedResultIndex, setSelectedResultIndex] = useState(0);

  const currentResult: TestResultItem | undefined =
    hiddenTestResults && hiddenTestResults.length > 0
      ? hiddenTestResults[
          Math.min(selectedResultIndex, hiddenTestResults.length - 1)
        ]
      : undefined;

  const passedCount = hiddenTestResults
    ? hiddenTestResults.filter(
        (r) => r.status === "SUCCESS" || r.status === "PASSED"
      ).length
    : 0;
  const totalResults = hiddenTestResults ? hiddenTestResults.length : 0;
  const allPassed =
    hiddenTestResults && totalResults > 0 && passedCount === totalResults;

  return (
    <div
      className={cn(
        "flex h-full w-full flex-col overflow-hidden bg-background select-none",
        className
      )}
    >
      {/* 1. Top Main Navigation Header: Hidden Testcases | Results | Run Button */}
      <div className="flex h-10 shrink-0 items-center justify-between border-b border-border/80 bg-muted/20 px-3">
        {/* Left: Tab Switcher (Testcases vs Results) */}
        <div className="flex items-center gap-1">
          {/* Tab 1: Hidden Testcases */}
          <button
            type="button"
            onClick={() => actions.setActiveHiddenTab("testcases")}
            className={cn(
              "flex h-7 cursor-pointer items-center gap-1.5 rounded-md px-2.5 text-xs font-medium transition-colors",
              activeHiddenTab === "testcases"
                ? "bg-background text-foreground shadow-2xs font-semibold"
                : "text-muted-foreground hover:bg-muted/60 hover:text-foreground"
            )}
          >
            <Lock className="size-3.5 text-primary" />
            <span>Hidden Testcases</span>
            <Badge
              variant="secondary"
              className="h-4 px-1 text-[9px] font-mono leading-none"
            >
              {hiddenTestcases.length}/20
            </Badge>
          </button>

          {/* Tab 2: Results */}
          <button
            type="button"
            onClick={() => actions.setActiveHiddenTab("results")}
            className={cn(
              "flex h-7 cursor-pointer items-center gap-1.5 rounded-md px-2.5 text-xs font-medium transition-colors",
              activeHiddenTab === "results"
                ? "bg-background text-foreground shadow-2xs font-semibold"
                : "text-muted-foreground hover:bg-muted/60 hover:text-foreground"
            )}
          >
            <CheckCircle2
              className={cn(
                "size-3.5",
                allPassed ? "text-emerald-500" : "text-muted-foreground"
              )}
            />
            <span>Results</span>
            {hiddenTestResults && (
              <Badge
                variant="outline"
                className={cn(
                  "h-4 px-1 text-[9px] font-mono leading-none",
                  allPassed
                    ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
                    : "border-destructive/30 bg-destructive/10 text-destructive"
                )}
              >
                {passedCount}/{totalResults}
              </Badge>
            )}
          </button>
        </div>

        {/* Right: Run Hidden Testcases Button */}
        <div className="flex items-center gap-2">
          <Button
            type="button"
            size="xs"
            disabled={isTestingHidden || hiddenTestcases.length === 0}
            onClick={() => actions.runHiddenTestcases()}
            className="cursor-pointer gap-1.5 bg-emerald-600 px-3 text-xs font-semibold text-white shadow-xs hover:bg-emerald-500"
          >
            {isTestingHidden ? (
              <Loader2 className="size-3 animate-spin" />
            ) : (
              <Play className="size-3 fill-current" />
            )}
            <span>{isTestingHidden ? "Running..." : "Run"}</span>
          </Button>
        </div>
      </div>

      {/* ====================================================================
          TAB 1: HIDDEN TESTCASES
         ==================================================================== */}
      {activeHiddenTab === "testcases" && (
        <div className="flex h-full flex-col overflow-hidden">
          {/* Testcases Strip Header */}
          <div className="flex h-10 shrink-0 items-center border-b border-border/70 bg-muted/10 px-3">
            {/* Horizontal Case Tabs */}
            <div className="no-scrollbar flex min-w-0 items-center gap-2 overflow-x-auto py-2">
              {hiddenTestcases.map((tc, idx) => {
                const isActive = idx === safeIndex;

                return (
                  <div
                    key={tc.id || idx}
                    onClick={() => actions.setActiveHiddenTestcaseIndex(idx)}
                    className={cn(
                      "group relative flex h-7 shrink-0 cursor-pointer items-center rounded-md px-3 text-xs font-medium transition-colors select-none",
                      isActive
                        ? "border border-border/80 bg-background text-foreground shadow-2xs font-semibold"
                        : "text-muted-foreground hover:bg-muted/60 hover:text-foreground"
                    )}
                  >
                    <span>{tc.title || `Case ${idx + 1}`}</span>

                    {/* Delete bubble at top right */}
                    {hiddenTestcases.length > 1 && (
                      <span
                        role="button"
                        tabIndex={0}
                        title="Delete testcase"
                        onClick={(e) => {
                          e.stopPropagation();
                          actions.removeHiddenTestcase(idx);
                        }}
                        className="absolute -top-1.5 -right-1.5 flex size-3.5 cursor-pointer items-center justify-center rounded-full border border-border/80 bg-background text-muted-foreground shadow-2xs transition-colors hover:border-destructive hover:bg-destructive hover:text-white"
                      >
                        <X className="size-2" />
                      </span>
                    )}
                  </div>
                );
              })}

              {/* Add / Duplicate Testcase Button at End of Strip */}
              <Tooltip>
                <TooltipTrigger
                  render={
                    <button
                      type="button"
                      disabled={isMaxReached}
                      onClick={() => {
                        if (hiddenTestcases.length === 0) {
                          actions.addHiddenTestcase();
                        } else {
                          actions.duplicateHiddenTestcase(safeIndex);
                        }
                      }}
                      className={cn(
                        "flex size-7 shrink-0 cursor-pointer items-center justify-center rounded-md border border-dashed border-border text-muted-foreground transition-colors",
                        isMaxReached
                          ? "cursor-not-allowed opacity-50"
                          : "hover:border-foreground hover:text-foreground"
                      )}
                    >
                      <Plus className="size-3.5" />
                    </button>
                  }
                />
                <TooltipContent>
                  {isMaxReached
                    ? "Maximum 20 hidden testcases allowed"
                    : "Duplicate Testcase"}
                </TooltipContent>
              </Tooltip>
            </div>
          </div>

          {/* Test Case Input Canvas */}
          <div className="min-h-0 w-full flex-1 overflow-hidden p-3 bg-background">
            {activeCase ? (
              <div className="flex h-full flex-col gap-2">
                <div className="flex items-center justify-between">
                  <span className="font-mono text-xs font-semibold text-foreground">
                    {isDatabase ? "dbInit" : "stdin"}
                  </span>
                  <span className="font-mono text-[10px] text-muted-foreground">
                    {isDatabase
                      ? `${activeCase.dbInit?.length || 0} chars`
                      : `${activeCase.stdin?.length || 0} chars • ${
                          (activeCase.stdin || "").split("\n").length
                        } lines`}
                  </span>
                </div>

                <div className="min-h-0 w-full flex-1 overflow-hidden rounded-lg border border-border/80 bg-card">
                  <MonacoEditor
                    language={isDatabase ? "sql" : "plaintext"}
                    value={
                      isDatabase
                        ? (activeCase.dbInit ?? "")
                        : (activeCase.stdin ?? "")
                    }
                    onChange={(val) => {
                      if (isDatabase) {
                        actions.updateHiddenTestcase(safeIndex, {
                          dbInit: val,
                        });
                      } else {
                        actions.updateHiddenTestcase(safeIndex, {
                          stdin: val,
                        });
                      }
                    }}
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
                    placeholder={
                      isDatabase
                        ? "-- Hidden testcase initialization SQL\n-- INSERT INTO Person VALUES (...);"
                        : "Enter hidden testcase standard input..."
                    }
                  />
                </div>
              </div>
            ) : (
              <div className="flex h-full flex-col items-center justify-center gap-2 text-xs text-muted-foreground">
                <p>No hidden testcases defined.</p>
                <Button
                  type="button"
                  size="xs"
                  onClick={() => actions.addHiddenTestcase()}
                >
                  Add Hidden Testcase
                </Button>
              </div>
            )}
          </div>
        </div>
      )}

      {/* ====================================================================
          TAB 2: RESULTS
         ==================================================================== */}
      {activeHiddenTab === "results" && (
        <div className="flex h-full flex-col overflow-hidden">
          {isTestingHidden ? (
            <div className="flex h-full flex-col items-center justify-center gap-2.5 text-xs text-muted-foreground">
              <Loader2 className="size-6 animate-spin text-primary" />
              <p className="font-medium text-foreground">
                Running Reference Solution...
              </p>
              <p className="text-[11px] text-muted-foreground">
                Executing against {hiddenTestcases.length} hidden testcase(s) in
                sandbox.
              </p>
            </div>
          ) : hiddenTestResults && hiddenTestResults.length > 0 ? (
            <div className="flex h-full flex-col overflow-hidden">
              {/* Results Case Tabs */}
              <div className="no-scrollbar flex shrink-0 items-center gap-1 overflow-x-auto border-b border-border/70 bg-muted/10 px-3 py-1.5">
                {hiddenTestResults.map((r, idx) => {
                  const isPassed =
                    r.status === "SUCCESS" || r.status === "PASSED";
                  const isSelected = idx === selectedResultIndex;

                  return (
                    <button
                      key={r.testCaseId || idx}
                      type="button"
                      onClick={() => setSelectedResultIndex(idx)}
                      className={cn(
                        "flex h-6.5 shrink-0 cursor-pointer items-center gap-1.5 rounded-md px-2 text-[11px] font-medium transition-colors",
                        isSelected
                          ? "border border-border/80 bg-background text-foreground shadow-2xs font-semibold"
                          : "text-muted-foreground hover:bg-muted/50 hover:text-foreground"
                      )}
                    >
                      {isPassed ? (
                        <CheckCircle2 className="size-3 text-emerald-500" />
                      ) : (
                        <XCircle className="size-3 text-destructive" />
                      )}
                      <span>Case {r.caseIndex}</span>
                      {r.runtimeMs !== undefined && (
                        <span className="font-mono text-[9px] opacity-70">
                          {r.runtimeMs}ms
                        </span>
                      )}
                    </button>
                  );
                })}
              </div>

              {/* Selected Case Result Details */}
              {currentResult && (
                <div className="min-h-0 flex-1 space-y-3.5 overflow-y-auto p-4 font-mono text-xs">
                  {/* Status & Metrics Bar */}
                  <div className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-border/80 bg-muted/20 p-2.5">
                    <div className="flex items-center gap-2">
                      {currentResult.status === "SUCCESS" ||
                      currentResult.status === "PASSED" ? (
                        <Badge className="gap-1 bg-emerald-500/10 text-emerald-600 border-emerald-500/30 dark:text-emerald-400">
                          <CheckCircle2 className="size-3" />
                          <span>Passed</span>
                        </Badge>
                      ) : currentResult.status === "COMPILATION_ERROR" ? (
                        <Badge className="gap-1 bg-rose-500/10 text-rose-600 border-rose-500/30 dark:text-rose-400">
                          <AlertTriangle className="size-3" />
                          <span>Compilation Error</span>
                        </Badge>
                      ) : currentResult.status === "TIME_LIMIT_EXCEEDED" ? (
                        <Badge className="gap-1 bg-amber-500/10 text-amber-600 border-amber-500/30 dark:text-amber-400">
                          <Clock className="size-3" />
                          <span>Time Limit Exceeded</span>
                        </Badge>
                      ) : currentResult.status === "MEMORY_LIMIT_EXCEEDED" ? (
                        <Badge className="gap-1 bg-amber-500/10 text-amber-600 border-amber-500/30 dark:text-amber-400">
                          <Cpu className="size-3" />
                          <span>Memory Limit Exceeded</span>
                        </Badge>
                      ) : (
                        <Badge className="gap-1 bg-rose-500/10 text-rose-600 border-rose-500/30 dark:text-rose-400">
                          <XCircle className="size-3" />
                          <span>Runtime Error</span>
                        </Badge>
                      )}
                    </div>

                    <div className="flex items-center gap-3 text-[11px] text-muted-foreground">
                      {currentResult.runtimeMs !== undefined && (
                        <div className="flex items-center gap-1">
                          <Clock className="size-3 text-primary" />
                          <span>{currentResult.runtimeMs} ms</span>
                        </div>
                      )}
                      {currentResult.memoryMb !== undefined && (
                        <div className="flex items-center gap-1">
                          <Cpu className="size-3 text-primary" />
                          <span>{currentResult.memoryMb} MB</span>
                        </div>
                      )}
                      {currentResult.exitCode !== undefined && (
                        <div className="flex items-center gap-1">
                          <Terminal className="size-3 text-muted-foreground" />
                          <span>Exit: {currentResult.exitCode}</span>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Testcase Input */}
                  {currentResult.input !== undefined && (
                    <div>
                      <div className="mb-1 text-[10px] font-semibold tracking-wider text-muted-foreground uppercase">
                        {isDatabase ? "dbInit (SQL)" : "stdin"}
                      </div>
                      <LineNumberedViewer
                        content={currentResult.input || ""}
                        language={isDatabase ? "sql" : "plaintext"}
                        maxHeight="140px"
                        emptyText={
                          isDatabase
                            ? "(Empty database initialization)"
                            : "(Empty standard input)"
                        }
                      />
                    </div>
                  )}

                  {/* Standard Output */}
                  <div>
                    <div className="mb-1 text-[10px] font-semibold tracking-wider text-muted-foreground uppercase">
                      Standard Output
                    </div>
                    <LineNumberedViewer
                      content={currentResult.actualOutput || ""}
                      maxHeight="160px"
                      emptyText="(No stdout generated)"
                    />
                  </div>

                  {/* Standard Error if present */}
                  {currentResult.stderr && (
                    <div>
                      <div className="mb-1 text-[10px] font-semibold tracking-wider text-destructive uppercase">
                        Standard Error
                      </div>
                      <LineNumberedViewer
                        content={currentResult.stderr}
                        variant="error"
                        maxHeight="160px"
                      />
                    </div>
                  )}

                  {/* Expected Output / Validator status */}
                  {currentResult.expectedOutput && (
                    <div>
                      <div className="mb-1 text-[10px] font-semibold tracking-wider text-muted-foreground uppercase">
                        Verification Script Output
                      </div>
                      <LineNumberedViewer
                        content={currentResult.expectedOutput}
                        variant="muted"
                        maxHeight="140px"
                      />
                    </div>
                  )}
                </div>
              )}
            </div>
          ) : (
            /* Empty State: Haven't run tests yet */
            <div className="flex h-full flex-col items-center justify-center gap-3 p-6 text-center select-none">
              <div className="flex size-12 items-center justify-center rounded-2xl border border-border/80 bg-muted/30">
                <Terminal className="size-6 text-muted-foreground/50" />
              </div>
              <div className="max-w-xs space-y-1">
                <p className="text-sm font-semibold text-foreground">
                  No Execution Results Yet
                </p>
                <p className="text-xs text-muted-foreground leading-relaxed">
                  Run the reference solution to evaluate execution against all
                  hidden testcases and inspect runtime metrics.
                </p>
              </div>
              <Button
                type="button"
                size="sm"
                onClick={() => actions.runHiddenTestcases()}
                className="cursor-pointer gap-1.5 shadow-xs bg-emerald-600 text-white hover:bg-emerald-500"
              >
                <Play className="size-3.5 fill-current" />
                <span>Run Hidden Testcases</span>
              </Button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default HiddenTestcasesPanel;
