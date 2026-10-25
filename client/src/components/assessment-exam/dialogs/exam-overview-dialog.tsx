import { Flag } from "lucide-react"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { cn } from "@/lib/utils"
import type {
  AssessmentItemResponse,
  SavedQuestionResponse,
} from "@/types/assessment"

interface ExamOverviewDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  questions: AssessmentItemResponse[]
  currentIndex: number
  localResponses: Record<string, SavedQuestionResponse>
  isQuestionAnswered: (
    q: AssessmentItemResponse,
    resp: SavedQuestionResponse | undefined
  ) => boolean
  answeredCount: number
  unansweredCount: number
  markedCount: number
  onSelectQuestion: (index: number) => void
}

export function ExamOverviewDialog({
  open,
  onOpenChange,
  questions,
  currentIndex,
  localResponses,
  isQuestionAnswered,
  answeredCount,
  unansweredCount,
  markedCount,
  onSelectQuestion,
}: ExamOverviewDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-xl">
        <DialogHeader>
          <DialogTitle className="text-base font-bold">
            Question Overview Matrix
          </DialogTitle>
          <DialogDescription className="text-xs">
            Review status across all test items. Click any question to navigate directly.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-2">
          {/* Summary Badges Strip */}
          <div className="grid grid-cols-3 gap-2 text-center text-xs">
            <div className="p-2.5 rounded-lg border bg-emerald-500/10 border-emerald-500/20">
              <div className="text-lg font-bold text-emerald-600 dark:text-emerald-400">
                {answeredCount}
              </div>
              <div className="text-[11px] text-muted-foreground">Answered</div>
            </div>
            <div className="p-2.5 rounded-lg border bg-amber-500/10 border-amber-500/20">
              <div className="text-lg font-bold text-amber-500">
                {unansweredCount}
              </div>
              <div className="text-[11px] text-muted-foreground">Unanswered</div>
            </div>
            <div className="p-2.5 rounded-lg border bg-purple-500/10 border-purple-500/20">
              <div className="text-lg font-bold text-purple-600 dark:text-purple-400">
                {markedCount}
              </div>
              <div className="text-[11px] text-muted-foreground">Marked for Review</div>
            </div>
          </div>

          {/* Questions Grid Matrix */}
          <div className="grid grid-cols-6 sm:grid-cols-8 gap-2 max-h-60 overflow-y-auto p-1">
            {questions.map((q, idx) => {
              const resp = localResponses[q.questionId]
              const isAns = isQuestionAnswered(q, resp)
              const isMarked = Boolean(resp?.markedForReview)
              const isCurrent = idx === currentIndex

              return (
                <button
                  key={q.questionId}
                  type="button"
                  onClick={() => {
                    onSelectQuestion(idx)
                    onOpenChange(false)
                  }}
                  className={cn(
                    "flex flex-col items-center justify-center p-2 rounded-lg border text-xs font-bold transition-all relative cursor-pointer",
                    isCurrent ? "ring-2 ring-primary" : "",
                    isMarked
                      ? "border-purple-500 bg-purple-500/15 text-purple-600 dark:text-purple-400"
                      : isAns
                        ? "border-emerald-500 bg-emerald-500/15 text-emerald-600 dark:text-emerald-400"
                        : "border-border bg-muted/30 text-muted-foreground hover:bg-muted/60"
                  )}
                >
                  <span>{idx + 1}</span>
                  {isMarked && (
                    <Flag className="size-2.5 fill-current absolute top-1 right-1" />
                  )}
                  <span className="text-[9px] font-normal font-mono opacity-80">
                    {q.points}p
                  </span>
                </button>
              )
            })}
          </div>

          {/* Legend */}
          <div className="flex flex-wrap items-center justify-center gap-4 text-[11px] text-muted-foreground pt-2 border-t">
            <span className="flex items-center gap-1.5">
              <span className="size-2 rounded-full bg-emerald-500" /> Answered
            </span>
            <span className="flex items-center gap-1.5">
              <span className="size-2 rounded-full bg-purple-500" /> Marked for Review
            </span>
            <span className="flex items-center gap-1.5">
              <span className="size-2 rounded-full bg-muted-foreground/40" /> Unanswered
            </span>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}

