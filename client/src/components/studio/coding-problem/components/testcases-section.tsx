import { useState } from "react"
import { Plus, Trash2, Copy, ChevronUp, ChevronDown } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { MonacoEditor } from "@/components/monaco-editor"
import { cn } from "@/lib/utils"
import type { ProblemType, TestCase } from "../types"
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip"

export interface TestcasesSectionProps {
  problemType: ProblemType
  testCases: TestCase[]
  onChange: (cases: TestCase[]) => void
  readOnly?: boolean
  className?: string
}

export function TestcasesSection({
  problemType,
  testCases,
  onChange,
  readOnly = false,
  className,
}: TestcasesSectionProps) {
  const [activeTab, setActiveTab] = useState(0)

  const activeIndex = Math.min(activeTab, Math.max(0, testCases.length - 1))
  const activeCase = testCases[activeIndex] || testCases[0]

  const handleAddTestCase = () => {
    const nextNumber = testCases.length + 1
    const newCase: TestCase = {
      id: `case-${Date.now()}`,
      title: `Case ${nextNumber}`,
      input: "",
    }

    const updated = [...testCases, newCase]
    onChange(updated)
    setActiveTab(updated.length - 1)
  }

  const handleDuplicateTestCase = (index: number) => {
    const target = testCases[index]
    if (!target) return

    const duplicated: TestCase = {
      ...target,
      id: `case-${Date.now()}`,
    }

    const updated = [...testCases]
    updated.splice(index + 1, 0, duplicated)
    const reindexed = updated.map((tc, idx) => ({
      ...tc,
      title: `Case ${idx + 1}`,
    }))
    onChange(reindexed)
    setActiveTab(index + 1)
  }

  const handleRemoveTestCase = (index: number) => {
    if (testCases.length <= 1) return
    const filtered = testCases.filter((_, i) => i !== index)
    const reindexed = filtered.map((tc, idx) => ({
      ...tc,
      title: `Case ${idx + 1}`,
    }))
    onChange(reindexed)
    setActiveTab(Math.max(0, Math.min(activeTab, reindexed.length - 1)))
  }

  const handleMoveTestCase = (fromIndex: number, direction: -1 | 1) => {
    const toIndex = fromIndex + direction
    if (toIndex < 0 || toIndex >= testCases.length) return

    const updated = [...testCases]
    const item = updated[fromIndex]
    updated[fromIndex] = updated[toIndex]
    updated[toIndex] = item

    const reindexed = updated.map((tc, idx) => ({
      ...tc,
      title: `Case ${idx + 1}`,
    }))

    onChange(reindexed)
    setActiveTab(toIndex)
  }

  const handleUpdateInput = (val: string) => {
    if (!activeCase) return
    const updated = [...testCases]
    updated[activeIndex] = {
      ...activeCase,
      input: val,
    }
    onChange(updated)
  }

  return (
    <div
      className={cn(
        "space-y-3 rounded-xl border border-border/80 bg-card p-4 shadow-2xs",
        className
      )}
    >
      {/* Tab Strip */}
      <div className="flex flex-wrap items-center gap-1.5 border-b border-border/60 pb-2.5">
        {testCases.map((tc, idx) => {
          const isActive = idx === activeIndex
          return (
            <button
              key={tc.id || idx}
              type="button"
              onClick={() => setActiveTab(idx)}
              className={cn(
                "flex cursor-pointer items-center gap-1.5 rounded-lg px-3 py-1 text-xs font-medium transition-colors select-none",
                isActive
                  ? "bg-primary font-semibold text-primary-foreground shadow-2xs"
                  : "bg-muted/60 text-muted-foreground hover:bg-muted hover:text-foreground"
              )}
            >
              <span>{tc.title || `Case ${idx + 1}`}</span>
            </button>
          )
        })}

        {!readOnly && (
          <Tooltip>
            <TooltipTrigger
              render={
                <button
                  type="button"
                  onClick={handleAddTestCase}
                  className="flex size-6.5 cursor-pointer items-center justify-center rounded-lg border border-dashed border-border/80 text-muted-foreground transition-colors hover:border-foreground hover:text-foreground"
                >
                  <Plus className="size-3.5" />
                </button>
              }
            />
            <TooltipContent>Add Case</TooltipContent>
          </Tooltip>
        )}
      </div>

      {activeCase ? (
        <div className="space-y-3 rounded-lg border border-border/70 bg-muted/20 p-3.5">
          <div className="flex items-center justify-between">
            <Label className="text-xs font-semibold text-foreground">
              {problemType === "DATABASE" ? "SQL Init Script" : "Input"}
            </Label>

            {!readOnly && (
              <div className="flex items-center gap-0.5">
                <Tooltip>
                  <TooltipContent>Move Earlier</TooltipContent>
                  <TooltipTrigger
                    render={
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon-xs"
                        disabled={activeIndex === 0}
                        onClick={() => handleMoveTestCase(activeIndex, -1)}
                        className="h-6 w-6 cursor-pointer text-muted-foreground hover:text-foreground disabled:opacity-30"
                      >
                        <ChevronUp className="size-3.5" />
                      </Button>
                    }
                  />
                </Tooltip>

                <Tooltip>
                  <TooltipContent>Move Later</TooltipContent>
                  <TooltipTrigger
                    render={
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon-xs"
                        disabled={activeIndex === testCases.length - 1}
                        onClick={() => handleMoveTestCase(activeIndex, 1)}
                        className="h-6 w-6 cursor-pointer text-muted-foreground hover:text-foreground disabled:opacity-30"
                      >
                        <ChevronDown className="size-3.5" />
                      </Button>
                    }
                  />
                </Tooltip>

                <Tooltip>
                  <TooltipContent>Duplicate Case</TooltipContent>
                  <TooltipTrigger
                    render={
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon-xs"
                        onClick={() => handleDuplicateTestCase(activeIndex)}
                        className="h-6 w-6 cursor-pointer text-muted-foreground hover:text-foreground"
                      >
                        <Copy className="size-3.5" />
                      </Button>
                    }
                  />
                </Tooltip>

                {testCases.length > 1 && (
                  <Tooltip>
                    <TooltipTrigger
                      render={
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon-xs"
                          onClick={() => handleRemoveTestCase(activeIndex)}
                          className="h-6 w-6 cursor-pointer text-destructive hover:bg-destructive/10"
                        >
                          <Trash2 className="size-3.5" />
                        </Button>
                      }
                    />
                    <TooltipContent>Delete Case</TooltipContent>
                  </Tooltip>
                )}
              </div>
            )}
          </div>

          {problemType === "DATABASE" ? (
            <div className="overflow-hidden rounded-md border border-border/80">
              <MonacoEditor
                language="sql"
                value={activeCase.input}
                onChange={(val) => handleUpdateInput(val)}
                readOnly={readOnly}
                height="200px"
                bordered={false}
                placeholder="-- DDL & DML (CREATE TABLE, INSERT INTO...)"
              />
            </div>
          ) : (
            <Textarea
              disabled={readOnly}
              rows={5}
              value={activeCase.input}
              onChange={(e) => handleUpdateInput(e.target.value)}
              placeholder="[2,7,11,15]&#10;9"
              className="bg-background font-mono text-xs"
            />
          )}
        </div>
      ) : (
        <div className="rounded-lg border border-dashed border-border/80 p-5 text-center text-xs text-muted-foreground">
          No test cases defined. Click &quot;Add Case&quot; to begin.
        </div>
      )}
    </div>
  )
}

export default TestcasesSection
