import { Eye, Loader2 } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { MonacoEditor } from "@/components/monaco-editor"
import { MarkdownPreview } from "@/components/markdown-preview/markdown-preview"
import { resolveMonacoLanguage } from "@/lib/monaco-languages"
import { cn } from "@/lib/utils"
import type {
  CandidateAssessmentResultResponse,
  AttemptStatus,
} from "@/types/assessment"

interface AttemptInspectDialogProps {
  attemptId: string | null
  onClose: () => void
  attemptDetail: CandidateAssessmentResultResponse | undefined
  loadingDetail: boolean
}

export function AttemptInspectDialog({
  attemptId,
  onClose,
  attemptDetail,
  loadingDetail,
}: AttemptInspectDialogProps) {
  const getAttemptBadge = (status?: AttemptStatus) => {
    switch (status) {
      case "IN_PROGRESS":
        return (
          <Badge className="bg-amber-500/15 text-amber-600 dark:text-amber-400 border-amber-500/30 animate-pulse">
            In Progress
          </Badge>
        )
      case "SUBMITTED":
        return (
          <Badge className="bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border-emerald-500/30">
            Submitted
          </Badge>
        )
      case "EXPIRED":
        return <Badge variant="destructive">Expired</Badge>
      case "CANCELLED":
        return <Badge variant="outline">Cancelled</Badge>
      default:
        return <Badge variant="outline">{status ?? "—"}</Badge>
    }
  }

  return (
    <Dialog open={Boolean(attemptId)} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-4xl max-h-[85vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Eye className="size-5 text-primary" />
            <span>Candidate Attempt Inspection</span>
          </DialogTitle>
          <DialogDescription>
            Review full candidate responses, code submissions, and execution test outcomes.
          </DialogDescription>
        </DialogHeader>

        {loadingDetail ? (
          <div className="flex items-center justify-center py-16 gap-2 text-muted-foreground">
            <Loader2 className="size-5 animate-spin text-primary" />
            <span>Loading attempt details...</span>
          </div>
        ) : attemptDetail ? (
          <div className="space-y-6 py-2">
            {/* Attempt Overview Header */}
            <div className="p-4 rounded-xl border bg-muted/20 grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
              <div>
                <span className="text-muted-foreground">Attempt Number</span>
                <p className="font-semibold text-foreground mt-0.5">
                  Attempt #{attemptDetail.attemptNumber}
                </p>
              </div>
              <div>
                <span className="text-muted-foreground">Status</span>
                <p className="mt-0.5">{getAttemptBadge(attemptDetail.status)}</p>
              </div>
              <div>
                <span className="text-muted-foreground">Score Awarded</span>
                <p className="font-bold text-foreground text-sm mt-0.5">
                  {attemptDetail.score} / {attemptDetail.maxPossibleScore} pts (
                  {attemptDetail.percentage.toFixed(1)}%)
                </p>
              </div>
              <div>
                <span className="text-muted-foreground">Completed</span>
                <p className="font-mono text-muted-foreground mt-0.5">
                  {attemptDetail.answeredCount} / {attemptDetail.totalQuestions}{" "}
                  answered
                </p>
              </div>
            </div>

            {/* Question Submissions List */}
            <div className="space-y-4">
              <h3 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
                Questions & Candidate Answers (
                {attemptDetail.questionResults?.length ?? 0})
              </h3>

              {attemptDetail.questionResults?.map((q) => {
                return (
                  <div
                    key={q.questionId}
                    className="rounded-xl border p-4 bg-card/40 space-y-3 text-xs"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-foreground">
                          Q#{q.questionIndex + 1}
                        </span>
                        <span className="font-semibold text-foreground">
                          {q.title}
                        </span>
                        <Badge variant="outline" className="text-[10px]">
                          {q.questionType}
                        </Badge>
                      </div>
                      <span className="font-mono font-semibold">
                        {q.pointsAwarded} / {q.maxPoints} pts
                      </span>
                    </div>

                    {q.questionType === "MCQ" && (
                      <div className="space-y-2 pt-1">
                        {q.mcqStatement && (
                          <div className="prose dark:prose-invert max-w-none text-xs">
                            <MarkdownPreview content={q.mcqStatement} />
                          </div>
                        )}
                        <div className="grid grid-cols-1 gap-1.5 pt-1">
                          {q.options?.map((opt) => {
                            const isSelected = q.selectedOptionIds?.includes(opt.id)
                            return (
                              <div
                                key={opt.id}
                                className={cn(
                                  "p-2.5 rounded-lg border text-xs flex items-center justify-between",
                                  isSelected
                                    ? "border-primary bg-primary/10 text-foreground font-medium"
                                    : "border-border/60 bg-muted/20 text-muted-foreground"
                                )}
                              >
                                <span>{opt.content}</span>
                                {isSelected && (
                                  <Badge
                                    variant="outline"
                                    className="text-[10px]"
                                  >
                                    Candidate Choice
                                  </Badge>
                                )}
                              </div>
                            )
                          })}
                        </div>
                      </div>
                    )}

                    {q.questionType === "CODING" && (
                      <div className="space-y-2 pt-1">
                        {q.codingStatement && (
                          <div className="prose dark:prose-invert max-w-none text-xs text-muted-foreground">
                            <MarkdownPreview content={q.codingStatement} />
                          </div>
                        )}
                        <div className="flex items-center justify-between pt-1">
                          <span className="text-muted-foreground font-medium">
                            Language:{" "}
                            <strong className="text-foreground">
                              {q.languageId || "none"}
                            </strong>
                          </span>
                          {q.allTestcasesPassed !== undefined && (
                            <Badge
                              className={cn(
                                "text-[10px]",
                                q.allTestcasesPassed
                                  ? "bg-emerald-500/15 text-emerald-600 dark:text-emerald-400"
                                  : "bg-destructive/15 text-destructive"
                              )}
                            >
                              {q.allTestcasesPassed
                                ? "ALL TEST CASES PASSED"
                                : "FAILED TEST CASES"}
                            </Badge>
                          )}
                        </div>

                        <div className="h-44 rounded-lg overflow-hidden border">
                          <MonacoEditor
                            language={resolveMonacoLanguage(q.languageId)}
                            value={q.code || "// No code submitted"}
                            height="100%"
                            options={{
                              readOnly: true,
                              minimap: { enabled: false },
                              fontSize: 11,
                              lineNumbers: "on",
                              automaticLayout: true,
                            }}
                          />
                        </div>
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
          </div>
        ) : null}

        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            Close
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

