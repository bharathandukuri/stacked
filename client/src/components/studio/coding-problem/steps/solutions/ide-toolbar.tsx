import { useState } from "react";
import {
  Play,
  Loader2,
  FileCode,
  Sparkles,
  ShieldCheck,
  ShieldAlert,
  Gauge,
  CheckCircle2,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Switch } from "@/components/ui/switch";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { cn } from "@/lib/utils";
import { useCodingProblemStore } from "../../store";
import { ExecutionLimitsDialog } from "./execution-limits-dialog";

export interface IdeToolbarProps {
  onRunTestcases: () => void;
  className?: string;
}

export function IdeToolbar({ onRunTestcases, className }: IdeToolbarProps) {
  const values = useCodingProblemStore((state) => state.values);
  const activeEditorMode = useCodingProblemStore(
    (state) => state.activeEditorMode
  );
  const starterCodes = useCodingProblemStore((state) => state.starterCodes);
  const referenceSolution = useCodingProblemStore(
    (state) => state.referenceSolution
  );
  const requireVerificationScript = useCodingProblemStore(
    (state) => state.requireVerificationScript
  );
  const executionConstraints = useCodingProblemStore(
    (state) => state.executionConstraints
  );
  const isTesting = useCodingProblemStore((state) => state.isTesting);
  const actions = useCodingProblemStore((state) => state.actions);

  const [limitsDialogOpen, setLimitsDialogOpen] = useState(false);

  const supportedLanguages =
    values.supportedLanguageIds.length > 0
      ? values.supportedLanguageIds
      : values.languages.length > 0
        ? values.languages
        : ["javascript-node-20"];

  // Count starter codes filled
  const starterCodesFilled = supportedLanguages.filter(
    (langId) => Boolean(starterCodes[langId]?.trim())
  ).length;

  const isRefSolutionFilled = Boolean(referenceSolution.trim());

  // Count configured constraints
  const configuredLimitsCount = Object.keys(executionConstraints).length;

  return (
    <>
      <div
        className={cn(
          "flex h-12 w-full shrink-0 items-center justify-between border-b border-border/80 bg-card/60 px-3 backdrop-blur-xs select-none",
          className
        )}
      >
        {/* Left: Mode Switcher Tabs */}
        <div className="flex min-w-0 items-center gap-2">
          <div className="flex items-center rounded-lg border border-border/70 bg-muted/30 p-0.5 text-xs">
            {/* Starter Code Mode */}
            <button
              type="button"
              onClick={() => actions.setActiveEditorMode("starter")}
              className={cn(
                "flex h-7 cursor-pointer items-center gap-1.5 rounded-md px-2.5 font-medium transition-colors select-none",
                activeEditorMode === "starter"
                  ? "bg-background text-foreground shadow-2xs font-semibold"
                  : "text-muted-foreground hover:bg-muted/60 hover:text-foreground"
              )}
            >
              <FileCode className="size-3.5 text-primary" />
              <span>Starter Code</span>
              <span className="font-mono text-[10px] text-muted-foreground">
                ({starterCodesFilled}/{supportedLanguages.length})
              </span>
            </button>

            {/* Reference Solution Mode */}
            <button
              type="button"
              onClick={() => actions.setActiveEditorMode("reference")}
              className={cn(
                "flex h-7 cursor-pointer items-center gap-1.5 rounded-md px-2.5 font-medium transition-colors select-none",
                activeEditorMode === "reference"
                  ? "bg-background text-foreground shadow-2xs font-semibold"
                  : "text-muted-foreground hover:bg-muted/60 hover:text-foreground"
              )}
            >
              <Sparkles className="size-3.5 text-emerald-500" />
              <span>Reference Solution</span>
              {isRefSolutionFilled ? (
                <CheckCircle2 className="size-3 text-emerald-500" />
              ) : (
                <span className="size-1.5 rounded-full bg-amber-500" />
              )}
            </button>

            {/* Verification Script Mode */}
            <button
              type="button"
              onClick={() => actions.setActiveEditorMode("verification")}
              className={cn(
                "flex h-7 cursor-pointer items-center gap-1.5 rounded-md px-2.5 font-medium transition-colors select-none",
                activeEditorMode === "verification"
                  ? "bg-background text-foreground shadow-2xs font-semibold"
                  : "text-muted-foreground hover:bg-muted/60 hover:text-foreground"
              )}
            >
              {requireVerificationScript ? (
                <ShieldCheck className="size-3.5 text-blue-500" />
              ) : (
                <ShieldAlert className="size-3.5 text-muted-foreground/60" />
              )}
              <span>Verification Script</span>
              <Badge
                variant="outline"
                className={cn(
                  "h-4 px-1 text-[9px] font-mono leading-none",
                  requireVerificationScript
                    ? "border-blue-500/40 bg-blue-500/10 text-blue-600 dark:text-blue-400"
                    : "border-border/60 text-muted-foreground/70"
                )}
              >
                {requireVerificationScript ? "ON" : "OFF"}
              </Badge>
            </button>
          </div>

          {/* Toggle for Verification Script */}
          {activeEditorMode === "verification" && (
            <>
              <div className="hidden h-5 w-px bg-border/80 sm:block" />
              <div className="flex items-center gap-1.5 text-xs">
                <Switch
                  id="enable-verifier-switch"
                  checked={requireVerificationScript}
                  onCheckedChange={actions.setRequireVerificationScript}
                />
                <label
                  htmlFor="enable-verifier-switch"
                  className="cursor-pointer text-[11px] font-medium text-foreground select-none"
                >
                  Enable Script
                </label>
              </div>
            </>
          )}
        </div>

        {/* Right: Actions (Execution Limits & Single Run Button) */}
        <div className="flex shrink-0 items-center gap-2 pl-3">
          {/* Execution Limits Configuration Trigger */}
          <Tooltip>
            <TooltipTrigger
              render={
                <Button
                  type="button"
                  variant="outline"
                  size="xs"
                  onClick={() => setLimitsDialogOpen(true)}
                  className="h-7 cursor-pointer gap-1.5 px-2.5 text-xs font-medium text-muted-foreground hover:text-foreground"
                >
                  <Gauge className="size-3.5 text-primary" />
                  <span className="hidden sm:inline">Limits</span>
                  <Badge
                    variant="secondary"
                    className="h-4 px-1 text-[9px] font-mono leading-none"
                  >
                    {configuredLimitsCount}
                  </Badge>
                </Button>
              }
            />
            <TooltipContent>
              Configure CPU time & memory execution limits per language
            </TooltipContent>
          </Tooltip>

          {/* Single Run Button */}
          <Tooltip>
            <TooltipTrigger
              render={
                <Button
                  type="button"
                  size="xs"
                  disabled={isTesting}
                  onClick={onRunTestcases}
                  className="h-7 cursor-pointer gap-1.5 px-3 text-xs font-semibold shadow-xs bg-emerald-600 text-white hover:bg-emerald-500 dark:bg-emerald-600 dark:hover:bg-emerald-500"
                >
                  {isTesting ? (
                    <Loader2 className="size-3.5 animate-spin" />
                  ) : (
                    <Play className="size-3.5 fill-current" />
                  )}
                  <span>{isTesting ? "Executing..." : "Run"}</span>
                </Button>
              }
            />
            <TooltipContent>
              Execute reference solution against sample testcases
            </TooltipContent>
          </Tooltip>
        </div>
      </div>

      {/* Execution Limits Dialog */}
      <ExecutionLimitsDialog
        open={limitsDialogOpen}
        onOpenChange={setLimitsDialogOpen}
      />
    </>
  );
}
