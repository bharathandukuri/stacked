import { ArrowLeft, Check, Loader2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { cn } from "@/lib/utils"

export type StudioTab = "form" | "review"

interface StudioNavbarProps {
  mode: "create" | "edit"
  problemId?: string
  problemTitle?: string
  activeTab: StudioTab
  onBack?: () => void
  onSubmit?: () => void
  isSubmitting?: boolean
}

export function StudioNavbar({
  mode,
  problemId,
  problemTitle,
  activeTab,
  onBack,
  onSubmit,
  isSubmitting = false,
}: StudioNavbarProps) {
  const displayTitle =
    mode === "create"
      ? problemTitle?.trim() || "New Coding Problem"
      : problemTitle?.trim() || `Problem #${problemId}`

  return (
    <header className="sticky top-0 z-30 flex h-14 w-full items-center justify-between border-b border-border bg-background/95 px-4 backdrop-blur-md sm:px-6">
      {/* Left: Back & Title */}
      <div className="flex min-w-0 items-center gap-3">
        {onBack && (
          <Button
            type="button"
            variant="ghost"
            size="icon-xs"
            onClick={onBack}
            className="shrink-0 cursor-pointer text-muted-foreground hover:text-foreground"
            title="Go Back"
          >
            <ArrowLeft className="size-4" />
          </Button>
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

      {/* Center: Non-clickable step / progress status */}
      <div className="flex items-center">
        {activeTab === "form" ? (
          <div className="flex items-center gap-2 rounded-full border border-border/80 bg-muted/40 px-3.5 py-1 text-xs select-none">
            <span className="flex size-2 rounded-full bg-primary" />
            <span className="font-semibold text-foreground">Step 1</span>
            <span className="text-muted-foreground">/</span>
            <span className="text-muted-foreground">Problem Configuration</span>
          </div>
        ) : (
          <div className="flex items-center gap-2 rounded-full border border-emerald-500/30 bg-emerald-500/10 px-3.5 py-1 text-xs select-none">
            <span className="flex size-2 rounded-full bg-emerald-500" />
            <span className="font-semibold text-emerald-600 dark:text-emerald-400">
              Step 2
            </span>
            <span className="text-muted-foreground">/</span>
            <span className="text-foreground">Review & Verification</span>
          </div>
        )}
      </div>

      {/* Right: Actions (Reset, Draft, Review removed completely) */}
      <div className="flex shrink-0 items-center gap-2">
        {activeTab === "review" && onSubmit && (
          <Button
            type="button"
            size="xs"
            disabled={isSubmitting}
            onClick={onSubmit}
            className="cursor-pointer gap-1.5 bg-primary text-xs font-semibold text-primary-foreground shadow-xs"
          >
            {isSubmitting ? (
              <Loader2 className="size-3 animate-spin" />
            ) : (
              <Check className="size-3.5" />
            )}
            <span>{mode === "create" ? "Create Problem" : "Save Changes"}</span>
          </Button>
        )}
      </div>
    </header>
  )
}
