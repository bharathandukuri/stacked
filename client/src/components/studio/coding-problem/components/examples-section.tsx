import { useState } from "react"
import {
  Plus,
  Trash2,
  ChevronUp,
  ChevronDown,
  BookOpen,
  Eye,
  Edit3,
  Table as TableIcon,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Badge } from "@/components/ui/badge"
import { MarkdownPreview } from "@/components/markdown-preview/markdown-preview"
import { cn } from "@/lib/utils"
import {
  INPUT_STRUCTURES,
  type ProblemType,
  type ProblemParameter,
  type DatabaseTableSchema,
  type ProblemExample,
} from "../types"

export interface ExamplesSectionProps {
  problemType: ProblemType
  parameters?: ProblemParameter[]
  databaseTables?: DatabaseTableSchema[]
  examples: ProblemExample[]
  onChange: (examples: ProblemExample[]) => void
  readOnly?: boolean
  className?: string
}

export function ExamplesSection({
  problemType,
  parameters = [],
  databaseTables = [],
  examples,
  onChange,
  readOnly = false,
  className,
}: ExamplesSectionProps) {
  const [activeTab, setActiveTab] = useState(0)
  const [previewExplanation, setPreviewExplanation] = useState(false)

  const handleAddExample = () => {
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

    const newExample: ProblemExample = {
      id: `example-${Date.now()}`,
      inputs: newInputs,
      output: "",
      explanation: "",
    }

    const updated = [...examples, newExample]
    onChange(updated)
    setActiveTab(updated.length - 1)
  }

  const handleRemoveExample = (index: number) => {
    if (examples.length <= 1) return
    const updated = examples.filter((_, i) => i !== index)
    onChange(updated)
    setActiveTab(Math.max(0, Math.min(activeTab, updated.length - 1)))
  }

  const handleMoveExample = (fromIndex: number, direction: -1 | 1) => {
    const toIndex = fromIndex + direction
    if (toIndex < 0 || toIndex >= examples.length) return

    const updated = [...examples]
    const item = updated[fromIndex]
    updated[fromIndex] = updated[toIndex]
    updated[toIndex] = item
    onChange(updated)
    setActiveTab(toIndex)
  }

  const handleUpdateInput = (
    exampleIndex: number,
    key: string,
    val: string
  ) => {
    // Force strictly single line for each parameter value
    const singleLineVal = val.replace(/\r?\n+/g, " ")
    const updated = [...examples]
    updated[exampleIndex] = {
      ...updated[exampleIndex],
      inputs: {
        ...updated[exampleIndex].inputs,
        [key]: singleLineVal,
      },
    }
    onChange(updated)
  }

  const handleUpdateOutput = (exampleIndex: number, val: string) => {
    const updated = [...examples]
    updated[exampleIndex] = {
      ...updated[exampleIndex],
      output: val,
    }
    onChange(updated)
  }

  const handleUpdateExplanation = (exampleIndex: number, val: string) => {
    const updated = [...examples]
    updated[exampleIndex] = {
      ...updated[exampleIndex],
      explanation: val,
    }
    onChange(updated)
  }

  const currentExample = examples[activeTab] || examples[0]

  return (
    <div
      className={cn(
        "space-y-4 rounded-xl border border-border/80 bg-card p-5 shadow-xs",
        className
      )}
    >
      {/* Header */}
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div className="space-y-0.5">
          <div className="flex items-center gap-2">
            <BookOpen className="size-4 text-primary" />
            <h3 className="text-sm font-semibold text-foreground">Examples</h3>
            <Badge variant="secondary" className="px-2 py-0 font-mono text-xs">
              {examples.length} {examples.length === 1 ? "example" : "examples"}
            </Badge>
          </div>
          <p className="text-xs text-muted-foreground">
            Representative inputs, expected outputs, and explanations displayed
            to help users understand the problem.
          </p>
        </div>

        {!readOnly && (
          <Button
            type="button"
            variant="outline"
            size="xs"
            onClick={handleAddExample}
            className="cursor-pointer gap-1 self-start text-xs sm:self-auto"
          >
            <Plus className="size-3.5" />
            <span>Add Example</span>
          </Button>
        )}
      </div>

      {/* Example Tabs & Reordering */}
      <div className="flex flex-wrap items-center gap-1.5 border-b border-border/60 pb-3">
        {examples.map((ex, idx) => {
          const isActive = idx === activeTab
          return (
            <button
              key={ex.id || idx}
              type="button"
              onClick={() => setActiveTab(idx)}
              className={cn(
                "flex cursor-pointer items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-medium transition-colors select-none",
                isActive
                  ? "bg-primary font-semibold text-primary-foreground shadow-2xs"
                  : "bg-muted/60 text-muted-foreground hover:bg-muted hover:text-foreground"
              )}
            >
              <span>Example {idx + 1}</span>
            </button>
          )
        })}

        {!readOnly && (
          <button
            type="button"
            onClick={handleAddExample}
            className="flex size-7 cursor-pointer items-center justify-center rounded-lg border border-dashed border-border/80 text-muted-foreground transition-colors hover:border-foreground hover:text-foreground"
            title="Add Example"
          >
            <Plus className="size-3.5" />
          </button>
        )}
      </div>

      {/* Active Example Body */}
      {currentExample ? (
        <div className="space-y-4 rounded-xl border border-border/70 bg-muted/20 p-4">
          {/* Example Toolbar (Reordering & Delete) */}
          <div className="flex items-center justify-between border-b border-border/60 pb-2.5">
            <span className="text-xs font-semibold text-foreground">
              Editing Example {activeTab + 1}
            </span>

            <div className="flex items-center gap-1">
              {!readOnly && (
                <>
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon-xs"
                    disabled={activeTab === 0}
                    onClick={() => handleMoveExample(activeTab, -1)}
                    className="h-7 w-7 cursor-pointer text-muted-foreground hover:text-foreground disabled:opacity-30"
                    title="Move Example Earlier"
                  >
                    <ChevronUp className="size-3.5" />
                  </Button>

                  <Button
                    type="button"
                    variant="ghost"
                    size="icon-xs"
                    disabled={activeTab === examples.length - 1}
                    onClick={() => handleMoveExample(activeTab, 1)}
                    className="h-7 w-7 cursor-pointer text-muted-foreground hover:text-foreground disabled:opacity-30"
                    title="Move Example Later"
                  >
                    <ChevronDown className="size-3.5" />
                  </Button>

                  {examples.length > 1 && (
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon-xs"
                      onClick={() => handleRemoveExample(activeTab)}
                      className="h-7 w-7 cursor-pointer text-destructive hover:bg-destructive/10"
                      title="Delete Example"
                    >
                      <Trash2 className="size-3.5" />
                    </Button>
                  )}
                </>
              )}
            </div>
          </div>

          {/* GENERIC PROBLEM EXAMPLES */}
          {problemType === "GENERIC" ? (
            <div className="space-y-4">
              {/* Ordered Parameter Inputs (Strictly 1 line per parameter) */}
              <div className="space-y-2.5">
                <div className="flex items-center justify-between">
                  <Label className="text-xs font-semibold text-foreground">
                    Input:
                  </Label>
                  <span className="text-[11px] text-muted-foreground">
                    Follows parameter order (1 line per parameter)
                  </span>
                </div>

                <div className="space-y-2">
                  {parameters.length === 0 ? (
                    <div className="rounded-lg border border-dashed border-border/80 p-3 text-center text-xs text-muted-foreground">
                      No parameters defined yet. Configure input parameters
                      above.
                    </div>
                  ) : (
                    parameters.map((param, pIdx) => {
                      const val = currentExample.inputs[param.name] ?? ""
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
                                      activeTab,
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
                              handleUpdateInput(
                                activeTab,
                                param.name,
                                e.target.value
                              )
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

              {/* Output (Single Line) */}
              <div className="space-y-1.5">
                <Label className="text-xs font-semibold text-foreground">
                  Output:
                </Label>
                <Input
                  disabled={readOnly}
                  value={currentExample.output}
                  onChange={(e) =>
                    handleUpdateOutput(activeTab, e.target.value)
                  }
                  placeholder="e.g. [0, 1]"
                  className="h-8.5 bg-background font-mono text-xs font-medium"
                />
              </div>

              {/* Explanation with Live Markdown Preview Toggle */}
              <div className="space-y-1.5">
                <div className="flex items-center justify-between">
                  <Label className="text-xs font-semibold text-foreground">
                    Explanation:
                  </Label>
                  <button
                    type="button"
                    onClick={() => setPreviewExplanation(!previewExplanation)}
                    className="flex cursor-pointer items-center gap-1 text-[11px] text-primary hover:underline"
                  >
                    {previewExplanation ? (
                      <>
                        <Edit3 className="size-3" />
                        <span>Edit</span>
                      </>
                    ) : (
                      <>
                        <Eye className="size-3" />
                        <span>Preview Markdown</span>
                      </>
                    )}
                  </button>
                </div>

                {previewExplanation ? (
                  <div className="min-h-16 rounded-md border border-border bg-background p-3 text-xs">
                    {currentExample.explanation ? (
                      <MarkdownPreview value={currentExample.explanation} />
                    ) : (
                      <span className="text-muted-foreground italic">
                        No explanation provided.
                      </span>
                    )}
                  </div>
                ) : (
                  <Textarea
                    disabled={readOnly}
                    rows={2}
                    value={currentExample.explanation || ""}
                    onChange={(e) =>
                      handleUpdateExplanation(activeTab, e.target.value)
                    }
                    placeholder="e.g. The numbers at index 0 and 1 add up to 9."
                    className="min-h-16 bg-background text-xs"
                  />
                )}
              </div>

              {/* Clean LeetCode-style presentation preview */}
              {parameters.length > 0 && (
                <div className="space-y-2 rounded-lg border border-border/60 bg-muted/30 p-3 text-xs">
                  <span className="font-mono text-[11px] font-semibold tracking-wide text-foreground text-muted-foreground uppercase">
                    LeetCode-Style Output Preview
                  </span>
                  <div className="space-y-2 rounded border border-border/50 bg-background/90 p-3 font-mono text-xs text-foreground">
                    <div>
                      <span className="font-bold text-foreground">Input:</span>
                      <div className="mt-0.5 space-y-0.5 pl-2 text-primary">
                        {parameters.map((param) => (
                          <div key={param.name}>
                            {currentExample.inputs[param.name] || (
                              <span className="text-muted-foreground/40 italic">
                                &lt;empty line&gt;
                              </span>
                            )}
                          </div>
                        ))}
                      </div>
                    </div>

                    <div>
                      <span className="font-bold text-foreground">Output:</span>
                      <div className="mt-0.5 pl-2 font-semibold text-foreground">
                        {currentExample.output || (
                          <span className="text-muted-foreground/40 italic">
                            &lt;empty output&gt;
                          </span>
                        )}
                      </div>
                    </div>

                    {currentExample.explanation && (
                      <div>
                        <span className="font-bold text-foreground">
                          Explanation:
                        </span>
                        <div className="mt-0.5 pl-2 font-sans text-xs text-muted-foreground">
                          {currentExample.explanation}
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>
          ) : (
            /* DATABASE PROBLEM EXAMPLES */
            <div className="space-y-4">
              {/* Input Tables Grid */}
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
                      const tableDataRaw =
                        currentExample.inputs[table.name] ?? "[]"
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
                          activeTab,
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
                          activeTab,
                          table.name,
                          JSON.stringify(updatedRows, null, 2)
                        )
                      }

                      const handleRemoveRow = (rIdx: number) => {
                        const updatedRows = rows.filter((_, i) => i !== rIdx)
                        handleUpdateInput(
                          activeTab,
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
                              No rows. Click &quot;Add Row&quot; to insert
                              sample data for {table.name}.
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

              {/* Output Table */}
              <div className="space-y-1.5">
                <Label className="text-xs font-semibold text-foreground">
                  Output Table (Expected Result):
                </Label>
                <Textarea
                  disabled={readOnly}
                  rows={3}
                  value={currentExample.output}
                  onChange={(e) =>
                    handleUpdateOutput(activeTab, e.target.value)
                  }
                  placeholder={`+----+---------+\n| id | name    |\n+----+---------+\n| 1  | Alice   |\n+----+---------+`}
                  className="bg-background font-mono text-xs"
                />
              </div>

              {/* Explanation with Live Markdown Preview Toggle */}
              <div className="space-y-1.5">
                <div className="flex items-center justify-between">
                  <Label className="text-xs font-semibold text-foreground">
                    Explanation:
                  </Label>
                  <button
                    type="button"
                    onClick={() => setPreviewExplanation(!previewExplanation)}
                    className="flex cursor-pointer items-center gap-1 text-[11px] text-primary hover:underline"
                  >
                    {previewExplanation ? (
                      <>
                        <Edit3 className="size-3" />
                        <span>Edit</span>
                      </>
                    ) : (
                      <>
                        <Eye className="size-3" />
                        <span>Preview Markdown</span>
                      </>
                    )}
                  </button>
                </div>

                {previewExplanation ? (
                  <div className="min-h-16 rounded-md border border-border bg-background p-3 text-xs">
                    {currentExample.explanation ? (
                      <MarkdownPreview value={currentExample.explanation} />
                    ) : (
                      <span className="text-muted-foreground italic">
                        No explanation provided.
                      </span>
                    )}
                  </div>
                ) : (
                  <Textarea
                    disabled={readOnly}
                    rows={2}
                    value={currentExample.explanation || ""}
                    onChange={(e) =>
                      handleUpdateExplanation(activeTab, e.target.value)
                    }
                    placeholder="Explain why this query output is produced from the sample input tables."
                    className="min-h-16 bg-background text-xs"
                  />
                )}
              </div>
            </div>
          )}
        </div>
      ) : (
        <div className="rounded-lg border border-dashed border-border/80 p-6 text-center text-xs text-muted-foreground">
          No examples created. Click &quot;Add Example&quot; to explain the
          problem.
        </div>
      )}
    </div>
  )
}

export default ExamplesSection
