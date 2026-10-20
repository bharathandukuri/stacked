import {
  Code2,
  FileText,
  ChevronRight,
  FlaskConical,
  ClipboardCheck,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

export interface StudioNavbarProps {
  mode: "create" | "edit";
  problemId?: string;
  problemTitle?: string;
  currentStep?: number;
  className?: string;
}

export function StudioNavbar({
  mode,
  problemTitle,
  currentStep = 1,
  className,
}: StudioNavbarProps) {
  const displayTitle =
    mode === "create"
      ? problemTitle?.trim() || "New Coding Problem"
      : problemTitle?.trim() || "Edit Coding Problem";

  return (
    <header
      className={cn(
        "sticky top-0 z-30 flex h-14 w-full items-center justify-between border-b border-border bg-background/95 px-4 backdrop-blur-md sm:px-6",
        className
      )}
    >
      {/* Left: Title & Mode Indicator */}
      <div className="flex min-w-0 items-center gap-3">
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

      {/* Center: Step Progress Stepper (Display Only - No Navigation) */}
      <div className="hidden items-center gap-1 rounded-lg border border-border/60 bg-muted/40 p-1 text-xs md:flex">
        {/* Step 1: Problem Details */}
        <div
          className={cn(
            "flex items-center gap-1.5 rounded-md px-3 py-1 font-medium select-none transition-colors",
            currentStep === 1
              ? "bg-background font-semibold text-foreground shadow-2xs"
              : "text-muted-foreground"
          )}
        >
          <FileText className="size-3.5" />
          <span>1. Problem Details</span>
        </div>

        <ChevronRight className="size-3 text-muted-foreground/50" />

        {/* Step 2: Solutions & Validation */}
        <div
          className={cn(
            "flex items-center gap-1.5 rounded-md px-3 py-1 font-medium select-none transition-colors",
            currentStep === 2
              ? "bg-background font-semibold text-foreground shadow-2xs"
              : "text-muted-foreground"
          )}
        >
          <Code2 className="size-3.5" />
          <span>2. Solutions & Validation</span>
        </div>

        <ChevronRight className="size-3 text-muted-foreground/50" />

        {/* Step 3: Hidden Testcases */}
        <div
          className={cn(
            "flex items-center gap-1.5 rounded-md px-3 py-1 font-medium select-none transition-colors",
            currentStep === 3
              ? "bg-background font-semibold text-foreground shadow-2xs"
              : "text-muted-foreground"
          )}
        >
          <FlaskConical className="size-3.5" />
          <span>3. Hidden Testcases</span>
        </div>

        <ChevronRight className="size-3 text-muted-foreground/50" />

        {/* Step 4: Review & Submit */}
        <div
          className={cn(
            "flex items-center gap-1.5 rounded-md px-3 py-1 font-medium select-none transition-colors",
            currentStep === 4
              ? "bg-background font-semibold text-foreground shadow-2xs"
              : "text-muted-foreground"
          )}
        >
          <ClipboardCheck className="size-3.5" />
          <span>4. Review & Submit</span>
        </div>
      </div>

      {/* Right: Step Indicator for Small Screens */}
      <div className="flex shrink-0 items-center gap-2">
        <Badge
          variant="outline"
          className="font-mono text-xs md:hidden border-border/80 text-muted-foreground"
        >
          Step {currentStep} of 4
        </Badge>
      </div>
    </header>
  );
}

export default StudioNavbar;
