import { Clock, Check, Loader2 } from "lucide-react"
import { Logo } from "@/components/logo"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

interface ExamHeaderProps {
  title?: string
  currentIndex: number
  totalQuestions: number
  saveStatus: "SAVED" | "SAVING" | "UNSAVED"
  remainingSeconds: number | null
  formatTimer: (seconds: number | null) => string
  onSubmitClick: () => void
  isSubmitting?: boolean
}

export function ExamHeader({
  title,
  currentIndex,
  totalQuestions,
  saveStatus,
  remainingSeconds,
  formatTimer,
  onSubmitClick,
  isSubmitting = false,
}: ExamHeaderProps) {
  const isTimeCritical = remainingSeconds !== null && remainingSeconds < 300 // < 5 mins

  return (
    <header className="h-14 border-b border-border bg-card/70 backdrop-blur-md px-4 flex items-center justify-between shrink-0 select-none z-10">
      {/* Brand & Assessment Title */}
      <div className="flex items-center gap-3 min-w-0">
        <Logo size={24} />
        <div className="min-w-0 flex items-center gap-2">
          <span className="font-semibold text-sm truncate max-w-[200px] sm:max-w-xs text-foreground">
            {title || "Assessment"}
          </span>
          <span className="text-xs text-muted-foreground hidden md:inline-block">
            • Q{currentIndex + 1} of {totalQuestions}
          </span>
        </div>
      </div>

      {/* Center Save Status */}
      <div className="hidden sm:flex items-center gap-2 text-xs">
        {saveStatus === "SAVING" ? (
          <span className="inline-flex items-center gap-1.5 text-muted-foreground">
            <Loader2 className="size-3 animate-spin text-primary" />
            <span>Auto-saving...</span>
          </span>
        ) : saveStatus === "SAVED" ? (
          <span className="inline-flex items-center gap-1.5 text-emerald-600 dark:text-emerald-400">
            <Check className="size-3.5" />
            <span>Saved</span>
          </span>
        ) : (
          <span className="inline-flex items-center gap-1.5 text-amber-500 font-medium">
            <span className="size-1.5 rounded-full bg-amber-500 animate-pulse" />
            <span>Unsaved</span>
          </span>
        )}
      </div>

      {/* Right: Countdown & Submit CTA */}
      <div className="flex items-center gap-3">
        {remainingSeconds !== null && (
          <div
            className={cn(
              "flex items-center gap-1.5 px-2.5 py-1 rounded-md font-mono text-xs font-semibold border transition-colors",
              isTimeCritical
                ? "bg-rose-500/10 text-rose-500 border-rose-500/30 animate-pulse"
                : "bg-muted/50 text-foreground border-border"
            )}
          >
            <Clock className="size-3.5" />
            <span>{formatTimer(remainingSeconds)}</span>
          </div>
        )}

        <Button
          size="sm"
          onClick={onSubmitClick}
          disabled={isSubmitting}
          className="h-8 text-xs font-semibold px-3"
        >
          <span>Finish Assessment</span>
        </Button>
      </div>
    </header>
  )
}

