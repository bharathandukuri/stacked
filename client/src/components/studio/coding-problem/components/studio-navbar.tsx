import {
  ArrowLeft,
  Check,
  Loader2,
  Code2,
  FileText,
  ChevronRight,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip"
import { cn } from "@/lib/utils"

export interface StudioNavbarProps {
  mode: "create" | "edit"
  problemId?: string
  problemTitle?: string
  currentStep?: number
  onStepChange?: (step: number) => void
  onBack?: () => void
  onSubmit?: () => void
  isSubmitting?: boolean
  submitLabel?: string
  className?: string
}

export function StudioNavbar({
  mode,
  problemId,
  problemTitle,
  currentStep = 1,
  onStepChange,
  onBack,
  onSubmit,
  isSubmitting = false,
  submitLabel,
  className,
}: StudioNavbarProps) {
  const displayTitle =
    mode === "create"
      ? problemTitle?.trim() || "New Coding Problem"
      : problemTitle?.trim() || `Problem #${problemId}`

  return (
    <header
      className={cn(
        "sticky top-0 z-30 flex h-14 w-full items-center justify-between border-b border-border bg-background/95 px-4 backdrop-blur-md sm:px-6",
        className
      )}
    >
      {/* Left: Back & Title */}
      <div className="flex min-w-0 items-center gap-3">
        {onBack && (
          <Tooltip>
            <TooltipTrigger
              render={
                <Button
                  type="button"
                  variant="ghost"
                  size="icon-xs"
                  onClick={onBack}
                  className="shrink-0 cursor-pointer text-muted-foreground hover:text-foreground"
                >
                  <ArrowLeft className="size-4" />
                </Button>
              }
            />
            <TooltipContent>Go Back</TooltipContent>
          </Tooltip>
        )}

        <div className="flex min-w-0 items-center gap-2">
          <span className="flex size-2 shrink-0 animate-pulse rounded-full bg-primary" />
          <h1 className="truncate text-sm font-semibold tracking-tight text-foreground">
            {displayTitle}
          </h1>
          <Badge
            variant="secondary"
            className={cn(
              "shrink-0 px-1.5 py-0 font-mono text-[10px] uppercase",
              mode === "create"
                ? "border-primary/20 bg-primary/10 text-primary"
                : "border-amber-500/20 bg-amber-500/10 text-amber-500"
            )}
          >
            {mode === "create" ? "New" : "Editing"}
          </Badge>
        </div>
      </div>

      {/* Center: Step Progress Stepper */}
      <div className="hidden items-center gap-1 rounded-lg border border-border/60 bg-muted/40 p-1 text-xs md:flex">
        <button
          type="button"
          onClick={() => onStepChange?.(1)}
          className={cn(
            "flex cursor-pointer items-center gap-1.5 rounded-md px-3 py-1 font-medium transition-colors select-none",
            currentStep === 1
              ? "bg-background font-semibold text-foreground shadow-2xs"
              : "text-muted-foreground hover:text-foreground"
          )}
        >
          <FileText className="size-3.5" />
          <span>1. Problem Details</span>
        </button>

        <ChevronRight className="size-3 text-muted-foreground/50" />

        <button
          type="button"
          onClick={() => onStepChange?.(2)}
          className={cn(
            "flex cursor-pointer items-center gap-1.5 rounded-md px-3 py-1 font-medium transition-colors select-none",
            currentStep === 2
              ? "bg-background font-semibold text-foreground shadow-2xs"
              : "text-muted-foreground hover:text-foreground"
          )}
        >
          <Code2 className="size-3.5" />
          <span>2. Solutions & Validation</span>
        </button>
      </div>

      {/* Right: Actions */}
      <div className="flex shrink-0 items-center gap-2">
        {currentStep === 2 && onStepChange && (
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => onStepChange(1)}
            className="cursor-pointer gap-1.5 px-3 text-xs text-muted-foreground hover:text-foreground"
          >
            <ArrowLeft className="size-3.5" />
            <span className="hidden sm:inline">Back to Details</span>
          </Button>
        )}

        {onSubmit && (
          <Button
            type="button"
            size="sm"
            disabled={isSubmitting}
            onClick={onSubmit}
            className="cursor-pointer gap-1.5 px-4 text-xs font-semibold shadow-xs"
          >
            {isSubmitting ? (
              <Loader2 className="size-3.5 animate-spin" />
            ) : (
              <Check className="size-3.5" />
            )}
            <span>
              {isSubmitting
                ? "Saving..."
                : submitLabel ||
                  (currentStep === 1
                    ? "Next: Code & Solutions →"
                    : "Save Problem")}
            </span>
          </Button>
        )}
      </div>
    </header>
  )
}

export default StudioNavbar
