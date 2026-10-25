import { Flag } from "lucide-react"
import { cn } from "@/lib/utils"
import type {
  AssessmentItemResponse,
  SavedQuestionResponse,
} from "@/types/assessment"

interface ExamSidebarProps {
  questions: AssessmentItemResponse[]
  currentIndex: number
  localResponses: Record<string, SavedQuestionResponse>
  isQuestionAnswered: (
    q: AssessmentItemResponse,
    resp: SavedQuestionResponse | undefined
  ) => boolean
  onSelectQuestion: (index: number) => void
}

export function ExamSidebar({
  questions,
  currentIndex,
  localResponses,
  isQuestionAnswered,
  onSelectQuestion,
}: ExamSidebarProps) {
  return (
    <aside className="w-14 border-r border-border bg-card/40 flex flex-col items-center py-3 select-none shrink-0">
      <div className="text-[10px] font-bold text-muted-foreground uppercase tracking-wider mb-2">
        Q#
      </div>

      <div className="flex-1 w-full overflow-y-auto px-2 space-y-2 flex flex-col items-center">
        {questions.map((q, idx) => {
          const resp = localResponses[q.questionId]
          const isAns = isQuestionAnswered(q, resp)
          const isMarked = Boolean(resp?.markedForReview)
          const isCurrent = idx === currentIndex

          return (
            <button
              key={q.questionId}
              type="button"
              onClick={() => onSelectQuestion(idx)}
              title={`Question ${idx + 1}${
                isMarked ? " (Marked)" : isAns ? " (Answered)" : " (Unanswered)"
              }`}
              className={cn(
                "relative size-9 rounded-lg font-mono text-xs font-bold transition-all flex items-center justify-center cursor-pointer",
                isCurrent
                  ? "bg-primary text-primary-foreground shadow-md ring-2 ring-primary/40"
                  : isMarked
                    ? "border border-purple-500/50 bg-purple-500/15 text-purple-600 dark:text-purple-400 hover:bg-purple-500/25"
                    : isAns
                      ? "border border-emerald-500/50 bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 hover:bg-emerald-500/25"
                      : "border border-border/80 bg-background/50 text-muted-foreground hover:bg-muted hover:text-foreground"
              )}
            >
              <span>{idx + 1}</span>

              {isMarked && (
                <span className="absolute -top-1 -right-1 size-3 rounded-full bg-purple-500 text-white flex items-center justify-center">
                  <Flag className="size-2 fill-current" />
                </span>
              )}

              {!isMarked && isAns && (
                <span className="absolute -top-1 -right-1 size-2 rounded-full bg-emerald-500 ring-2 ring-background" />
              )}
            </button>
          )
        })}
      </div>
    </aside>
  )
}

