import {
  Play,
  Loader2,
  CheckCircle2,
  AlertCircle,
  RotateCcw,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip"
import { cn } from "@/lib/utils"
import { useCodingProblemStore } from "../../store"
import { getLanguageById } from "@/config/languages"

export interface IdeToolbarProps {
  onRunTestcases: () => void
  onResetReferenceSolution?: () => void
  className?: string
}

export function IdeToolbar({
  onRunTestcases,
  onResetReferenceSolution,
  className,
}: IdeToolbarProps) {
  const values = useCodingProblemStore((state) => state.values)
  const activeCodeLanguage = useCodingProblemStore(
    (state) =>
      state.activeCodeLanguage || values.languages[0] || "javascript-node-20"
  )
  const isTesting = useCodingProblemStore((state) => state.isTesting)
  const referenceSolutions = useCodingProblemStore(
    (state) => state.referenceSolutions
  )
  const solutionValidators = useCodingProblemStore(
    (state) => state.solutionValidators
  )
  const databaseSolution = useCodingProblemStore(
    (state) => state.databaseSolution
  )
  const actions = useCodingProblemStore((state) => state.actions)

  const isDatabase = values.problemType === "DATABASE"
  const selectedLanguages =
    values.languages.length > 0 ? values.languages : ["javascript-node-20"]

  const isLanguageReady = (langId: string) => {
    if (isDatabase) {
      return Boolean(databaseSolution.trim())
    }
    const sol = referenceSolutions[langId]?.trim()
    const val = solutionValidators[langId]?.trim()
    return Boolean(sol && val)
  }

  const completedCount = selectedLanguages.filter(isLanguageReady).length
  const totalCount = selectedLanguages.length
  const allReady = completedCount === totalCount

  return (
    <div
      className={cn(
        "flex h-11 w-full shrink-0 items-center justify-between border-b border-border/80 bg-card/60 px-3 backdrop-blur-xs select-none",
        className
      )}
    >
      {/* Left: Language Tabs */}
      <div className="no-scrollbar flex min-w-0 items-center gap-1.5 overflow-x-auto py-1">
        {selectedLanguages.map((langId) => {
          const lang = getLanguageById(langId)
          const isActive = activeCodeLanguage === langId
          const ready = isLanguageReady(langId)

          return (
            <button
              key={langId}
              type="button"
              onClick={() => actions.setActiveCodeLanguage(langId)}
              className={cn(
                "group flex h-7.5 shrink-0 cursor-pointer items-center gap-2 rounded-md px-2.5 text-xs font-medium transition-all",
                isActive
                  ? "bg-primary text-primary-foreground shadow-2xs"
                  : "bg-muted/50 text-muted-foreground hover:bg-muted hover:text-foreground"
              )}
            >
              <span className="truncate">{lang?.name || langId}</span>
              {ready ? (
                <CheckCircle2
                  className={cn(
                    "size-3.5 shrink-0",
                    isActive ? "text-primary-foreground" : "text-emerald-500"
                  )}
                />
              ) : (
                <span
                  className={cn(
                    "size-1.5 shrink-0 rounded-full",
                    isActive ? "bg-primary-foreground/70" : "bg-amber-500"
                  )}
                />
              )}
            </button>
          )
        })}
      </div>

      {/* Right: Actions & Status */}
      <div className="flex shrink-0 items-center gap-2 pl-3">
        {/* Readiness Pill */}
        <Badge
          variant="outline"
          className={cn(
            "hidden h-6 gap-1 px-2 font-mono text-[10px] sm:inline-flex",
            allReady
              ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
              : "border-amber-500/30 bg-amber-500/10 text-amber-600 dark:text-amber-400"
          )}
        >
          {allReady ? (
            <>
              <CheckCircle2 className="size-3" />
              <span>
                All Ready ({completedCount}/{totalCount})
              </span>
            </>
          ) : (
            <>
              <AlertCircle className="size-3" />
              <span>
                {completedCount}/{totalCount} Completed
              </span>
            </>
          )}
        </Badge>

        {/* Optional Reset Shortcut */}
        {!isDatabase && onResetReferenceSolution && (
          <Tooltip>
            <TooltipTrigger
              render={
                <Button
                  type="button"
                  variant="ghost"
                  size="xs"
                  onClick={onResetReferenceSolution}
                  className="h-7 cursor-pointer text-muted-foreground hover:text-foreground"
                >
                  <RotateCcw className="size-3.5" />
                  <span className="hidden text-[11px] lg:inline">
                    Reset Solution
                  </span>
                </Button>
              }
            />
            <TooltipContent>Reset solution code to starter stub</TooltipContent>
          </Tooltip>
        )}

        {/* Run Reference Solution Button */}
        <Button
          type="button"
          size="xs"
          disabled={isTesting}
          onClick={onRunTestcases}
          className="h-7.5 cursor-pointer gap-1.5 px-3 text-xs font-semibold shadow-xs"
        >
          {isTesting ? (
            <>
              <Loader2 className="size-3.5 animate-spin" />
              <span>Running...</span>
            </>
          ) : (
            <>
              <Play className="size-3.5 fill-current" />
              <span>Run Solution</span>
            </>
          )}
        </Button>
      </div>
    </div>
  )
}
