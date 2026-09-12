import { useState, useMemo } from "react"
import {
  Plus,
  Trash2,
  Copy,
  Check,
  ChevronUp,
  ChevronDown,
  FlaskConical,
  Eye,
  EyeOff,
  SlidersHorizontal,
  Code2,
  Table as TableIcon,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Badge } from "@/components/ui/badge"
import { cn } from "@/lib/utils"
import {
  INPUT_STRUCTURES,
  type ProblemType,
  type ProblemParameter,
  type DatabaseTableSchema,
  type TestCase,
} from "../types"
import { mapSourceToUi, mapUiToSource } from "../utils/testcase-mapper"

export interface SampleTestcasesSectionProps {
  problemType: ProblemType
  parameters?: ProblemParameter[]
  databaseTables?: DatabaseTableSchema[]
  testCases: TestCase[]
  onChange: (testCases: TestCase[]) => void
  readOnly?: boolean
  className?: string
}

export function SampleTestcasesSection({
  problemType,
  parameters = [],
  databaseTables = [],
  testCases,
  onChange,
  readOnly = false,
  className,
}: SampleTestcasesSectionProps) {
  const [activeTab, setActiveTab] = useState(0)
  const [viewMode, setViewMode] = useState<"ui" | "raw">("ui")
  const [copiedRaw, setCopiedRaw] = useState(false)

  const activeCase = testCases[activeTab] || testCases[0]

  // Counts
  const sampleCount = useMemo(
    () => testCases.filter((tc) => tc.isSample).length,
    [testCases]
  )
  const hiddenCount = testCases.length - sampleCount

  // Compute live raw line-per-parameter text representation
  const activeCaseRawInput = useMemo(() => {
    if (!activeCase || problemType !== "GENERIC") return ""
    return mapUiToSource(activeCase.inputs, parameters)
  }, [activeCase, parameters, problemType])

  const handleAddTestCase = (isSample = false) => {
    const newInputs: Record<string, string> = {}

    if (problemType === "GENERIC") {
      parameters.forEach((p) => {
        newInputs[p.name] = ""
      })
    } else {
      databaseTables.forEach((t) => {
        newInputs[t.name] = JSON.stringify([], null, 2)
      })
    }

    const newCase: TestCase = {
      id: `case-${Date.now()}`,
      inputs: newInputs,
      expectedOutput: "",
      isSample,
      explanation: "",
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
      inputs: { ...target.inputs },
    }

    const updated = [...testCases]
    updated.splice(index + 1, 0, duplicated)
    onChange(updated)
    setActiveTab(index + 1)
  }

  const handleRemoveTestCase = (index: number) => {
    if (testCases.length <= 1) return
    const updated = testCases.filter((_, i) => i !== index)
    onChange(updated)
    setActiveTab(Math.max(0, Math.min(activeTab, updated.length - 1)))
  }

  const handleMoveTestCase = (fromIndex: number, direction: -1 | 1) => {
    const toIndex = fromIndex + direction
    if (toIndex < 0 || toIndex >= testCases.length) return

    const updated = [...testCases]
    const item = updated[fromIndex]
    updated[fromIndex] = updated[toIndex]
    updated[toIndex] = item
    onChange(updated)
    setActiveTab(toIndex)
  }

  const handleToggleSample = (isSample: boolean) => {
    if (!activeCase) return
    const updated = [...testCases]
    updated[activeTab] = {
      ...activeCase,
      isSample,
    }
    onChange(updated)
  }

  const handleUpdateInput = (key: string, val: string) => {
    if (!activeCase) return
    // Strict 1-line-per-parameter: flatten any newlines
    const singleLineVal = val.replace(/\r?\n+/g, " ")
    const updated = [...testCases]
    updated[activeTab] = {
      ...activeCase,
      inputs: {
        ...activeCase.inputs,
        [key]: singleLineVal,
      },
    }
    onChange(updated)
  }

  const handleUpdateOutput = (val: string) => {
    if (!activeCase) return
    const updated = [...testCases]
    updated[activeTab] = {
      ...activeCase,
      expectedOutput: val,
    }
    onChange(updated)
  }

  // Handle direct editing of raw ordered input lines
  const handleRawInputChange = (newRawSource: string) => {
    if (!activeCase || problemType !== "GENERIC") return
    const { inputs } = mapSourceToUi(newRawSource, parameters)
    const updated = [...testCases]
    updated[activeTab] = {
      ...activeCase,
      inputs,
      source: newRawSource,
    }
    onChange(updated)
  }

  const handleCopyRaw = async () => {
    try {
      await navigator.clipboard.writeText(activeCaseRawInput)
      setCopiedRaw(true)
      setTimeout(() => setCopiedRaw(false), 2000)
    } catch {
      // Fallback
    }
  }

  return (
    <div
      className={cn(
        "space-y-4 rounded-xl border border-border/80 bg-card p-5 shadow-xs",
        className
      )}
    >
      {/* Section Header */}
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div className="space-y-0.5">
          <div className="flex flex-wrap items-center gap-2">
            <FlaskConical className="size-4 text-primary" />
            <h3 className="text-sm font-semibold text-foreground">
              Sample & Evaluation Test Cases
            </h3>
            <div className="flex items-center gap-1.5 font-mono text-xs">
              <Badge variant="secondary" className="px-2 py-0 text-[11px]">
                {testCases.length} Total
              </Badge>
              <Badge
                variant="outline"
                className="border-emerald-500/30 bg-emerald-500/10 px-2 py-0 text-[11px] text-emerald-600 dark:text-emerald-400"
              >
                {sampleCount} Sample (Public)
              </Badge>
              <Badge
                variant="outline"
                className="border-border bg-muted/60 px-2 py-0 text-[11px] text-muted-foreground"
              >
                {hiddenCount} Hidden (Evaluation)
              </Badge>
            </div>
          </div>
          <p className="text-xs text-muted-foreground">
            Execution inputs used by the test runner. Mark test cases as Sample
            (public in problem runner) or Hidden (evaluation only).
          </p>
        </div>

        {!readOnly && (
          <div className="flex items-center gap-2 self-start sm:self-auto">
            <Button
              type="button"
              variant="outline"
              size="xs"
              onClick={() => handleAddTestCase(false)}
              className="cursor-pointer gap-1 text-xs"
            >
              <Plus className="size-3.5" />
              <span>Add Test Case</span>
            </Button>
          </div>
        )}
      </div>

      {/* Case Tabs */}
      <div className="flex flex-wrap items-center gap-1.5 border-b border-border/60 pb-3">
        {testCases.map((tc, idx) => {
          const isActive = idx === activeTab
          return (
            <button
              key={tc.id || idx}
              type="button"
              onClick={() => setActiveTab(idx)}
              className={cn(
                "flex cursor-pointer items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-medium transition-colors select-none",
                isActive
                  ? "bg-primary font-semibold text-primary-foreground shadow-2xs"
                  : "bg-muted/60 text-muted-foreground hover:bg-muted hover:text-foreground"
              )}
            >
              <span>Case {idx + 1}</span>
              {tc.isSample ? (
                <span
                  className={cn(
                    "flex items-center gap-0.5 rounded px-1 text-[9px] font-semibold tracking-wider uppercase",
                    isActive
                      ? "bg-primary-foreground/20 text-primary-foreground"
                      : "bg-emerald-500/15 text-emerald-600 dark:text-emerald-400"
                  )}
                  title="Public sample test case"
                >
                  <Eye className="size-2.5" />
                  <span>Sample</span>
                </span>
              ) : (
                <span
                  className={cn(
                    "flex items-center gap-0.5 rounded px-1 text-[9px] font-semibold tracking-wider uppercase",
                    isActive
                      ? "bg-primary-foreground/20 text-primary-foreground"
                      : "bg-muted text-muted-foreground"
                  )}
                  title="Hidden evaluation test case"
                >
                  <EyeOff className="size-2.5" />
                  <span>Hidden</span>
                </span>
              )}
            </button>
          )
        })}

        {!readOnly && (
          <button
            type="button"
            onClick={() => handleAddTestCase(false)}
            className="flex size-7 cursor-pointer items-center justify-center rounded-lg border border-dashed border-border/80 text-muted-foreground transition-colors hover:border-foreground hover:text-foreground"
            title="Add Test Case"
          >
            <Plus className="size-3.5" />
          </button>
        )}
      </div>

      {/* Active Case Content */}
      {activeCase ? (
        <div className="space-y-4 rounded-xl border border-border/70 bg-muted/20 p-4">
          {/* Active Case Toolbar */}
          <div className="flex flex-col gap-2.5 border-b border-border/60 pb-3 sm:flex-row sm:items-center sm:justify-between">
            {/* Visibility Toggle */}
            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold text-foreground">
                Visibility:
              </span>
              <div className="inline-flex rounded-lg border border-border bg-background p-0.5 shadow-2xs">
                <button
                  type="button"
                  disabled={readOnly}
                  onClick={() => handleToggleSample(true)}
                  className={cn(
                    "flex cursor-pointer items-center gap-1 rounded-md px-2.5 py-1 text-xs font-medium transition-all select-none",
                    activeCase.isSample
                      ? "bg-emerald-500/15 font-semibold text-emerald-700 shadow-2xs dark:text-emerald-300"
                      : "text-muted-foreground hover:text-foreground"
                  )}
                >
                  <Eye className="size-3 text-emerald-600 dark:text-emerald-400" />
                  <span>Sample (Public in runner)</span>
                </button>
                <button
                  type="button"
                  disabled={readOnly}
                  onClick={() => handleToggleSample(false)}
                  className={cn(
                    "flex cursor-pointer items-center gap-1 rounded-md px-2.5 py-1 text-xs font-medium transition-all select-none",
                    !activeCase.isSample
                      ? "bg-muted font-semibold text-foreground shadow-2xs"
                      : "text-muted-foreground hover:text-foreground"
                  )}
                >
                  <EyeOff className="size-3" />
                  <span>Hidden (Evaluation only)</span>
                </button>
              </div>
            </div>

            {/* Action Tools */}
            <div className="flex items-center gap-1.5 self-end sm:self-auto">
              {problemType === "GENERIC" && (
                <div className="mr-1 flex items-center rounded-lg border border-border bg-background p-0.5">
                  <button
                    type="button"
                    onClick={() => setViewMode("ui")}
                    className={cn(
                      "flex cursor-pointer items-center gap-1 rounded-md px-2 py-0.5 text-xs font-medium transition-colors select-none",
                      viewMode === "ui"
                        ? "bg-muted font-semibold text-foreground"
                        : "text-muted-foreground hover:text-foreground"
                    )}
                  >
                    <SlidersHorizontal className="size-3" />
                    <span>Line Inputs</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setViewMode("raw")}
                    className={cn(
                      "flex cursor-pointer items-center gap-1 rounded-md px-2 py-0.5 text-xs font-medium transition-colors select-none",
                      viewMode === "raw"
                        ? "bg-muted font-semibold text-foreground"
                        : "text-muted-foreground hover:text-foreground"
                    )}
                  >
                    <Code2 className="size-3" />
                    <span>Raw Input</span>
                  </button>
                </div>
              )}

              {problemType === "GENERIC" && viewMode === "raw" && (
                <Button
                  type="button"
                  variant="ghost"
                  size="icon-xs"
                  onClick={handleCopyRaw}
                  className="h-7 w-7 cursor-pointer text-muted-foreground hover:text-foreground"
                  title="Copy raw input"
                >
                  {copiedRaw ? (
                    <Check className="size-3.5 text-emerald-500" />
                  ) : (
                    <Copy className="size-3.5" />
                  )}
                </Button>
              )}

              {!readOnly && (
                <>
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon-xs"
                    disabled={activeTab === 0}
                    onClick={() => handleMoveTestCase(activeTab, -1)}
                    className="h-7 w-7 cursor-pointer text-muted-foreground hover:text-foreground disabled:opacity-30"
                    title="Move Earlier"
                  >
                    <ChevronUp className="size-3.5" />
                  </Button>

                  <Button
                    type="button"
                    variant="ghost"
                    size="icon-xs"
                    disabled={activeTab === testCases.length - 1}
                    onClick={() => handleMoveTestCase(activeTab, 1)}
                    className="h-7 w-7 cursor-pointer text-muted-foreground hover:text-foreground disabled:opacity-30"
                    title="Move Later"
                  >
                    <ChevronDown className="size-3.5" />
                  </Button>

                  <Button
                    type="button"
                    variant="ghost"
                    size="icon-xs"
                    onClick={() => handleDuplicateTestCase(activeTab)}
                    className="h-7 w-7 cursor-pointer text-muted-foreground hover:text-foreground"
                    title="Duplicate Case"
                  >
                    <Copy className="size-3.5" />
                  </Button>

                  {testCases.length > 1 && (
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon-xs"
                      onClick={() => handleRemoveTestCase(activeTab)}
                      className="h-7 w-7 cursor-pointer text-destructive hover:bg-destructive/10"
                      title="Delete Case"
                    >
                      <Trash2 className="size-3.5" />
                    </Button>
                  )}
                </>
              )}
            </div>
          </div>

          {/* GENERIC PROBLEM TEST CASES */}
          {problemType === "GENERIC" ? (
            viewMode === "ui" ? (
              <div className="space-y-3">
                {/* Ordered Parameter Inputs (1 Line per parameter) */}
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <Label className="text-xs font-semibold text-foreground">
                      Test Input:
                    </Label>
                    <span className="text-[11px] text-muted-foreground">
                      1 line per parameter (follows parameter definition order)
                    </span>
                  </div>

                  <div className="space-y-2">
                    {parameters.length === 0 ? (
                      <div className="rounded-lg border border-dashed border-border/80 p-3 text-center text-xs text-muted-foreground">
                        No parameters defined yet. Configure parameters above to
                        set test inputs.
                      </div>
                    ) : (
                      parameters.map((param, pIdx) => {
                        const val = activeCase.inputs[param.name] ?? ""
                        const structMeta =
                          INPUT_STRUCTURES.find(
                            (s) => s.id === param.structure
                          ) || INPUT_STRUCTURES[0]
                        const isArray = param.structure !== "VARIABLE"

                        return (
                          <div key={param.name || pIdx} className="space-y-1">
                            <div className="flex items-center justify-between font-mono text-[11px] text-muted-foreground">
                              <span>
                                Line {pIdx + 1}:{" "}
                                <span className="font-semibold text-foreground">
                                  {structMeta.label}
                                </span>{" "}
                                <span className="font-normal text-muted-foreground">
                                  ({param.name})
                                </span>
                              </span>
                              {isArray && structMeta.defaultBrackets && (
                                <button
                                  type="button"
                                  onClick={() => {
                                    if (!val)
                                      handleUpdateInput(
                                        param.name,
                                        structMeta.defaultBrackets
                                      )
                                  }}
                                  className="cursor-pointer text-[10px] text-primary hover:underline"
                                >
                                  Insert {structMeta.defaultBrackets}
                                </button>
                              )}
                            </div>
                            <Input
                              disabled={readOnly}
                              value={val}
                              onChange={(e) =>
                                handleUpdateInput(param.name, e.target.value)
                              }
                              placeholder={structMeta.placeholder}
                              className="h-8.5 bg-background font-mono text-xs"
                            />
                          </div>
                        )
                      })
                    )}
                  </div>
                </div>

                {/* Expected Output */}
                <div className="space-y-1.5">
                  <Label className="text-xs font-semibold text-foreground">
                    Expected Output:
                  </Label>
                  <Input
                    disabled={readOnly}
                    value={activeCase.expectedOutput}
                    onChange={(e) => handleUpdateOutput(e.target.value)}
                    placeholder="e.g. [0, 1]"
                    className="h-8.5 bg-background font-mono text-xs font-medium"
                  />
                </div>
              </div>
            ) : (
              /* Raw Input Multi-line Mode (1 line per parameter) */
              <div className="space-y-3">
                <div className="space-y-1">
                  <div className="flex items-center justify-between text-[11px] text-muted-foreground">
                    <Label className="text-xs font-semibold text-foreground">
                      Raw Input (Ordered Lines):
                    </Label>
                    <span className="font-mono text-[10px]">
                      Line{" "}
                      {parameters.length === 0
                        ? "1"
                        : `1..${parameters.length}`}{" "}
                      = parameter value
                    </span>
                  </div>
                  <Textarea
                    disabled={readOnly}
                    rows={Math.max(4, parameters.length + 1)}
                    value={activeCaseRawInput}
                    onChange={(e) => handleRawInputChange(e.target.value)}
                    placeholder={`[2,7,11,15]\n9`}
                    className="bg-background font-mono text-xs"
                  />
                  <p className="text-[11px] text-muted-foreground">
                    Each parameter must occupy exactly one line in parameter
                    order without parameter names.
                  </p>
                </div>

                {/* Expected Output */}
                <div className="space-y-1.5">
                  <Label className="text-xs font-semibold text-foreground">
                    Expected Output:
                  </Label>
                  <Input
                    disabled={readOnly}
                    value={activeCase.expectedOutput}
                    onChange={(e) => handleUpdateOutput(e.target.value)}
                    placeholder="e.g. [0, 1]"
                    className="h-8.5 bg-background font-mono text-xs font-medium"
                  />
                </div>
              </div>
            )
          ) : (
            /* DATABASE PROBLEM TEST CASES */
            <div className="space-y-4">
              {/* Input Tables */}
              <div className="space-y-3">
                <Label className="flex items-center gap-1.5 text-xs font-semibold text-foreground">
                  <TableIcon className="size-3.5 text-primary" />
                  <span>Input Tables:</span>
                </Label>

                {databaseTables.length === 0 ? (
                  <div className="rounded-lg border border-dashed border-border/80 p-4 text-center text-xs text-muted-foreground">
                    No database tables defined yet. Add tables in the Database
                    Schema section above.
                  </div>
                ) : (
                  <div className="space-y-3">
                    {databaseTables.map((table) => {
                      const tableDataRaw = activeCase.inputs[table.name] ?? "[]"
                      let rows: Record<string, string>[] = []
                      try {
                        const parsed = JSON.parse(tableDataRaw)
                        if (Array.isArray(parsed)) rows = parsed
                      } catch {
                        // ignore
                      }

                      const handleAddRow = () => {
                        const newRow: Record<string, string> = {}
                        table.columns.forEach((c) => {
                          newRow[c.name] = ""
                        })
                        const updatedRows = [...rows, newRow]
                        handleUpdateInput(
                          table.name,
                          JSON.stringify(updatedRows, null, 2)
                        )
                      }

                      const handleUpdateCell = (
                        rIdx: number,
                        colName: string,
                        val: string
                      ) => {
                        const updatedRows = [...rows]
                        updatedRows[rIdx] = {
                          ...updatedRows[rIdx],
                          [colName]: val,
                        }
                        handleUpdateInput(
                          table.name,
                          JSON.stringify(updatedRows, null, 2)
                        )
                      }

                      const handleRemoveRow = (rIdx: number) => {
                        const updatedRows = rows.filter((_, i) => i !== rIdx)
                        handleUpdateInput(
                          table.name,
                          JSON.stringify(updatedRows, null, 2)
                        )
                      }

                      return (
                        <div
                          key={table.id || table.name}
                          className="space-y-2 rounded-lg border border-border/70 bg-card p-3"
                        >
                          <div className="flex items-center justify-between">
                            <span className="font-mono text-xs font-semibold text-foreground">
                              {table.name} Table ({rows.length} rows)
                            </span>
                            {!readOnly && (
                              <Button
                                type="button"
                                variant="outline"
                                size="xs"
                                onClick={handleAddRow}
                                className="h-6 gap-1 px-2 text-[10px]"
                              >
                                <Plus className="size-2.5" />
                                <span>Add Row</span>
                              </Button>
                            )}
                          </div>

                          {rows.length === 0 ? (
                            <div className="rounded border border-dashed border-border/60 p-3 text-center text-[11px] text-muted-foreground">
                              No rows. Click &quot;Add Row&quot; to insert test
                              data for {table.name}.
                            </div>
                          ) : (
                            <div className="overflow-x-auto rounded border border-border/60">
                              <table className="w-full text-left text-xs">
                                <thead className="border-b border-border/60 bg-muted/50 font-mono text-[11px] text-muted-foreground uppercase">
                                  <tr>
                                    {table.columns.map((col) => (
                                      <th
                                        key={col.name}
                                        className="px-3 py-1.5 font-semibold"
                                      >
                                        {col.name} ({col.type})
                                      </th>
                                    ))}
                                    {!readOnly && (
                                      <th className="w-10 px-2 py-1.5 text-right" />
                                    )}
                                  </tr>
                                </thead>
                                <tbody>
                                  {rows.map((row, rIdx) => (
                                    <tr
                                      key={rIdx}
                                      className="border-b border-border/40 last:border-0 hover:bg-muted/20"
                                    >
                                      {table.columns.map((col) => (
                                        <td key={col.name} className="p-1.5">
                                          <Input
                                            disabled={readOnly}
                                            value={row[col.name] ?? ""}
                                            onChange={(e) =>
                                              handleUpdateCell(
                                                rIdx,
                                                col.name,
                                                e.target.value
                                              )
                                            }
                                            className="h-7 bg-background font-mono text-xs"
                                            placeholder="null"
                                          />
                                        </td>
                                      ))}
                                      {!readOnly && (
                                        <td className="p-1.5 text-right">
                                          <button
                                            type="button"
                                            onClick={() =>
                                              handleRemoveRow(rIdx)
                                            }
                                            className="cursor-pointer p-1 text-muted-foreground hover:text-destructive"
                                            title="Delete Row"
                                          >
                                            <Trash2 className="size-3" />
                                          </button>
                                        </td>
                                      )}
                                    </tr>
                                  ))}
                                </tbody>
                              </table>
                            </div>
                          )}
                        </div>
                      )
                    })}
                  </div>
                )}
              </div>

              {/* Expected Output Table */}
              <div className="space-y-1.5">
                <Label className="text-xs font-semibold text-foreground">
                  Expected Output Table:
                </Label>
                <Textarea
                  disabled={readOnly}
                  rows={3}
                  value={activeCase.expectedOutput}
                  onChange={(e) => handleUpdateOutput(e.target.value)}
                  placeholder={`+----+---------+\n| id | name    |\n+----+---------+\n| 1  | Alice   |\n+----+---------+`}
                  className="bg-background font-mono text-xs"
                />
              </div>
            </div>
          )}
        </div>
      ) : (
        <div className="rounded-lg border border-dashed border-border/80 p-6 text-center text-xs text-muted-foreground">
          No test cases configured. Click &quot;Add Test Case&quot; to begin.
        </div>
      )}
    </div>
  )
}

export default SampleTestcasesSection
