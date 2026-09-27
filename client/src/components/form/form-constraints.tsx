import { useState } from "react"
import { Plus, Trash2, Eye, EyeOff, ChevronUp, ChevronDown } from "lucide-react"
import { FormBase, type FormControlProps } from "@/components/form/form-base"
import { useFieldContext } from "@/hooks/form/create-form-hooks"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip"
import { cn } from "@/lib/utils"
import { MarkdownPreview } from "@/components/markdown-preview/markdown-preview"

export interface FormConstraintsProps extends FormControlProps {
  placeholder?: string
  className?: string
}

const COMMON_PRESETS = [
  "1 <= nums.length <= 10^5",
  "-10^9 <= nums[i] <= 10^9",
  "1 <= s.length <= 1000",
]

export function FormConstraints({
  placeholder = "e.g. 1 <= nums.length <= 10^5",
  className,
  ...controlProps
}: FormConstraintsProps) {
  const field = useFieldContext<string[]>()
  const constraints = field.state.value ?? []
  const [showPreview, setShowPreview] = useState(true)

  const handleAddConstraint = (initialVal = "") => {
    field.handleChange([...constraints, initialVal])
  }

  const handleUpdateConstraint = (index: number, val: string) => {
    const next = [...constraints]
    next[index] = val
    field.handleChange(next)
  }

  const handleRemoveConstraint = (index: number) => {
    field.handleChange(constraints.filter((_, i) => i !== index))
  }

  const handleMoveConstraint = (fromIndex: number, direction: -1 | 1) => {
    const toIndex = fromIndex + direction
    if (toIndex < 0 || toIndex >= constraints.length) return

    const next = [...constraints]
    const item = next[fromIndex]
    next[fromIndex] = next[toIndex]
    next[toIndex] = item
    field.handleChange(next)
  }

  return (
    <FormBase
      {...controlProps}
      labelAction={
        <div className="flex items-center gap-2">
          {constraints.length > 0 && (
            <Badge
              variant="secondary"
              className="h-5 px-1.5 font-mono text-[10px] text-muted-foreground"
            >
              {constraints.length} constraint{constraints.length !== 1 && "s"}
            </Badge>
          )}

          <Tooltip>
            <TooltipTrigger
              render={
                <Button
                  type="button"
                  variant="ghost"
                  size="xs"
                  onClick={() => setShowPreview(!showPreview)}
                  className="h-5.5 cursor-pointer gap-1 px-2 text-[11px] text-muted-foreground hover:text-foreground"
                >
                  {showPreview ? (
                    <>
                      <EyeOff className="size-3" />
                      <span>Hide Preview</span>
                    </>
                  ) : (
                    <>
                      <Eye className="size-3" />
                      <span>Show Preview</span>
                    </>
                  )}
                </Button>
              }
            />
            <TooltipContent>
              {showPreview ? "Hide preview" : "Show preview"}
            </TooltipContent>
          </Tooltip>

          <Button
            type="button"
            variant="outline"
            size="xs"
            onClick={() => handleAddConstraint()}
            className="h-5.5 cursor-pointer gap-1 px-2 text-[11px] text-primary"
          >
            <Plus className="size-3" /> Add Constraint
          </Button>
        </div>
      }
    >
      <div className={cn("space-y-3", className)}>
        {/* Constraints List */}
        {constraints.length === 0 ? (
          <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border/80 bg-muted/20 p-6 text-center text-xs text-muted-foreground">
            <p>No constraints added yet.</p>
            <div className="mt-2.5 flex flex-wrap items-center justify-center gap-1.5">
              <Button
                type="button"
                variant="secondary"
                size="xs"
                onClick={() => handleAddConstraint()}
                className="cursor-pointer gap-1 text-xs"
              >
                <Plus className="size-3" />
                <span>Add Constraint</span>
              </Button>

              {COMMON_PRESETS.map((preset) => (
                <Button
                  key={preset}
                  type="button"
                  variant="outline"
                  size="xs"
                  onClick={() => handleAddConstraint(preset)}
                  className="cursor-pointer font-mono text-[11px] text-muted-foreground"
                >
                  +{preset}
                </Button>
              ))}
            </div>
          </div>
        ) : (
          <div className="space-y-2">
            {constraints.map((constraint, index) => (
              <div
                key={index}
                className="group flex flex-col gap-1.5 rounded-xl border border-border/70 bg-card p-2.5 shadow-2xs transition-colors focus-within:border-ring/60"
              >
                <div className="flex items-center gap-2">
                  <span className="flex size-5 shrink-0 items-center justify-center rounded-full bg-muted font-mono text-[10px] font-semibold text-muted-foreground">
                    {index + 1}
                  </span>

                  <Input
                    value={constraint}
                    onChange={(e) =>
                      handleUpdateConstraint(index, e.target.value)
                    }
                    placeholder={placeholder}
                    className="h-8 flex-1 bg-background font-mono text-xs"
                  />

                  <div className="flex items-center gap-0.5">
                    <Tooltip>
                      <TooltipTrigger
                        render={
                          <Button
                            type="button"
                            variant="ghost"
                            size="icon-xs"
                            disabled={index === 0}
                            onClick={() => handleMoveConstraint(index, -1)}
                            className="shrink-0 cursor-pointer text-muted-foreground hover:text-foreground disabled:opacity-30"
                          >
                            <ChevronUp className="size-3.5" />
                          </Button>
                        }
                      />
                      <TooltipContent>Move Up</TooltipContent>
                    </Tooltip>

                    <Tooltip>
                      <TooltipTrigger
                        render={
                          <Button
                            type="button"
                            variant="ghost"
                            size="icon-xs"
                            disabled={index === constraints.length - 1}
                            onClick={() => handleMoveConstraint(index, 1)}
                            className="shrink-0 cursor-pointer text-muted-foreground hover:text-foreground disabled:opacity-30"
                          >
                            <ChevronDown className="size-3.5" />
                          </Button>
                        }
                      />
                      <TooltipContent>Move Down</TooltipContent>
                    </Tooltip>

                    <Tooltip>
                      <TooltipTrigger
                        render={
                          <Button
                            type="button"
                            variant="ghost"
                            size="icon-xs"
                            onClick={() => handleRemoveConstraint(index)}
                            className="shrink-0 cursor-pointer text-muted-foreground hover:text-destructive"
                          >
                            <Trash2 className="size-3.5" />
                          </Button>
                        }
                      />
                      <TooltipContent>Remove constraint</TooltipContent>
                    </Tooltip>
                  </div>
                </div>

                {/* Inline Markdown Preview */}
                {showPreview && constraint.trim() && (
                  <div className="ml-7 flex items-center gap-2 rounded-md border border-border/40 bg-muted/40 px-2.5 py-1 text-xs text-foreground/90">
                    <span className="font-mono text-[10px] text-muted-foreground uppercase select-none">
                      preview:
                    </span>
                    <div className="text-xs [&_code]:rounded [&_code]:bg-muted [&_code]:px-1 [&_code]:py-0.5 [&_code]:font-mono [&_code]:text-[11px] [&>p]:m-0">
                      <MarkdownPreview value={constraint} />
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </FormBase>
  )
}

export default FormConstraints
