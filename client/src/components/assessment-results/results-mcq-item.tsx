import { CheckCircle2, XCircle, Check } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { MarkdownPreview } from "@/components/markdown-preview/markdown-preview"
import { cn } from "@/lib/utils"
import type { QuestionResultSummary } from "@/types/assessment"

interface ResultsMcqItemProps {
  question: QuestionResultSummary
}

export function ResultsMcqItem({ question }: ResultsMcqItemProps) {
  const selectedIds = question.selectedOptionIds ?? []

  return (
    <div className="space-y-4">
      {/* Statement */}
      <div className="space-y-1">
        <h4 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
          Question Statement
        </h4>
        <div className="text-xs sm:text-sm text-foreground bg-card/40 p-4 rounded-xl border leading-relaxed">
          <MarkdownPreview content={question.mcqStatement || "(No statement)"} />
        </div>
      </div>

      {/* Options List */}
      <div className="space-y-2">
        <h4 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
          Options & Evaluation
        </h4>
        <div className="space-y-2">
          {question.options?.map((opt, optIdx) => {
            const isSelected = selectedIds.includes(opt.id)
            const isCorrectOption = opt.isCorrect === true
            const letter = String.fromCharCode(65 + optIdx)

            return (
              <div
                key={opt.id}
                className={cn(
                  "p-3 rounded-lg border text-xs sm:text-sm flex items-start gap-3 transition-colors",
                  isCorrectOption && isSelected
                    ? "border-emerald-500/60 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
                    : isSelected && !isCorrectOption
                      ? "border-rose-500/60 bg-rose-500/10 text-rose-500 dark:text-rose-400"
                      : isCorrectOption
                        ? "border-emerald-500/40 bg-emerald-500/5 text-foreground"
                        : "border-border/70 bg-card/50 text-muted-foreground"
                )}
              >
                {/* Letter Chip */}
                <div
                  className={cn(
                    "size-6 rounded-md font-mono text-xs font-bold flex items-center justify-center shrink-0 mt-0.5",
                    isCorrectOption
                      ? "bg-emerald-500 text-white"
                      : isSelected
                        ? "bg-rose-500 text-white"
                        : "bg-muted text-muted-foreground"
                  )}
                >
                  {letter}
                </div>

                <div className="flex-1 min-w-0">
                  <MarkdownPreview content={opt.content} />
                </div>

                {/* Status Badges */}
                <div className="flex flex-col items-end gap-1 shrink-0 pt-0.5">
                  {isCorrectOption && (
                    <Badge className="bg-emerald-500/20 text-emerald-600 dark:text-emerald-400 border-emerald-500/40 text-[10px] gap-1 px-1.5 py-0">
                      <Check className="size-3" />
                      <span>Correct Answer</span>
                    </Badge>
                  )}
                  {isSelected && (
                    <Badge
                      variant="outline"
                      className={cn(
                        "text-[10px] gap-1 px-1.5 py-0",
                        isCorrectOption
                          ? "border-emerald-500/40 text-emerald-600 dark:text-emerald-400"
                          : "border-rose-500/40 text-rose-500 dark:text-rose-400"
                      )}
                    >
                      {isCorrectOption ? (
                        <CheckCircle2 className="size-3" />
                      ) : (
                        <XCircle className="size-3" />
                      )}
                      <span>Your Choice</span>
                    </Badge>
                  )}
                </div>
              </div>
            )
          })}
        </div>
      </div>
    </div>
  )
}
