import { FileCode, History, Check, AlertCircle } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { MonacoEditor } from "@/components/monaco-editor"
import { MarkdownPreview } from "@/components/markdown-preview/markdown-preview"
import { resolveMonacoLanguage } from "@/lib/monaco-languages"
import { cn } from "@/lib/utils"
import type { QuestionResultSummary } from "@/types/assessment"

interface ResultsCodingItemProps {
  question: QuestionResultSummary
}

export function ResultsCodingItem({ question }: ResultsCodingItemProps) {
  const submissions = question.submissions ?? []
  const bestPoints = question.bestAwardedPoints ?? question.pointsAwarded ?? 0
  const passedCases = question.bestPassedTestcases ?? 0
  const totalCases = question.totalTestcases ?? 0

  return (
    <div className="space-y-5">
      {/* Statement */}
      <div className="space-y-1">
        <h4 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
          Problem Statement
        </h4>
        <div className="text-xs sm:text-sm text-foreground bg-card/40 p-4 rounded-xl border leading-relaxed">
          <MarkdownPreview content={question.codingStatement || "(No statement)"} />
        </div>
      </div>

      {/* Candidate Code Submission */}
      <div className="space-y-2">
        <div className="flex items-center justify-between">
          <h4 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider flex items-center gap-1.5">
            <FileCode className="size-3.5 text-primary" />
            <span>Final Submitted Code ({question.languageId || "code"})</span>
          </h4>

          {totalCases > 0 && (
            <span className="text-xs font-mono text-muted-foreground">
              Passed {passedCases} / {totalCases} test cases
            </span>
          )}
        </div>

        <div className="rounded-xl border border-border overflow-hidden bg-background">
          <MonacoEditor
            language={resolveMonacoLanguage(question.languageId || "java-21")}
            value={question.code || "// No code was submitted for this question"}
            height="260px"
            options={{
              readOnly: true,
              minimap: { enabled: false },
              fontSize: 12,
              lineNumbers: "on",
              scrollBeyondLastLine: false,
              automaticLayout: true,
              padding: { top: 10, bottom: 10 },
            }}
          />
        </div>
      </div>

      {/* Compiler / Execution Output if failed */}
      {question.lastExecutionResult?.logs && (
        <div className="space-y-1">
          <span className="text-xs font-semibold text-rose-500 dark:text-rose-400">
            Compiler / Evaluation Logs:
          </span>
          <pre className="font-mono p-3 bg-rose-500/10 text-rose-500 dark:text-rose-400 border border-rose-500/30 rounded-lg text-xs whitespace-pre-wrap max-h-40 overflow-y-auto">
            {question.lastExecutionResult.logs}
          </pre>
        </div>
      )}

      {/* Submission History Table */}
      {submissions.length > 0 && (
        <div className="space-y-2 pt-2 border-t">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-foreground flex items-center gap-1.5">
              <History className="size-3.5 text-muted-foreground" />
              Submission History ({submissions.length})
            </span>
            <span className="text-[11px] text-muted-foreground">
              Best evaluated run determines awarded score
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
                {submissions.map((sub, sIdx) => {
                  const subAccepted =
                    sub.status === "ACCEPTED" || sub.status === "SUCCESS"
                  const isBest =
                    sub.awardedPoints === bestPoints && bestPoints > 0

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
                        {new Date(sub.submittedAt).toLocaleTimeString([], {
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
}

