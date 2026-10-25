import { AlertCircle, CheckCircle2, Loader2 } from "lucide-react"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

interface ExamSubmitDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  totalQuestions: number
  answeredCount: number
  unansweredCount: number
  markedCount: number
  isSubmitting: boolean
  onConfirmSubmit: () => void
}

export function ExamSubmitDialog({
  open,
  onOpenChange,
  totalQuestions,
  answeredCount,
  unansweredCount,
  markedCount,
  isSubmitting,
  onConfirmSubmit,
}: ExamSubmitDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <CheckCircle2 className="size-5 text-primary" />
            <span>Submit Assessment</span>
          </DialogTitle>
          <DialogDescription className="text-xs pt-1">
            Are you sure you want to finalize and submit your assessment attempt?
            You will not be able to modify your answers once submitted.
          </DialogDescription>
        </DialogHeader>

        <div className="py-2 space-y-3">
          {/* Summary Breakdown Strip */}
          <div className="rounded-xl border border-border bg-card/50 p-3 space-y-2 text-xs">
            <div className="flex items-center justify-between">
              <span className="text-muted-foreground">Questions Answered:</span>
              <strong className="font-mono text-foreground font-semibold">
                {answeredCount} / {totalQuestions}
              </strong>
            </div>

            <div className="flex items-center justify-between">
              <span className="text-muted-foreground">Unanswered Questions:</span>
              <strong
                className={cn(
                  "font-mono font-semibold",
                  unansweredCount > 0 ? "text-amber-500" : "text-foreground"
                )}
              >
                {unansweredCount}
              </strong>
            </div>

            {markedCount > 0 && (
              <div className="flex items-center justify-between">
                <span className="text-muted-foreground">Marked for Review:</span>
                <strong className="font-mono text-purple-600 dark:text-purple-400 font-semibold">
                  {markedCount}
                </strong>
              </div>
            )}
          </div>

          {/* Unanswered Warning Banner */}
          {unansweredCount > 0 && (
            <div className="p-3 rounded-lg border border-amber-500/30 bg-amber-500/10 text-xs text-amber-600 dark:text-amber-400 flex items-start gap-2.5">
              <AlertCircle className="size-4 shrink-0 mt-0.5" />
              <p className="leading-relaxed">
                You have {unansweredCount} unanswered question(s). You can still return to complete them before your timer expires.
              </p>
            </div>
          )}
        </div>

        <DialogFooter className="gap-2 sm:gap-0 pt-2">
          <Button
            variant="outline"
            size="sm"
            disabled={isSubmitting}
            onClick={() => onOpenChange(false)}
            className="text-xs"
          >
            Return to Exam
          </Button>
          <Button
            size="sm"
            disabled={isSubmitting}
            onClick={onConfirmSubmit}
            className="text-xs gap-1.5 font-semibold"
          >
            {isSubmitting && <Loader2 className="size-3.5 animate-spin" />}
            <span>Confirm & Submit</span>
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

