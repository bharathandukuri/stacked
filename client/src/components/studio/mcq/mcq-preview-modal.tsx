import { useState } from "react"
import {
  CheckCircle2,
  XCircle,
  Eye,
  Sparkles,
  RotateCcw,
  BookOpen,
} from "lucide-react"
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { MarkdownPreview } from "@/components/markdown-preview/markdown-preview"
import type { McqQuestionDetail, McqOption } from "@/types/mcq"
import { cn } from "@/lib/utils"

interface McqPreviewModalProps {
  question: McqQuestionDetail | null
  open: boolean
  onOpenChange: (open: boolean) => void
}

const OPTION_LETTERS = ["A", "B", "C", "D", "E", "F", "G", "H"]

export function McqPreviewModal({
  question,
  open,
  onOpenChange,
}: McqPreviewModalProps) {
  const [selectedOptionIds, setSelectedOptionIds] = useState<string[]>([])
  const [submitted, setSubmitted] = useState(false)
  const [examinerMode, setExaminerMode] = useState(false)

  if (!question) return null

  const isMultiple = question.type === "MULTIPLE_CHOICE"

  const handleOptionToggle = (optionId: string) => {
    if (submitted) return
    if (isMultiple) {
      setSelectedOptionIds((prev) =>
        prev.includes(optionId)
          ? prev.filter((id) => id !== optionId)
          : [...prev, optionId]
      )
    } else {
      setSelectedOptionIds([optionId])
    }
  }

  const handleReset = () => {
    setSelectedOptionIds([])
    setSubmitted(false)
  }

  // Calculate score if submitted
  const isCorrect =
    question.correctOptionIds.length === selectedOptionIds.length &&
    question.correctOptionIds.every((id) => selectedOptionIds.includes(id))

  const earnedPoints = isCorrect
    ? question.defaultPoints
    : selectedOptionIds.length > 0 && question.negativePoints > 0
    ? -question.negativePoints
    : 0

  const showAnswers = examinerMode || submitted

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] sm:max-w-3xl overflow-y-auto">
        <DialogHeader>
          <div className="flex flex-wrap items-center justify-between gap-2 border-b border-border/60 pb-3">
            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold tracking-wider text-muted-foreground uppercase">
                {question.type.replace("_", " ")}
              </span>
              <Badge
                variant={
                  question.difficulty === "EASY"
                    ? "default"
                    : question.difficulty === "MEDIUM"
                    ? "secondary"
                    : "destructive"
                }
                className="text-[11px] font-medium"
              >
                {question.difficulty}
              </Badge>
              <Badge variant="outline" className="text-[11px]">
                +{question.defaultPoints} pts
                {question.negativePoints > 0 && ` / -${question.negativePoints}`}
              </Badge>
            </div>

            <div className="flex items-center gap-2">
              <Button
                variant={examinerMode ? "default" : "outline"}
                size="sm"
                onClick={() => setExaminerMode(!examinerMode)}
                className="gap-1.5 text-xs h-7"
              >
                <Eye className="h-3.5 w-3.5" />
                {examinerMode ? "Examiner View ON" : "Examiner View"}
              </Button>
            </div>
          </div>
          <DialogTitle className="mt-2 text-lg font-semibold text-foreground">
            {question.title}
          </DialogTitle>
          {question.topics && question.topics.length > 0 && (
            <div className="flex flex-wrap gap-1.5 pt-1">
              {question.topics.map((t) => (
                <Badge
                  key={t}
                  variant="outline"
                  className="bg-accent/40 text-[10px] text-muted-foreground"
                >
                  {t}
                </Badge>
              ))}
            </div>
          )}
        </DialogHeader>

        {/* Question Prompt Markdown */}
        <div className="rounded-lg border border-border/70 bg-card p-4">
          <MarkdownPreview
            content={question.content}
            className="prose prose-sm dark:prose-invert max-w-none text-foreground"
          />
        </div>

        {/* Options List */}
        <div className="space-y-2.5">
          <div className="flex items-center justify-between text-xs text-muted-foreground">
            <span>
              {isMultiple
                ? "Select all correct choices (Multiple Choice):"
                : "Select one choice:"}
            </span>
            {question.shuffleOptions && (
              <span className="italic text-[11px]">Shuffle enabled</span>
            )}
          </div>

          <div className="grid gap-2">
            {question.options.map((option: McqOption, index: number) => {
              const isSelected = selectedOptionIds.includes(option.id)
              const isCorrectOption = question.correctOptionIds.includes(option.id)

              let optionStyle =
                "border-border/70 bg-card/60 hover:bg-accent/40 hover:border-primary/40 text-foreground"

              if (showAnswers) {
                if (isCorrectOption) {
                  optionStyle =
                    "border-emerald-500/80 bg-emerald-500/10 dark:bg-emerald-950/20 text-emerald-900 dark:text-emerald-200 ring-1 ring-emerald-500/50"
                } else if (isSelected && !isCorrectOption) {
                  optionStyle =
                    "border-destructive/80 bg-destructive/10 text-destructive ring-1 ring-destructive/40"
                } else {
                  optionStyle = "opacity-60 border-border/40 bg-card/30"
                }
              } else if (isSelected) {
                optionStyle =
                  "border-primary bg-primary/10 text-primary ring-1 ring-primary"
              }

              return (
                <div
                  key={option.id}
                  onClick={() => handleOptionToggle(option.id)}
                  className={cn(
                    "flex flex-col rounded-lg border p-3.5 transition-all cursor-pointer",
                    optionStyle
                  )}
                >
                  <div className="flex items-start gap-3">
                    <span
                      className={cn(
                        "flex h-6 w-6 shrink-0 items-center justify-center rounded-md text-xs font-semibold",
                        isCorrectOption && showAnswers
                          ? "bg-emerald-600 text-white"
                          : isSelected
                          ? "bg-primary text-primary-foreground"
                          : "border border-border/80 bg-muted/60 text-muted-foreground"
                      )}
                    >
                      {OPTION_LETTERS[index] || index + 1}
                    </span>

                    <div className="flex-1 text-sm leading-relaxed">
                      <MarkdownPreview content={option.text} />
                    </div>

                    {showAnswers && isCorrectOption && (
                      <CheckCircle2 className="h-5 w-5 shrink-0 text-emerald-600 dark:text-emerald-400" />
                    )}
                    {showAnswers && isSelected && !isCorrectOption && (
                      <XCircle className="h-5 w-5 shrink-0 text-destructive" />
                    )}
                  </div>

                  {showAnswers && option.explanation && (
                    <div className="mt-2.5 rounded bg-muted/50 p-2.5 text-xs text-muted-foreground border-l-2 border-primary/50">
                      <span className="font-semibold text-foreground/90 block mb-1">
                        Explanation:
                      </span>
                      <MarkdownPreview content={option.explanation} />
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        </div>

        {/* Candidate Interactive Actions & Results */}
        {!examinerMode && (
          <div className="flex items-center justify-between border-t border-border/60 pt-3">
            {!submitted ? (
              <Button
                size="sm"
                onClick={() => setSubmitted(true)}
                disabled={selectedOptionIds.length === 0}
                className="gap-2"
              >
                <Sparkles className="h-4 w-4" />
                Submit Simulated Answer
              </Button>
            ) : (
              <div className="flex items-center gap-3">
                <div
                  className={cn(
                    "flex items-center gap-2 rounded-md px-3 py-1.5 text-xs font-semibold",
                    isCorrect
                      ? "bg-emerald-500/15 text-emerald-600 dark:text-emerald-400"
                      : "bg-destructive/15 text-destructive"
                  )}
                >
                  {isCorrect ? (
                    <>
                      <CheckCircle2 className="h-4 w-4" />
                      Correct! (+{earnedPoints} pts)
                    </>
                  ) : (
                    <>
                      <XCircle className="h-4 w-4" />
                      Incorrect ({earnedPoints} pts)
                    </>
                  )}
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleReset}
                  className="gap-1.5 text-xs h-7"
                >
                  <RotateCcw className="h-3.5 w-3.5" />
                  Try Again
                </Button>
              </div>
            )}
            <span className="text-xs text-muted-foreground">
              {selectedOptionIds.length} option(s) selected
            </span>
          </div>
        )}

        {/* Overall Explanation / Rationale */}
        {showAnswers && question.explanation && (
          <div className="rounded-lg border border-primary/20 bg-primary/5 p-3.5 text-sm">
            <div className="flex items-center gap-1.5 font-semibold text-primary mb-1 text-xs uppercase tracking-wider">
              <BookOpen className="h-4 w-4" />
              Comprehensive Solution Rationale
            </div>
            <MarkdownPreview
              content={question.explanation}
              className="prose prose-sm dark:prose-invert max-w-none text-muted-foreground"
            />
          </div>
        )}
      </DialogContent>
    </Dialog>
  )
}
export default McqPreviewModal

