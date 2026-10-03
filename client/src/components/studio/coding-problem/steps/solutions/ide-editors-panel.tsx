import { useState } from "react"
import {
  Sparkles,
  FileCode,
  ShieldCheck,
  RotateCcw,
  HelpCircle,
  Database,
  CheckCircle2,
} from "lucide-react"
import { MonacoEditor } from "@/components/monaco-editor"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip"
import {
  ResizablePanelGroup,
  ResizablePanel,
  ResizableHandle,
} from "@/components/ui/resizable"
import { cn } from "@/lib/utils"
import { useCodingProblemStore, type DatabaseValidationMode } from "../../store"
import { getLanguageById } from "@/config/languages"

export interface IdeEditorsPanelProps {
  className?: string
}

export function IdeEditorsPanel({ className }: IdeEditorsPanelProps) {
  const values = useCodingProblemStore((state) => state.values)
  const activeCodeLanguage = useCodingProblemStore(
    (state) =>
      state.activeCodeLanguage || values.languages[0] || "javascript-node-20"
  )
  const starterCodes = useCodingProblemStore((state) => state.starterCodes)
  const referenceSolutions = useCodingProblemStore(
    (state) => state.referenceSolutions
  )
  const solutionValidators = useCodingProblemStore(
    (state) => state.solutionValidators
  )
  const databaseSolution = useCodingProblemStore(
    (state) => state.databaseSolution
  )
  const databaseValidationMode = useCodingProblemStore(
    (state) => state.databaseValidationMode
  )
  const databaseCustomValidator = useCodingProblemStore(
    (state) => state.databaseCustomValidator
  )
  const actions = useCodingProblemStore((state) => state.actions)

  // Top pane tab in generic mode: "solution" vs "starter"
  const [topEditorTab, setTopEditorTab] = useState<"solution" | "starter">(
    "solution"
  )

  const isDatabase = values.problemType === "DATABASE"
  const activeLangConfig = getLanguageById(activeCodeLanguage)
  const monacoLang = activeLangConfig?.monacoLanguage || "javascript"
  const extension = activeLangConfig?.defaultExtension || ".txt"

  return (
    <div
      className={cn("h-full w-full overflow-hidden bg-background", className)}
    >
      <ResizablePanelGroup orientation="vertical" className="h-full w-full">
        {/* Top Pane: Reference Solution or Starter Code (Generic) / Reference SQL (Database) */}
        <ResizablePanel
          defaultSize={58}
          minSize={25}
          className="flex flex-col overflow-hidden"
        >
          {!isDatabase ? (
            <div className="flex h-full flex-col overflow-hidden">
              {/* Header Bar */}
              <div className="flex h-9 shrink-0 items-center justify-between border-b border-border/80 bg-muted/20 px-3 select-none">
                {/* Editor Tabs */}
                <div className="flex items-center gap-1">
                  <button
                    type="button"
                    onClick={() => setTopEditorTab("solution")}
                    className={cn(
                      "flex h-7 cursor-pointer items-center gap-1.5 rounded-md px-2.5 text-xs font-medium transition-colors",
                      topEditorTab === "solution"
                        ? "border border-border/80 bg-background text-foreground shadow-2xs"
                        : "text-muted-foreground hover:bg-muted/60 hover:text-foreground"
                    )}
                  >
                    <Sparkles className="size-3.5 text-emerald-500" />
                    <span>Reference Solution</span>
                    <span className="font-mono text-[10px] text-muted-foreground">
                      (Solution{extension})
                    </span>
                  </button>

                  <button
                    type="button"
                    onClick={() => setTopEditorTab("starter")}
                    className={cn(
                      "flex h-7 cursor-pointer items-center gap-1.5 rounded-md px-2.5 text-xs font-medium transition-colors",
                      topEditorTab === "starter"
                        ? "border border-border/80 bg-background text-foreground shadow-2xs"
                        : "text-muted-foreground hover:bg-muted/60 hover:text-foreground"
                    )}
                  >
                    <FileCode className="size-3.5 text-primary" />
                    <span>Starter Code</span>
                    <span className="font-mono text-[10px] text-muted-foreground">
                      (Starter{extension})
                    </span>
                  </button>
                </div>

                {/* Right controls */}
                <div className="flex items-center gap-1.5">
                  {topEditorTab === "solution" && (
                    <Tooltip>
                      <TooltipTrigger
                        render={
                          <Button
                            type="button"
                            variant="ghost"
                            size="xs"
                            onClick={() =>
                              actions.resetReferenceSolutionToStarter(
                                activeCodeLanguage
                              )
                            }
                            className="h-6 cursor-pointer gap-1 px-2 text-[11px] text-muted-foreground hover:text-foreground"
                          >
                            <RotateCcw className="size-3" />
                            <span>Reset to Starter</span>
                          </Button>
                        }
                      />
                      <TooltipContent>
                        Reset solution to match starter code template
                      </TooltipContent>
                    </Tooltip>
                  )}

                  <Badge
                    variant="secondary"
                    className="font-mono text-[10px] text-muted-foreground"
                  >
                    {activeLangConfig?.name || activeCodeLanguage}
                  </Badge>
                </div>
              </div>

              {/* Editor Canvas */}
              <div className="min-h-0 w-full flex-1 overflow-hidden bg-background">
                {topEditorTab === "solution" ? (
                  <MonacoEditor
                    key={`sol-${activeCodeLanguage}`}
                    language={monacoLang}
                    value={referenceSolutions[activeCodeLanguage] || ""}
                    onChange={(val) =>
                      actions.setReferenceSolution(activeCodeLanguage, val)
                    }
                    height="100%"
                    width="100%"
                    bordered={false}
                  />
                ) : (
                  <MonacoEditor
                    key={`starter-${activeCodeLanguage}`}
                    language={monacoLang}
                    value={starterCodes[activeCodeLanguage] || ""}
                    onChange={(val) =>
                      actions.setStarterCode(activeCodeLanguage, val)
                    }
                    height="100%"
                    width="100%"
                    bordered={false}
                  />
                )}
              </div>
            </div>
          ) : (
            /* Database Reference SQL Query */
            <div className="flex h-full flex-col overflow-hidden">
              <div className="flex h-9 shrink-0 items-center justify-between border-b border-border/80 bg-muted/20 px-3 select-none">
                <div className="flex items-center gap-2">
                  <Database className="size-3.5 text-primary" />
                  <span className="text-xs font-semibold text-foreground">
                    Reference SQL Solution
                  </span>
                  <span className="font-mono text-[10px] text-muted-foreground">
                    (solution.sql)
                  </span>
                </div>

                <Badge variant="secondary" className="font-mono text-[10px]">
                  {activeLangConfig?.name || "SQL"}
                </Badge>
              </div>

              <div className="min-h-0 w-full flex-1 overflow-hidden bg-background">
                <MonacoEditor
                  language="sql"
                  value={databaseSolution}
                  onChange={(val) => actions.setDatabaseSolution(val)}
                  height="100%"
                  width="100%"
                  bordered={false}
                />
              </div>
            </div>
          )}
        </ResizablePanel>

        <ResizableHandle withHandle />

        {/* Bottom Pane: Solution Validator (Generic) / Validation Engine (Database) */}
        <ResizablePanel
          defaultSize={42}
          minSize={20}
          className="flex flex-col overflow-hidden"
        >
          {!isDatabase ? (
            <div className="flex h-full flex-col overflow-hidden">
              {/* Header Bar */}
              <div className="flex h-9 shrink-0 items-center justify-between border-b border-border/80 bg-muted/20 px-3 select-none">
                <div className="flex items-center gap-2">
                  <ShieldCheck className="size-3.5 text-primary" />
                  <span className="text-xs font-semibold text-foreground">
                    Solution Validator Script
                  </span>
                  <span className="font-mono text-[10px] text-muted-foreground">
                    (validator{extension})
                  </span>
                </div>

                <div className="flex items-center gap-2">
                  <Badge
                    variant="outline"
                    className="border-primary/30 font-mono text-[9px] text-primary"
                  >
                    Exit 0 = Accepted
                  </Badge>

                  <Tooltip>
                    <TooltipTrigger
                      render={
                        <div className="flex cursor-help items-center gap-1 text-[11px] text-muted-foreground hover:text-foreground">
                          <HelpCircle className="size-3.5" />
                          <span className="font-mono text-[10px]">
                            \n---OUTPUT---\n
                          </span>
                        </div>
                      }
                    />
                    <TooltipContent className="max-w-xs text-xs">
                      Input is provided first, followed by \n---OUTPUT---\n,
                      followed by candidate output.
                    </TooltipContent>
                  </Tooltip>
                </div>
              </div>

              {/* Validator Editor Canvas */}
              <div className="min-h-0 w-full flex-1 overflow-hidden bg-background">
                <MonacoEditor
                  key={`val-${activeCodeLanguage}`}
                  language={monacoLang}
                  value={solutionValidators[activeCodeLanguage] || ""}
                  onChange={(val) =>
                    actions.setSolutionValidator(activeCodeLanguage, val)
                  }
                  height="100%"
                  width="100%"
                  bordered={false}
                />
              </div>
            </div>
          ) : (
            /* Database Validation Mode & Custom Query */
            <div className="flex h-full flex-col overflow-hidden">
              <div className="flex h-9 shrink-0 items-center justify-between border-b border-border/80 bg-muted/20 px-3 select-none">
                <div className="flex items-center gap-2">
                  <ShieldCheck className="size-3.5 text-primary" />
                  <span className="text-xs font-semibold text-foreground">
                    SQL Validation Strategy
                  </span>
                </div>

                {/* Mode Selector Tabs */}
                <div className="flex items-center gap-1">
                  {(
                    [
                      {
                        id: "ORDER_INSENSITIVE",
                        label: "Multiset / Unordered",
                      },
                      { id: "ORDER_SENSITIVE", label: "Strict Order" },
                      { id: "CUSTOM_QUERY", label: "Custom Assertion" },
                    ] as const
                  ).map((m) => (
                    <button
                      key={m.id}
                      type="button"
                      onClick={() =>
                        actions.setDatabaseValidationMode(
                          m.id as DatabaseValidationMode
                        )
                      }
                      className={cn(
                        "h-6.5 cursor-pointer rounded-md px-2 text-[11px] font-medium transition-colors",
                        databaseValidationMode === m.id
                          ? "bg-primary text-primary-foreground shadow-2xs"
                          : "text-muted-foreground hover:bg-muted hover:text-foreground"
                      )}
                    >
                      {m.label}
                    </button>
                  ))}
                </div>
              </div>

              <div className="min-h-0 w-full flex-1 overflow-hidden bg-background">
                {databaseValidationMode === "CUSTOM_QUERY" ? (
                  <MonacoEditor
                    language="sql"
                    value={databaseCustomValidator}
                    onChange={(val) => actions.setDatabaseCustomValidator(val)}
                    height="100%"
                    width="100%"
                    bordered={false}
                    placeholder="-- Custom assertion query. Exit 0 / return rows to validate."
                  />
                ) : (
                  <div className="flex h-full flex-col justify-center p-5 text-xs text-muted-foreground">
                    <div className="flex items-center gap-2 font-medium text-foreground">
                      <CheckCircle2 className="size-4 text-emerald-500" />
                      <span>
                        {databaseValidationMode === "ORDER_INSENSITIVE"
                          ? "Order-Insensitive Multiset Verification"
                          : "Strict Sequence Verification"}
                      </span>
                    </div>
                    <p className="mt-1.5 leading-relaxed text-muted-foreground">
                      {databaseValidationMode === "ORDER_INSENSITIVE"
                        ? "Candidate query results are compared against reference query results as unordered multisets. Rows can appear in any order as long as counts and column values match."
                        : "Candidate query output must match the reference query results in exact row order. Suitable for problems requiring ORDER BY clauses."}
                    </p>
                  </div>
                )}
              </div>
            </div>
          )}
        </ResizablePanel>
      </ResizablePanelGroup>
    </div>
  )
}
