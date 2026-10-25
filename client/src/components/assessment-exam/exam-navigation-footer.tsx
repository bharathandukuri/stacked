import {
  ChevronLeft,
  ChevronRight,
  Flag,
  LayoutGrid,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

interface ExamNavigationFooterProps {
  currentIndex: number
  totalQuestions: number
  answeredCount: number
  isMarked: boolean
  onPrevious: () => void
  onNext: () => void
  onToggleMark: () => void
  onOpenOverview: () => void
}

export function ExamNavigationFooter({
  currentIndex,
  totalQuestions,
  answeredCount,
  isMarked,
  onPrevious,
  onNext,
  onToggleMark,
  onOpenOverview,
}: ExamNavigationFooterProps) {
  const isFirst = currentIndex === 0
  const isLast = currentIndex === totalQuestions - 1

  return (
    <footer className="h-14 border-t border-border bg-card/60 backdrop-blur-md px-4 flex items-center justify-between shrink-0 select-none">
      {/* Left: Overview Matrix Button */}
      <div className="flex items-center gap-3">
        <Button
          variant="outline"
          size="sm"
          onClick={onOpenOverview}
          className="h-8 text-xs gap-1.5"
        >
          <LayoutGrid className="size-3.5" />
          <span>Overview ({answeredCount}/{totalQuestions})</span>
        </Button>
      </div>

      {/* Center: Mark for Review */}
      <div>
        <Button
          variant={isMarked ? "default" : "outline"}
          size="sm"
          onClick={onToggleMark}
          className={cn(
            "h-8 text-xs gap-1.5 transition-all",
            isMarked
              ? "bg-purple-600 hover:bg-purple-700 text-white border-purple-600"
              : "text-muted-foreground hover:text-foreground"
          )}
        >
          <Flag className={cn("size-3.5", isMarked && "fill-current")} />
          <span>{isMarked ? "Marked for Review" : "Mark for Review"}</span>
        </Button>
      </div>

      {/* Right: Prev / Next */}
      <div className="flex items-center gap-2">
        <Button
          variant="outline"
          size="sm"
          disabled={isFirst}
          onClick={onPrevious}
          className="h-8 text-xs gap-1"
        >
          <ChevronLeft className="size-4" />
          <span>Previous</span>
        </Button>

        <Button
          size="sm"
          onClick={onNext}
          className="h-8 text-xs gap-1 font-semibold"
        >
          <span>{isLast ? "Next / Review" : "Next"}</span>
          <ChevronRight className="size-4" />
        </Button>
      </div>
    </footer>
  )
}

