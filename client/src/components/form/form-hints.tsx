import { Plus, Trash2, Lightbulb, ChevronUp, ChevronDown } from "lucide-react"
import { FormBase, type FormControlProps } from "@/components/form/form-base"
import { useFieldContext } from "@/hooks/form/create-form-hooks"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import { Badge } from "@/components/ui/badge"
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip"
import { cn } from "@/lib/utils"

export interface FormHintsProps extends FormControlProps {
  placeholder?: string
  className?: string
}

export function FormHints({
  placeholder = "Provide a hint using Markdown (e.g. You can use a hash map `Map<Integer, Integer>` to store complements)...",
  className,
  ...controlProps
}: FormHintsProps) {
  const field = useFieldContext<string[]>()
  const hints = field.state.value ?? []

  const handleAddHint = () => {
    field.handleChange([...hints, ""])
  }

  const handleUpdateHint = (index: number, val: string) => {
    const next = [...hints]
    next[index] = val
    field.handleChange(next)
  }

  const handleRemoveHint = (index: number) => {
    field.handleChange(hints.filter((_, i) => i !== index))
  }

  const handleMoveHint = (fromIndex: number, direction: -1 | 1) => {
    const toIndex = fromIndex + direction
    if (toIndex < 0 || toIndex >= hints.length) return

    const next = [...hints]
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
          {hints.length > 0 && (
            <Badge
              variant="secondary"
              className="h-5 px-1.5 font-mono text-[10px] text-muted-foreground"
            >
              {hints.length} hint{hints.length !== 1 && "s"}
            </Badge>
          )}

          <Button
            type="button"
            variant="outline"
            size="xs"
            onClick={handleAddHint}
            className="h-5.5 cursor-pointer gap-1 px-2 text-[11px] text-primary"
          >
            <Plus className="size-3" /> Add Hint
          </Button>
        </div>
      }
    >
      <div className={cn("space-y-3", className)}>
        {hints.length === 0 ? (
          <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border/80 bg-muted/20 p-6 text-center text-xs text-muted-foreground">
            <Lightbulb className="mb-1 size-5 text-amber-500" />
            <p>No hints added yet. Hints assist learners when stuck.</p>
            <Button
              type="button"
              variant="secondary"
              size="xs"
              onClick={handleAddHint}
              className="mt-2.5 cursor-pointer gap-1 text-xs"
            >
              <Plus className="size-3" />
              <span>Add First Hint</span>
            </Button>
          </div>
        ) : (
          <div className="space-y-3">
            {hints.map((hint, index) => {
              const isFirst = index === 0
              const isLast = index === hints.length - 1

              return (
                <div
                  key={index}
                  className="space-y-2 rounded-xl border border-border/70 bg-card p-3 shadow-2xs"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <span className="flex size-5 shrink-0 items-center justify-center rounded-full bg-amber-500/10 font-mono text-[10px] font-semibold text-amber-600 dark:text-amber-400">
                        {index + 1}
                      </span>
                      <span className="text-xs font-semibold text-foreground">
                        Hint {index + 1}
                      </span>
                    </div>

                    <div className="flex items-center gap-1">
                      {/* Reorder Buttons */}
                      <Tooltip>
                        <TooltipTrigger
                          render={
                            <Button
                              type="button"
                              variant="ghost"
                              size="icon-xs"
                              disabled={isFirst}
                              onClick={() => handleMoveHint(index, -1)}
                              className="h-6 w-6 cursor-pointer text-muted-foreground hover:text-foreground disabled:opacity-30"
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
                              disabled={isLast}
                              onClick={() => handleMoveHint(index, 1)}
                              className="h-6 w-6 cursor-pointer text-muted-foreground hover:text-foreground disabled:opacity-30"
                            >
                              <ChevronDown className="size-3.5" />
                            </Button>
                          }
                        />
                        <TooltipContent>Move Down</TooltipContent>
                      </Tooltip>

                      {/* Delete */}
                      <Tooltip>
                        <TooltipTrigger
                          render={
                            <Button
                              type="button"
                              variant="ghost"
                              size="icon-xs"
                              onClick={() => handleRemoveHint(index)}
                              className="h-6 w-6 cursor-pointer text-muted-foreground hover:text-destructive"
                            >
                              <Trash2 className="size-3.5" />
                            </Button>
                          }
                        />
                        <TooltipContent>Remove Hint {index + 1}</TooltipContent>
                      </Tooltip>
                    </div>
                  </div>

                  <Textarea
                    value={hint}
                    rows={2}
                    onChange={(e) => handleUpdateHint(index, e.target.value)}
                    placeholder={placeholder}
                    className="min-h-16 bg-background font-mono text-xs"
                  />
                </div>
              )
            })}
          </div>
        )}
      </div>
    </FormBase>
  )
}

export default FormHints
