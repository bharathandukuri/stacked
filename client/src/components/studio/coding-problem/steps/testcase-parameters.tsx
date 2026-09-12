import { useState, useId } from "react"
import {
  Code2,
  Plus,
  Trash2,
  ChevronUp,
  ChevronDown,
  ArrowRight,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Badge } from "@/components/ui/badge"
import { cn } from "@/lib/utils"
import {
  INPUT_STRUCTURES,
  type InputStructure,
  type ProblemParameter,
} from "../types"

export interface TestcaseParametersProps {
  functionName?: string
  onFunctionNameChange?: (name: string) => void
  parameters?: ProblemParameter[]
  onParametersChange?: (params: ProblemParameter[]) => void
  className?: string
  readOnly?: boolean
}

const DEFAULT_PARAMS: ProblemParameter[] = [
  { name: "nums", structure: "ARRAY_1D" },
  { name: "target", structure: "VARIABLE" },
]

export function TestcaseParameters({
  functionName: controlledFnName,
  onFunctionNameChange,
  parameters: controlledParams,
  onParametersChange,
  className,
  readOnly = false,
}: TestcaseParametersProps) {
  const [internalFnName, setInternalFnName] = useState("twoSum")
  const [internalParams, setInternalParams] =
    useState<ProblemParameter[]>(DEFAULT_PARAMS)

  const functionName =
    controlledFnName !== undefined ? controlledFnName : internalFnName
  const parameters =
    controlledParams !== undefined ? controlledParams : internalParams

  const updateFunctionName = (name: string) => {
    const cleaned = name.trim().replace(/\s+/g, "")
    if (onFunctionNameChange) onFunctionNameChange(cleaned)
    else setInternalFnName(cleaned)
  }

  const updateParameters = (newParams: ProblemParameter[]) => {
    if (onParametersChange) onParametersChange(newParams)
    else setInternalParams(newParams)
  }

  const handleAddParameter = () => {
    const nextIdx = parameters.length + 1
    const newParam: ProblemParameter = {
      name: `param${nextIdx}`,
      structure: "VARIABLE",
    }
    updateParameters([...parameters, newParam])
  }

  const handleRemoveParameter = (index: number) => {
    if (parameters.length <= 1) return
    updateParameters(parameters.filter((_, i) => i !== index))
  }

  const handleMoveParameter = (fromIndex: number, direction: -1 | 1) => {
    const toIndex = fromIndex + direction
    if (toIndex < 0 || toIndex >= parameters.length) return
    const updated = [...parameters]
    const temp = updated[fromIndex]
    updated[fromIndex] = updated[toIndex]
    updated[toIndex] = temp
    updateParameters(updated)
  }

  const handleUpdateParamName = (index: number, name: string) => {
    const updated = [...parameters]
    updated[index] = {
      ...updated[index],
      name: name.trim().replace(/\s+/g, ""),
    }
    updateParameters(updated)
  }

  const handleUpdateParamStructure = (
    index: number,
    structure: InputStructure
  ) => {
    const updated = [...parameters]
    updated[index] = {
      ...updated[index],
      structure,
    }
    updateParameters(updated)
  }

  const structureItems = INPUT_STRUCTURES.reduce<Record<string, string>>(
    (acc, s) => {
      acc[s.id] = s.label
      return acc
    },
    {}
  )

  const functionNameInputId = useId()

  return (
    <div
      className={cn(
        "space-y-5 rounded-xl border border-border/80 bg-card p-5 shadow-xs",
        className
      )}
    >
      {/* Header */}
      <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
        <div className="space-y-0.5">
          <div className="flex items-center gap-2">
            <Code2 className="size-4 text-primary" />
            <h3 className="text-sm font-semibold text-foreground">
              Problem Parameters & Input Structure
            </h3>
            <Badge variant="secondary" className="px-2 py-0 font-mono text-xs">
              {parameters.length} {parameters.length === 1 ? "line" : "lines"}
            </Badge>
          </div>
          <p className="text-xs text-muted-foreground">
            Define the shape and ordering of input parameters. Each parameter
            occupies exactly one line in test cases and examples.
          </p>
        </div>

        {!readOnly && (
          <Button
            type="button"
            variant="outline"
            size="xs"
            onClick={handleAddParameter}
            className="cursor-pointer gap-1 self-start text-xs sm:self-auto"
          >
            <Plus className="size-3.5" />
            <span>Add Parameter</span>
          </Button>
        )}
      </div>

      {/* Function / Entry Method Name (Optional) */}
      <div className="max-w-md space-y-1.5">
        <Label
          htmlFor={functionNameInputId}
          className="text-xs font-semibold text-foreground"
        >
          Function Entry Point{" "}
          <span className="font-normal text-muted-foreground">(Optional)</span>
        </Label>
        <Input
          id={functionNameInputId}
          disabled={readOnly}
          value={functionName}
          onChange={(e) => updateFunctionName(e.target.value)}
          placeholder="e.g. twoSum"
          className="h-8.5 bg-background font-mono text-xs"
        />
        <p className="text-[11px] text-muted-foreground">
          The main entry method name invoked by solution runners across all
          supported languages.
        </p>
      </div>

      {/* Parameters List */}
      <div className="space-y-2.5">
        <div className="flex items-center justify-between">
          <Label className="text-xs font-semibold text-foreground">
            Ordered Parameter Lines
          </Label>
          <span className="text-[11px] text-muted-foreground">
            Line order matches input order
          </span>
        </div>

        {parameters.length === 0 ? (
          <div className="rounded-lg border border-dashed border-border/80 p-4 text-center text-xs text-muted-foreground">
            No parameters defined. Click &quot;Add Parameter&quot; to begin.
          </div>
        ) : (
          <div className="space-y-2">
            {parameters.map((param, index) => {
              const structMeta =
                INPUT_STRUCTURES.find((s) => s.id === param.structure) ||
                INPUT_STRUCTURES[0]

              return (
                <div
                  key={index}
                  className="flex flex-col gap-2 rounded-lg border border-border/70 bg-muted/20 p-2.5 text-xs sm:flex-row sm:items-center"
                >
                  {/* Line Number Badge */}
                  <div className="flex shrink-0 items-center gap-1.5">
                    <span className="flex h-6 shrink-0 items-center justify-center rounded-md bg-muted px-2 font-mono text-[11px] font-semibold text-muted-foreground">
                      Line {index + 1}
                    </span>
                  </div>

                  {/* Parameter Name */}
                  <div className="min-w-0 flex-1">
                    <Input
                      disabled={readOnly}
                      value={param.name}
                      onChange={(e) =>
                        handleUpdateParamName(index, e.target.value)
                      }
                      placeholder={`param${index + 1}`}
                      className="h-8 bg-background font-mono text-xs"
                    />
                  </div>

                  <ArrowRight className="hidden size-3 shrink-0 text-muted-foreground sm:block" />

                  {/* Input Structure Dropdown */}
                  <div className="w-full shrink-0 sm:w-56">
                    <Select
                      disabled={readOnly}
                      value={param.structure}
                      items={structureItems}
                      onValueChange={(val) => {
                        if (val)
                          handleUpdateParamStructure(
                            index,
                            val as InputStructure
                          )
                      }}
                    >
                      <SelectTrigger className="h-8 w-full justify-between bg-background font-mono text-xs">
                        <SelectValue placeholder="Select structure" />
                      </SelectTrigger>
                      <SelectContent className="max-h-60 font-mono text-xs">
                        {INPUT_STRUCTURES.map((s) => (
                          <SelectItem key={s.id} value={s.id}>
                            {s.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  {/* Placeholder hint */}
                  <span className="hidden max-w-[140px] shrink-0 truncate font-mono text-[11px] text-muted-foreground lg:inline-block">
                    {structMeta.placeholder}
                  </span>

                  {/* Actions: Reorder & Remove */}
                  {!readOnly && (
                    <div className="flex shrink-0 items-center gap-0.5 self-end sm:self-auto">
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon-xs"
                        disabled={index === 0}
                        onClick={() => handleMoveParameter(index, -1)}
                        className="h-7 w-7 cursor-pointer text-muted-foreground hover:text-foreground disabled:opacity-30"
                        title="Move Line Earlier"
                      >
                        <ChevronUp className="size-3.5" />
                      </Button>

                      <Button
                        type="button"
                        variant="ghost"
                        size="icon-xs"
                        disabled={index === parameters.length - 1}
                        onClick={() => handleMoveParameter(index, 1)}
                        className="h-7 w-7 cursor-pointer text-muted-foreground hover:text-foreground disabled:opacity-30"
                        title="Move Line Later"
                      >
                        <ChevronDown className="size-3.5" />
                      </Button>

                      {parameters.length > 1 && (
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon-xs"
                          onClick={() => handleRemoveParameter(index)}
                          className="h-7 w-7 cursor-pointer text-destructive hover:bg-destructive/10"
                          title="Remove Parameter"
                        >
                          <Trash2 className="size-3.5" />
                        </Button>
                      )}
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        )}
      </div>

      {/* Input Line Rule Preview */}
      {parameters.length > 0 && (
        <div className="space-y-1.5 rounded-lg border border-border/60 bg-muted/40 p-3 text-xs">
          <div className="flex items-center justify-between text-muted-foreground">
            <span className="font-semibold text-foreground">
              Input Line Specification:
            </span>
            <span className="text-[11px]">Strictly 1 line per parameter</span>
          </div>
          <div className="space-y-0.5 rounded border border-border/50 bg-background/80 p-2 font-mono text-[11px]">
            {parameters.map((p, idx) => {
              const meta =
                INPUT_STRUCTURES.find((s) => s.id === p.structure) ||
                INPUT_STRUCTURES[0]
              return (
                <div
                  key={p.name || idx}
                  className="flex items-center justify-between text-foreground"
                >
                  <span>
                    Line {idx + 1}:{" "}
                    <span className="font-semibold text-primary">
                      {meta.placeholder}
                    </span>
                  </span>
                  <span className="text-[10px] text-muted-foreground">
                    ({p.name} → {meta.label})
                  </span>
                </div>
              )
            })}
          </div>
        </div>
      )}
    </div>
  )
}

export default TestcaseParameters
