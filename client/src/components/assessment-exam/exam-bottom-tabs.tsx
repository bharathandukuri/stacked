import {
  FileCode,
  Terminal,
  CheckCircle2,
  Play,
  Loader2,
  AlertCircle,
  Check,
  History,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import { Badge } from "@/components/ui/badge"
import { cn } from "@/lib/utils"
import type {
  AssessmentItemResponse,
  SavedQuestionResponse,
  CodingProblemExecutionResponse,
} from "@/types/assessment"

interface ExamBottomTabsProps {
  question: AssessmentItemResponse
  activeTab: "sample" | "custom" | "submit"
  onTabChange: (tab: "sample" | "custom" | "submit") => void
  // Sample Testcases
  isSampleRunning: boolean
  sampleResult: CodingProblemExecutionResponse | null
  selectedSampleIndex: number
  onSelectSampleIndex: (index: number) => void
  // Custom Input
  customInputText: string
  onCustomInputChange: (text: string) => void
  isCustomRunning: boolean
  customResult: CodingProblemExecutionResponse | null
  onRunCustomInput: () => void
  // Submission Output
  isSubmittingCode: boolean
  submitResult: SavedQuestionResponse | null
  savedResponse: SavedQuestionResponse | undefined
}

export function ExamBottomTabs({
  question,
  activeTab,
  onTabChange,
  isSampleRunning,
  sampleResult,
  selectedSampleIndex,
  onSelectSampleIndex,
  customInputText,
  onCustomInputChange,
  isCustomRunning,
  customResult,
  onRunCustomInput,
  isSubmittingCode,
  submitResult,
  savedResponse,
}: ExamBottomTabsProps) {
  return (
    <div className="h-full flex flex-col bg-card/20 min-w-0">
      {/* Tab Navigation Header */}
      <div className="h-9 border-b border-border bg-card/50 px-4 flex items-center gap-4 text-xs font-medium shrink-0">
        <button
          type="button"
          onClick={() => onTabChange("sample")}
          className={cn(
            "pb-1 border-b-2 transition-colors cursor-pointer flex items-center gap-1.5",
            activeTab === "sample"
              ? "border-primary text-foreground font-semibold"
              : "border-transparent text-muted-foreground hover:text-foreground"
          )}
        >
          <FileCode className="size-3.5" />
          <span>Sample Testcases</span>
        </button>

        <button
          type="button"
          onClick={() => onTabChange("custom")}
          className={cn(
            "pb-1 border-b-2 transition-colors cursor-pointer flex items-center gap-1.5",
            activeTab === "custom"
              ? "border-primary text-foreground font-semibold"
              : "border-transparent text-muted-foreground hover:text-foreground"
          )}
        >
          <Terminal className="size-3.5" />
          <span>Custom Input</span>
        </button>

        <button
          type="button"
          onClick={() => onTabChange("submit")}
          className={cn(
            "pb-1 border-b-2 transition-colors cursor-pointer flex items-center gap-1.5",
            activeTab === "submit"
              ? "border-primary text-foreground font-semibold"
              : "border-transparent text-muted-foreground hover:text-foreground"
          )}
        >
          <CheckCircle2 className="size-3.5" />
          <span>Submission Output</span>
        </button>
      </div>

      {/* Tab Content Body */}
      <div className="flex-1 overflow-y-auto p-4 text-xs">
        {/* TAB 1: Sample Testcases */}
        {activeTab === "sample" && (
          <div>
            {isSampleRunning ? (
              <div className="flex items-center gap-2 text-muted-foreground py-6">
                <Loader2 className="size-4 animate-spin text-primary" />
                <span>Executing code against sample test cases in sandbox...</span>
              </div>
            ) : sampleResult ? (
              (() => {
                const isOverallAccepted =
                  sampleResult.overallStatus?.toUpperCase() === "ACCEPTED" ||
                  sampleResult.overallStatus?.toUpperCase() === "SUCCESS"
                return (
                  <div className="space-y-4">
                    {/* Status Header */}
                    <div className="flex items-center justify-between pb-2 border-b border-border/60">
                      <div className="flex items-center gap-2">
                        <span
                          className={cn(
                            "inline-flex items-center px-2.5 py-0.5 rounded-md text-xs font-semibold border",
                            isOverallAccepted
                              ? "bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border-emerald-500/30"
                              : "bg-rose-500/15 text-rose-500 dark:text-rose-400 border-rose-500/30"
                          )}
                        >
                          {sampleResult.overallStatus}
                        </span>
                        <span className="text-muted-foreground font-medium text-xs">
                          Passed {sampleResult.passedTestcases} /{" "}
                          {sampleResult.totalTestcases} test cases
                        </span>
                      </div>
                      <span className="text-muted-foreground font-mono text-xs">
                        {sampleResult.totalExecutionTimeMs} ms
                      </span>
                    </div>

                    {/* Testcase Pill Selectors */}
                    <div className="flex items-center gap-2 overflow-x-auto pb-1">
                      {sampleResult.results?.map((res, idx) => {
                        const isCasePassed =
                          res.passed === true ||
                          res.status?.toUpperCase() === "PASSED" ||
                          res.status?.toUpperCase() === "ACCEPTED" ||
                          res.status?.toUpperCase() === "SUCCESS"
                        return (
                          <button
                            key={idx}
                            type="button"
                            onClick={() => onSelectSampleIndex(idx)}
                            className={cn(
                              "px-3 py-1.5 rounded-lg border text-xs font-medium transition-all flex items-center gap-1.5 cursor-pointer",
                              selectedSampleIndex === idx
                                ? isCasePassed
                                  ? "border-emerald-500/60 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 ring-1 ring-emerald-500/40"
                                  : "border-rose-500/60 bg-rose-500/10 text-rose-500 dark:text-rose-400 ring-1 ring-rose-500/40"
                                : "border-border/80 hover:bg-muted/40 text-muted-foreground"
                            )}
                          >
                            <span
                              className={cn(
                                "size-2 rounded-full",
                                isCasePassed ? "bg-emerald-500" : "bg-rose-500"
                              )}
                            />
                            <span>Case {idx + 1}</span>
                          </button>
                        )
                      })}
                    </div>

                    {/* Selected Case Detail: STACKED VERTICALLY ONE BELOW ANOTHER ONLY */}
                    {sampleResult.results?.[selectedSampleIndex] &&
                      (() => {
                        const res = sampleResult.results[selectedSampleIndex]
                        const isSelectedPassed =
                          res.passed === true ||
                          res.status?.toUpperCase() === "PASSED" ||
                          res.status?.toUpperCase() === "ACCEPTED" ||
                          res.status?.toUpperCase() === "SUCCESS"
                        const currentSample =
                          question.sampleTestCases?.[selectedSampleIndex]
                        const inputContent = res.stdin ?? currentSample?.stdin
                        const expectedContent =
                          res.expectedOutput ?? currentSample?.output

                        return (
                          <div className="space-y-3 pt-2">
                            <div className="flex items-center justify-between">
                              <span
                                className={cn(
                                  "inline-flex items-center px-2 py-0.5 rounded-md text-[11px] font-semibold border",
                                  isSelectedPassed
                                    ? "bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border-emerald-500/30"
                                    : "bg-rose-500/15 text-rose-500 dark:text-rose-400 border-rose-500/30"
                                )}
                              >
                                {res.status}
                              </span>
                              <span className="text-[11px] text-muted-foreground font-mono">
                                {res.runtimeMs ?? res.executionTimeMs ?? 0} ms
                              </span>
                            </div>

                            {/* Stacked Vertically: Input -> Your Output -> Expected Output */}
                            <div className="space-y-3">
                              <div className="space-y-1">
                                <span className="text-[11px] font-semibold text-muted-foreground">
                                  Input (stdin):
                                </span>
                                <pre className="font-mono p-2.5 bg-background border rounded-lg text-xs whitespace-pre-wrap min-h-[38px]">
                                  {inputContent !== undefined &&
                                  inputContent !== null &&
                                  inputContent !== ""
                                    ? inputContent
                                    : "(no input)"}
                                </pre>
                              </div>

                              <div className="space-y-1">
                                <span className="text-[11px] font-semibold text-muted-foreground">
                                  Your Output:
                                </span>
                                <pre className="font-mono p-2.5 bg-background border rounded-lg text-xs whitespace-pre-wrap min-h-[48px]">
                                  {res.actualOutput || "(empty)"}
                                </pre>
                              </div>

                              <div className="space-y-1">
                                <span className="text-[11px] font-semibold text-muted-foreground">
                                  Expected Output:
                                </span>
                                <pre className="font-mono p-2.5 bg-background border rounded-lg text-xs whitespace-pre-wrap min-h-[48px]">
                                  {expectedContent || "(empty)"}
                                </pre>
                              </div>
                            </div>

                            {/* Stderr Output */}
                            {res.stderr && (
                              <div className="space-y-1 pt-1">
                                <span className="text-[11px] font-semibold text-rose-500 dark:text-rose-400">
                                  Error / Compiler / Stderr Output:
                                </span>
                                <pre className="font-mono p-2.5 bg-rose-500/10 text-rose-500 dark:text-rose-400 border border-rose-500/30 rounded-lg text-xs whitespace-pre-wrap">
                                  {res.stderr}
                                </pre>
                              </div>
                            )}
                          </div>
                        )
                      })()}
                  </div>
                )
              })()
            ) : (
              <p className="text-muted-foreground py-6 text-center text-xs">
                Click "Run Tests" above to execute your solution against sample
                test cases.
              </p>
            )}
          </div>
        )}

        {/* TAB 2: Custom Input Runner */}
        {activeTab === "custom" && (
          <div className="space-y-4">
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <label className="text-xs font-semibold text-foreground">
                  Standard Input (stdin):
                </label>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={onRunCustomInput}
                  disabled={isCustomRunning}
                  className="h-7 text-xs gap-1.5"
                >
                  {isCustomRunning ? (
                    <Loader2 className="size-3 animate-spin text-primary" />
                  ) : (
                    <Play className="size-3 text-emerald-500 fill-emerald-500" />
                  )}
                  <span>Run Custom Input</span>
                </Button>
              </div>
              <Textarea
                placeholder="Enter standard input values here to test edge cases..."
                rows={3}
                value={customInputText}
                onChange={(e) => onCustomInputChange(e.target.value)}
                className="font-mono text-xs"
              />
            </div>

            {/* Custom Output Result */}
            {isCustomRunning ? (
              <div className="flex items-center gap-2 text-muted-foreground py-3">
                <Loader2 className="size-4 animate-spin text-primary" />
                <span>Executing code with custom input in sandbox...</span>
              </div>
            ) : customResult ? (
              (() => {
                const isCustomSuccess =
                  customResult.overallStatus === "SUCCESS" ||
                  customResult.overallStatus === "ACCEPTED" ||
                  customResult.overallStatus === "PASSED"
                const customOut =
                  customResult.results?.[0]?.actualOutput ??
                  customResult.results?.[0]?.stdout
                const customExpected =
                  customResult.results?.[0]?.expectedOutput
                const customErr =
                  customResult.results?.[0]?.stderr ||
                  customResult.results?.[0]?.error

                return (
                  <div className="rounded-lg border p-3 bg-background space-y-3">
                    <div className="flex items-center justify-between pb-2 border-b">
                      <span
                        className={cn(
                          "inline-flex items-center px-2 py-0.5 rounded-md text-[11px] font-semibold border",
                          isCustomSuccess
                            ? "bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border-emerald-500/30"
                            : "bg-rose-500/15 text-rose-500 dark:text-rose-400 border-rose-500/30"
                        )}
                      >
                        {customResult.overallStatus}
                      </span>
                      <span className="text-muted-foreground font-mono text-xs">
                        {customResult.totalExecutionTimeMs} ms
                      </span>
                    </div>

                    {/* Stacked Vertically: Your Output -> Expected Output */}
                    <div className="space-y-3">
                      <div className="space-y-1">
                        <span className="text-[11px] font-semibold text-muted-foreground">
                          Your Output:
                        </span>
                        <pre className="font-mono p-2.5 bg-background border rounded-lg text-xs whitespace-pre-wrap min-h-[48px]">
                          {customOut !== undefined && customOut !== ""
                            ? customOut
                            : "(empty output)"}
                        </pre>
                      </div>

                      <div className="space-y-1">
                        <span className="text-[11px] font-semibold text-muted-foreground">
                          Expected Output:
                        </span>
                        <pre className="font-mono p-2.5 bg-background border rounded-lg text-xs whitespace-pre-wrap min-h-[48px]">
                          {customExpected !== undefined &&
                          customExpected !== ""
                            ? customExpected
                            : "(empty)"}
                        </pre>
                      </div>
                    </div>

                    {customErr && (
                      <div className="space-y-1 pt-1">
                        <span className="text-[11px] font-semibold text-rose-500 dark:text-rose-400">
                          Error / Compiler / Stderr Output:
                        </span>
                        <pre className="font-mono p-2.5 bg-rose-500/10 text-rose-500 dark:text-rose-400 border border-rose-500/30 rounded-lg text-xs whitespace-pre-wrap">
                          {customErr}
                        </pre>
                      </div>
                    )}
                  </div>
                )
              })()
            ) : (
              <p className="text-muted-foreground text-xs py-2">
                Provide standard input above and click "Run Custom Input".
              </p>
            )}
          </div>
        )}

        {/* TAB 3: Submission Output */}
        {activeTab === "submit" && (
          <div>
            {isSubmittingCode ? (
              <div className="flex items-center gap-2 text-muted-foreground py-6">
                <Loader2 className="size-4 animate-spin text-primary" />
                <span>Evaluating code against complete test suite...</span>
              </div>
            ) : submitResult || savedResponse?.submissions?.length ? (
              (() => {
                const activeResp = submitResult ?? savedResponse
                const lastExec = activeResp?.lastExecutionResult
                const status =
                  lastExec?.overallStatus ??
                  (activeResp?.allTestcasesPassed ? "ACCEPTED" : "FAILED")
                const isAccepted =
                  status === "ACCEPTED" ||
                  status === "SUCCESS" ||
                  activeResp?.allTestcasesPassed === true
                const passedCount =
                  lastExec?.passedTestcases ??
                  activeResp?.bestPassedTestcases ??
                  activeResp?.passedTestcases ??
                  0
                const totalCount =
                  lastExec?.totalTestcases ?? activeResp?.totalTestcases ?? 0
                const execTime = lastExec?.totalExecutionTimeMs ?? 0
                const bestPoints =
                  activeResp?.bestAwardedPoints ??
                  activeResp?.awardedPoints ??
                  0
                const maxPts =
                  activeResp?.maxPoints ?? question.points ?? 0
                const submissionsList = activeResp?.submissions ?? []

                return (
                  <div className="space-y-4">
                    {/* Top Verdict Banner */}
                    <div className="flex flex-wrap items-center justify-between gap-3 p-3 rounded-lg border bg-card/60">
                      <div className="flex items-center gap-2.5">
                        <span
                          className={cn(
                            "inline-flex items-center px-2.5 py-1 rounded-md text-xs font-semibold border",
                            isAccepted
                              ? "bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border-emerald-500/30"
                              : "bg-rose-500/15 text-rose-500 dark:text-rose-400 border-rose-500/30"
                          )}
                        >
                          {status}
                        </span>
                        <span className="text-xs font-semibold text-foreground">
                          Passed {passedCount} / {totalCount} test cases
                          {!isAccepted && totalCount > 0 && (
                            <span className="ml-1.5 text-xs text-rose-500 dark:text-rose-400 font-normal">
                              ({totalCount - passedCount} failed)
                            </span>
                          )}
                        </span>
                      </div>
                      <div className="flex items-center gap-3 text-xs text-muted-foreground">
                        {execTime > 0 && (
                          <span>
                            Runtime:{" "}
                            <strong className="font-mono text-foreground">
                              {execTime} ms
                            </strong>
                          </span>
                        )}
                        <span>
                          Best Score:{" "}
                          <strong className="font-mono text-emerald-600 dark:text-emerald-400">
                            {bestPoints.toFixed(1)} / {maxPts} pts
                          </strong>
                        </span>
                      </div>
                    </div>

                    {/* Helpful failure breakdown notification */}
                    {!isAccepted && totalCount > 0 && (
                      <div className="p-3 rounded-lg border border-rose-500/30 bg-rose-500/10 text-xs text-rose-500 dark:text-rose-400 space-y-1">
                        <div className="font-semibold flex items-center gap-1.5">
                          <AlertCircle className="size-4 shrink-0" />
                          <span>
                            Test cases failed: Passed {passedCount} of {totalCount} cases.
                          </span>
                        </div>
                        <p className="text-[11px] text-muted-foreground">
                          Your previous best score ({bestPoints.toFixed(1)} /{" "}
                          {maxPts} pts) is safely preserved. Test edge cases
                          with the custom runner and submit an updated solution.
                        </p>
                      </div>
                    )}

                    {/* Stderr / Compiler Output if failed */}
                    {lastExec?.logs && (
                      <div className="space-y-1">
                        <span className="text-[11px] font-semibold text-rose-500 dark:text-rose-400">
                          Compilation / Stderr Output:
                        </span>
                        <pre className="font-mono p-2.5 bg-rose-500/10 text-rose-500 dark:text-rose-400 border border-rose-500/30 rounded-lg text-xs whitespace-pre-wrap max-h-40 overflow-y-auto">
                          {lastExec.logs}
                        </pre>
                      </div>
                    )}

                    {/* Submission History Table */}
                    {submissionsList.length > 0 && (
                      <div className="space-y-2 pt-2 border-t">
                        <div className="flex items-center justify-between">
                          <span className="text-xs font-semibold text-foreground flex items-center gap-1.5">
                            <History className="size-3.5 text-muted-foreground" />
                            Submission History ({submissionsList.length})
                          </span>
                          <span className="text-[11px] text-muted-foreground">
                            Best score is preserved
                          </span>
                        </div>
                        <div className="rounded-md border overflow-hidden">
                          <table className="w-full text-xs text-left">
                            <thead className="bg-muted/50 text-muted-foreground font-medium border-b text-[11px]">
                              <tr>
                                <th className="px-3 py-2">#</th>
                                <th className="px-3 py-2">Verdict</th>
                                <th className="px-3 py-2">Test Cases</th>
                                <th className="px-3 py-2">Score</th>
                                <th className="px-3 py-2">Runtime</th>
                                <th className="px-3 py-2 text-right">Submitted</th>
                              </tr>
                            </thead>
                            <tbody className="divide-y divide-border/60">
                              {submissionsList.map((sub, sIdx) => {
                                const subAccepted =
                                  sub.status === "ACCEPTED" ||
                                  sub.status === "SUCCESS"
                                const isBest =
                                  sub.awardedPoints === bestPoints &&
                                  bestPoints > 0
                                return (
                                  <tr
                                    key={sub.submissionId || sIdx}
                                    className={cn(
                                      "hover:bg-muted/30 transition-colors",
                                      isBest && "bg-emerald-500/5"
                                    )}
                                  >
                                    <td className="px-3 py-2 text-muted-foreground font-mono">
                                      {sIdx + 1}
                                    </td>
                                    <td className="px-3 py-2">
                                      <span
                                        className={cn(
                                          "font-semibold inline-flex items-center gap-1",
                                          subAccepted
                                            ? "text-emerald-600 dark:text-emerald-400"
                                            : "text-rose-500 dark:text-rose-400"
                                        )}
                                      >
                                        {subAccepted ? (
                                          <Check className="size-3" />
                                        ) : (
                                          <AlertCircle className="size-3" />
                                        )}
                                        {sub.status}
                                      </span>
                                    </td>
                                    <td className="px-3 py-2 font-mono">
                                      {sub.passedTestcases} / {sub.totalTestcases}
                                    </td>
                                    <td className="px-3 py-2 font-mono">
                                      <span
                                        className={cn(
                                          isBest &&
                                            "font-bold text-emerald-600 dark:text-emerald-400"
                                        )}
                                      >
                                        {sub.awardedPoints.toFixed(1)} pts
                                      </span>
                                      {isBest && (
                                        <Badge
                                          variant="outline"
                                          className="ml-1.5 text-[9px] px-1 py-0 border-emerald-500/40 text-emerald-600 dark:text-emerald-400"
                                        >
                                          Best
                                        </Badge>
                                      )}
                                    </td>
                                    <td className="px-3 py-2 font-mono text-muted-foreground">
                                      {sub.executionTimeMs} ms
                                    </td>
                                    <td className="px-3 py-2 text-right text-muted-foreground">
                                      {new Date(
                                        sub.submittedAt
                                      ).toLocaleTimeString([], {
                                        hour: "2-digit",
                                        minute: "2-digit",
                                        second: "2-digit",
                                      })}
                                    </td>
                                  </tr>
                                )
                              })}
                            </tbody>
                          </table>
                        </div>
                      </div>
                    )}
                  </div>
                )
              })()
            ) : (
              <p className="text-muted-foreground py-6 text-center text-xs">
                Click "Submit Code" to evaluate your solution against all hidden
                and visible test cases.
              </p>
            )}
          </div>
        )}
      </div>
    </div>
  )
}

