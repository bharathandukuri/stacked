import { useState } from "react"
import {
  Plus,
  Trash2,
  Lightbulb,
  ChevronUp,
  ChevronDown,
  Eye,
  EyeOff,
} from "lucide-react"
import { FormBase, type FormControlProps } from "@/components/form/form-base"
import { useFieldContext } from "@/hooks/form/create-form-hooks"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import { Badge } from "@/components/ui/badge"
import { cn } from "@/lib/utils"
import { MarkdownPreview } from "@/components/markdown-preview/markdown-preview"

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

  // Track which hints are currently showing markdown preview
  const [previewingIndexes, setPreviewingIndexes] = useState<Record<number, boolean>>({})

  const togglePreview = (index: number) => {
    setPreviewingIndexes((prev) => ({
      ...prev,
      [index]: !prev[index],
    }))
  }

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

    // Also swap preview states
    setPreviewingIndexes((prev) => ({
      ...prev,
      [fromIndex]: prev[toIndex],
      [toIndex]: prev[fromIndex],
    }))
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
            <Lightbulb className="size-5 text-amber-500 mb-1" />
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
              const isPreview = Boolean(previewingIndexes[index])
              const isFirst = index === 0
              const isLast = index === hints.length - 1

              return (
                <div
                  key={index}
                  className="space-y-2.5 rounded-xl border border-border/70 bg-card p-3.5 shadow-2xs"
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
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon-xs"
                        disabled={isFirst}
                        onClick={() => handleMoveHint(index, -1)}
                        className="h-6 w-6 text-muted-foreground hover:text-foreground cursor-pointer disabled:opacity-30"
                        title="Move Up"
                      >
                        <ChevronUp className="size-3.5" />
                      </Button>

                      <Button
                        type="button"
                        variant="ghost"
                        size="icon-xs"
                        disabled={isLast}
                        onClick={() => handleMoveHint(index, 1)}
                        className="h-6 w-6 text-muted-foreground hover:text-foreground cursor-pointer disabled:opacity-30"
                        title="Move Down"
                      >
                        <ChevronDown className="size-3.5" />
                      </Button>

                      {/* Markdown Preview Toggle */}
                      <Button
                        type="button"
                        variant="ghost"
                        size="xs"
                        onClick={() => togglePreview(index)}
                        className="h-6 gap-1 px-1.5 text-[11px] text-muted-foreground hover:text-foreground cursor-pointer"
                        title={isPreview ? "Edit markdown" : "Preview markdown"}
                      >
                        {isPreview ? (
                          <>
                            <EyeOff className="size-3" />
                            <span>Edit</span>
                          </>
                        ) : (
                          <>
                            <Eye className="size-3" />
                            <span>Preview</span>
                          </>
                        )}
                      </Button>

                      {/* Delete */}
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon-xs"
                        onClick={() => handleRemoveHint(index)}
                        className="h-6 w-6 text-muted-foreground hover:text-destructive cursor-pointer"
                        title={`Remove Hint ${index + 1}`}
                      >
                        <Trash2 className="size-3.5" />
                      </Button>
                    </div>
                  </div>

                  {isPreview ? (
                    <div className="min-h-16 rounded-lg border border-border/60 bg-muted/30 p-3 text-xs">
                      {hint.trim() ? (
                        <MarkdownPreview value={hint} />
                      ) : (
                        <span className="italic text-muted-foreground">
                          No content to preview.
                        </span>
                      )}
                    </div>
                  ) : (
                    <Textarea
                      value={hint}
                      rows={2}
                      onChange={(e) => handleUpdateHint(index, e.target.value)}
                      placeholder={placeholder}
                      className="min-h-16 text-xs bg-background font-mono"
                    />
                  )}
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
