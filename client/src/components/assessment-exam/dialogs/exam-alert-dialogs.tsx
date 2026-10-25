import { AlertCircle, Clock, ArrowRight } from "lucide-react"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"

interface ExamExpiredDialogProps {
  open: boolean
  onViewResults: () => void
}

export function ExamExpiredDialog({
  open,
  onViewResults,
}: ExamExpiredDialogProps) {
  return (
    <Dialog open={open} onOpenChange={() => {}}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2 text-amber-500">
            <Clock className="size-5" />
            <span>Time Expired</span>
          </DialogTitle>
          <DialogDescription className="pt-2 text-xs text-foreground/90 leading-relaxed">
            Your assessment duration has concluded. All completed responses and code executions have been securely saved and submitted.
          </DialogDescription>
        </DialogHeader>
        <DialogFooter className="pt-4">
          <Button onClick={onViewResults} className="w-full text-xs gap-1.5">
            <span>View Assessment Results</span>
            <ArrowRight className="size-3.5" />
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

interface ExamManualCompletionDialogProps {
  open: boolean
  message: string | null
  onViewResults: () => void
}

export function ExamManualCompletionDialog({
  open,
  message,
  onViewResults,
}: ExamManualCompletionDialogProps) {
  return (
    <Dialog open={open} onOpenChange={() => {}}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2 text-destructive">
            <AlertCircle className="size-5" />
            <span>Assessment Completed by Administrator</span>
          </DialogTitle>
          <DialogDescription className="pt-2 text-xs text-foreground/90 leading-relaxed">
            {message ||
              "The administrator has manually concluded this assessment. All your saved answers have been preserved."}
          </DialogDescription>
        </DialogHeader>
        <DialogFooter className="pt-4">
          <Button onClick={onViewResults} className="w-full text-xs gap-1.5">
            <span>View Assessment Results</span>
            <ArrowRight className="size-3.5" />
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

