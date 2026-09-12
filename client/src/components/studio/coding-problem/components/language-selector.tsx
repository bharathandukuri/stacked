import { Check, CheckSquare, Square } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { FormBase, type FormControlProps } from "@/components/form/form-base"
import { cn } from "@/lib/utils"
import type { LanguageOption } from "../types"

export interface LanguageSelectorProps extends FormControlProps {
  options: readonly LanguageOption[]
  value: string[]
  onChange: (value: string[]) => void
  className?: string
}

export function LanguageSelector({
  options,
  value,
  onChange,
  className,
  ...formBaseProps
}: LanguageSelectorProps) {
  const allIds = options.map((o) => o.id)
  const isAllSelected =
    options.length > 0 && options.every((o) => value.includes(o.id))

  const handleToggle = (id: string) => {
    if (value.includes(id)) {
      onChange(value.filter((v) => v !== id))
    } else {
      onChange([...value, id])
    }
  }

  const handleSelectAll = () => {
    onChange([...allIds])
  }

  const handleClearAll = () => {
    onChange([])
  }

  return (
    <FormBase
      {...formBaseProps}
      labelAction={
        <div className="flex items-center gap-2">
          <Badge
            variant="secondary"
            className="h-5 px-1.5 font-mono text-[10px] text-muted-foreground"
          >
            {value.length} / {options.length} Enabled
          </Badge>
          <Button
            type="button"
            variant="outline"
            size="xs"
            onClick={isAllSelected ? handleClearAll : handleSelectAll}
            className="h-5.5 cursor-pointer gap-1 px-2 text-[11px] text-primary"
          >
            {isAllSelected ? (
              <>
                <Square className="size-3" />
                <span>Deselect All</span>
              </>
            ) : (
              <>
                <CheckSquare className="size-3" />
                <span>Select All</span>
              </>
            )}
          </Button>
        </div>
      }
    >
      <div
        className={cn(
          "grid grid-cols-2 gap-2.5 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5",
          className
        )}
      >
        {options.map((option) => {
          const isSelected = value.includes(option.id)

          return (
            <button
              key={option.id}
              type="button"
              onClick={() => handleToggle(option.id)}
              className={cn(
                "group flex cursor-pointer items-center justify-between gap-2 rounded-xl border p-3 text-left transition-all duration-150",
                isSelected
                  ? "border-primary/50 bg-primary/10 font-semibold text-foreground shadow-xs ring-2 ring-primary/20"
                  : "border-border/80 bg-card text-muted-foreground hover:border-border hover:bg-muted/40 hover:text-foreground"
              )}
            >
              <div className="flex min-w-0 flex-1 items-center gap-2">
                <div
                  className={cn(
                    "flex size-4 shrink-0 items-center justify-center rounded-sm border transition-colors",
                    isSelected
                      ? "border-primary bg-primary text-primary-foreground"
                      : "border-muted-foreground/40 bg-transparent group-hover:border-muted-foreground"
                  )}
                >
                  {isSelected && <Check className="size-3 stroke-3" />}
                </div>

                <span
                  className={cn(
                    "truncate text-xs font-semibold tracking-tight",
                    isSelected
                      ? "text-foreground"
                      : "text-muted-foreground group-hover:text-foreground"
                  )}
                >
                  {option.name}
                </span>
              </div>
            </button>
          )
        })}
      </div>
    </FormBase>
  )
}

export default LanguageSelector
