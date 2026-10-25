import { RotateCcw } from "lucide-react"
import { Button } from "@/components/ui/button"
import { MarkdownPreview } from "@/components/markdown-preview/markdown-preview"
import { cn } from "@/lib/utils"
import type {
  AssessmentItemResponse,
  SavedQuestionResponse,
} from "@/types/assessment"

interface ExamMcqPanelProps {
  question: AssessmentItemResponse
  savedResponse: SavedQuestionResponse | undefined
  onOptionToggle: (optionId: string) => void
  onClearOptions: () => void
}

export function ExamMcqPanel({
  question,
  savedResponse,
  onOptionToggle,
  onClearOptions,
}: ExamMcqPanelProps) {
  const selectedIds = savedResponse?.selectedOptionIds || []
  const isMultiple = question.mcqType === "MULTIPLE_CHOICE"

  return (
    <div className="h-full flex flex-col p-6 space-y-6 overflow-y-auto">
      <div className="flex items-center justify-between pb-3 border-b border-border/60">
        <div>
          <h3 className="text-sm font-bold text-foreground">Select Answer</h3>
          <p className="text-xs text-muted-foreground mt-0.5">
            {isMultiple
              ? "Multiple options may be correct. Select all that apply."
              : "Single choice question. Choose the best answer."}
          </p>
        </div>

        {selectedIds.length > 0 && (
          <Button
            variant="ghost"
            size="sm"
            onClick={onClearOptions}
            className="h-8 text-xs text-muted-foreground hover:text-foreground gap-1.5"
          >
            <RotateCcw className="size-3" />
            <span>Clear Selection</span>
          </Button>
        )}
      </div>

      <div className="space-y-3">
        {question.options?.map((opt, optIdx) => {
          const isSelected = selectedIds.includes(opt.id)
          const letter = String.fromCharCode(65 + optIdx)

          return (
            <div
              key={opt.id}
              onClick={() => onOptionToggle(opt.id)}
              className={cn(
                "p-4 rounded-xl border transition-all cursor-pointer flex items-start gap-3.5 select-none text-xs sm:text-sm",
                isSelected
                  ? "border-primary bg-primary/10 shadow-xs ring-1 ring-primary/30"
                  : "border-border/80 bg-card hover:bg-muted/40 hover:border-border text-foreground"
              )}
            >
              {/* Option Indicator / Letter */}
              <div
                className={cn(
                  "size-6 rounded-md font-mono text-xs font-bold flex items-center justify-center shrink-0 mt-0.5 transition-colors",
                  isSelected
                    ? "bg-primary text-primary-foreground"
                    : "bg-muted text-muted-foreground"
                )}
              >
                {letter}
              </div>

              {/* Option Content with Markdown Preview */}
              <div className="flex-1 min-w-0 pt-0.5 text-foreground leading-relaxed">
                <MarkdownPreview content={opt.content} />
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
