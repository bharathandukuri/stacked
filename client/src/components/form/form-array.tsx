import { type ReactNode } from "react"
import { Plus, Trash2 } from "lucide-react"
import { FormBase, type FormControlProps } from "@/components/form/form-base"
import { useFieldContext } from "@/hooks/form/create-form-hooks"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { cn } from "@/lib/utils"

export interface FormArrayProps extends FormControlProps {
  placeholder?: string
  addLabel?: string
  emptyMessage?: string
  minItems?: number
  maxItems?: number
  className?: string
  renderItem?: (
    item: string,
    index: number,
    onChange: (val: string) => void,
    onRemove: () => void,
    isOnly: boolean
  ) => ReactNode
}

export function FormArray({
  placeholder = "Enter value...",
  addLabel = "Add Item",
  emptyMessage = "No items yet. Click add to create one.",
  minItems = 0,
  maxItems,
  className,
  renderItem,
  ...controlProps
}: FormArrayProps) {
  const field = useFieldContext<string[]>()
  const items = field.state.value ?? []

  const handleAddItem = (initialValue = "") => {
    if (maxItems !== undefined && items.length >= maxItems) return
    field.handleChange([...items, initialValue])
  }

  const handleUpdateItem = (index: number, value: string) => {
    const next = [...items]
    next[index] = value
    field.handleChange(next)
  }

  const handleRemoveItem = (index: number) => {
    if (items.length <= minItems) return
    field.handleChange(items.filter((_, i) => i !== index))
  }

  const isAtMax = maxItems !== undefined && items.length >= maxItems

  return (
    <FormBase
      {...controlProps}
      labelAction={
        controlProps.labelAction || (
          <Button
            type="button"
            variant="outline"
            size="xs"
            disabled={isAtMax}
            onClick={() => handleAddItem()}
            className="h-5.5 cursor-pointer gap-1 px-2 text-[11px] text-primary"
          >
            <Plus className="size-3" /> {addLabel}
          </Button>
        )
      }
    >
      <div className={cn("space-y-2", className)}>
        {items.length === 0 ? (
          <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border/80 bg-muted/20 p-6 text-center text-xs text-muted-foreground">
            <p>{emptyMessage}</p>
            <Button
              type="button"
              variant="secondary"
              size="xs"
              onClick={() => handleAddItem()}
              className="mt-2.5 cursor-pointer gap-1 text-xs"
            >
              <Plus className="size-3" />
              <span>{addLabel}</span>
            </Button>
          </div>
        ) : (
          <div className="space-y-2">
            {items.map((item, index) => {
              if (renderItem) {
                return renderItem(
                  item,
                  index,
                  (val) => handleUpdateItem(index, val),
                  () => handleRemoveItem(index),
                  items.length <= minItems
                )
              }

              return (
                <div
                  key={index}
                  className="flex items-center gap-2 rounded-lg border border-border/70 bg-card p-2 shadow-2xs"
                >
                  <span className="flex size-5 shrink-0 items-center justify-center rounded-full bg-muted font-mono text-[10px] font-semibold text-muted-foreground">
                    {index + 1}
                  </span>

                  <Input
                    value={item}
                    onChange={(e) => handleUpdateItem(index, e.target.value)}
                    placeholder={placeholder}
                    className="h-8 flex-1 text-xs"
                  />

                  <Button
                    type="button"
                    variant="ghost"
                    size="icon-xs"
                    disabled={items.length <= minItems}
                    onClick={() => handleRemoveItem(index)}
                    className="shrink-0 text-muted-foreground hover:text-destructive cursor-pointer disabled:opacity-30"
                    title="Remove item"
                  >
                    <Trash2 className="size-3.5" />
                  </Button>
                </div>
              )
            })}
          </div>
        )}
      </div>
    </FormBase>
  )
}

export default FormArray
