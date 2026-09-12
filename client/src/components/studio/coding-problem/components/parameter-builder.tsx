import { Plus, Trash2 } from "lucide-react"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Button } from "@/components/ui/button"
import {
  INPUT_STRUCTURES,
  type InputStructure,
  type ProblemParameter,
} from "../types"

interface ParameterBuilderProps {
  functionName: string
  onFunctionNameChange: (name: string) => void
  parameters: ProblemParameter[]
  onParametersChange: (params: ProblemParameter[]) => void
  error?: string
}

export function ParameterBuilder({
  functionName,
  onFunctionNameChange,
  parameters,
  onParametersChange,
  error,
}: ParameterBuilderProps) {
  const handleAddParameter = () => {
    const nextIdx = parameters.length + 1
    const newParam: ProblemParameter = {
      name: `param${nextIdx}`,
      structure: "VARIABLE",
    }
    onParametersChange([...parameters, newParam])
  }

  const handleRemoveParameter = (index: number) => {
    onParametersChange(parameters.filter((_, i) => i !== index))
  }

  const handleUpdateParamName = (index: number, name: string) => {
    const updated = [...parameters]
    updated[index] = {
      ...updated[index],
      name: name.trim().replace(/\s+/g, ""),
    }
    onParametersChange(updated)
  }

  const handleUpdateParamStructure = (
    index: number,
    structure: InputStructure
  ) => {
    const updated = [...parameters]
    updated[index] = { ...updated[index], structure }
    onParametersChange(updated)
  }

  return (
    <div className="space-y-4">
      {/* Function Name */}
      <div className="space-y-1.5">
        <Label className="text-xs font-semibold text-foreground">
          Function Name
        </Label>
        <Input
          value={functionName}
          onChange={(e) => onFunctionNameChange(e.target.value.trim())}
          placeholder="e.g. twoSum"
          className="font-mono text-xs font-medium"
        />
      </div>

      {/* Parameter Lines */}
      <div className="space-y-2">
        <div className="flex items-center justify-between">
          <Label className="text-xs font-semibold text-foreground">
            Parameters ({parameters.length})
          </Label>
          <Button
            type="button"
            variant="outline"
            size="xs"
            onClick={handleAddParameter}
            className="cursor-pointer gap-1 text-xs"
          >
            <Plus className="size-3.5" />
            <span>Add Parameter</span>
          </Button>
        </div>

        {parameters.length === 0 ? (
          <div className="rounded-lg border border-dashed border-border/80 p-4 text-center text-xs text-muted-foreground">
            No parameters defined yet.
          </div>
        ) : (
          <div className="space-y-2">
            {parameters.map((param, index) => (
              <div
                key={index}
                className="flex items-center gap-2 rounded-lg border border-border/60 bg-muted/20 p-2 text-xs"
              >
                <span className="flex size-5 shrink-0 items-center justify-center rounded-full bg-muted font-mono text-[10px] font-semibold text-muted-foreground">
                  {index + 1}
                </span>

                <Input
                  value={param.name}
                  onChange={(e) => handleUpdateParamName(index, e.target.value)}
                  placeholder={`param${index + 1}`}
                  className="h-8 flex-1 bg-background font-mono text-xs"
                />

                <div className="w-44 shrink-0">
                  <select
                    value={param.structure}
                    onChange={(e) =>
                      handleUpdateParamStructure(
                        index,
                        e.target.value as InputStructure
                      )
                    }
                    className="h-8 w-full rounded-md border border-input bg-background px-2.5 font-mono text-xs text-foreground shadow-2xs focus:ring-1 focus:ring-ring focus:outline-none"
                  >
                    {INPUT_STRUCTURES.map((s) => (
                      <option key={s.id} value={s.id}>
                        {s.label}
                      </option>
                    ))}
                  </select>
                </div>

                <Button
                  type="button"
                  variant="ghost"
                  size="icon-xs"
                  onClick={() => handleRemoveParameter(index)}
                  className="shrink-0 cursor-pointer text-muted-foreground hover:text-destructive"
                  title="Remove parameter"
                >
                  <Trash2 className="size-3.5" />
                </Button>
              </div>
            ))}
          </div>
        )}

        {error && <p className="text-xs text-destructive">{error}</p>}
      </div>
    </div>
  )
}

export default ParameterBuilder
