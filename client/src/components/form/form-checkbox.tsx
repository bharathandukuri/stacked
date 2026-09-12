import { type ReactNode } from "react"
import { Check, CheckSquare, Square, type LucideIcon } from "lucide-react"
import { FormBase, type FormControlProps } from "@/components/form/form-base"
import { useFieldContext } from "@/hooks/form/create-form-hooks"
import { Checkbox } from "@/components/ui/checkbox"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

/* ==========================================================================
   Single Checkbox Component (boolean)
   ========================================================================== */

export interface FormCheckboxProps extends FormControlProps {
  checkboxLabel?: ReactNode
  className?: string
  disabled?: boolean
}

export function FormCheckbox({
  checkboxLabel,
  className,
  disabled,
  ...controlProps
}: FormCheckboxProps) {
  const field = useFieldContext<boolean>()
  const checked = Boolean(field.state.value)

  return (
    <FormBase {...controlProps}>
      <label
        htmlFor={field.name}
        className={cn(
          "flex cursor-pointer items-center gap-2.5 rounded-lg border border-border/80 bg-card p-3 text-sm select-none transition-colors hover:bg-muted/40",
          disabled && "cursor-not-allowed opacity-50",
          className
        )}
      >
        <Checkbox
          id={field.name}
          disabled={disabled}
          checked={checked}
          onCheckedChange={(val) => field.handleChange(Boolean(val))}
        />
        {checkboxLabel && (
          <span className="text-xs font-medium text-foreground">
            {checkboxLabel}
          </span>
        )}
      </label>
    </FormBase>
  )
}

/* ==========================================================================
   Checkbox Group Component (string[])
   ========================================================================== */

export interface FormCheckboxOption {
  value: string
  label: ReactNode
  description?: string
  disabled?: boolean
  badge?: ReactNode
  icon?: LucideIcon
}

export interface FormCheckboxGroupProps extends FormControlProps {
  options: readonly FormCheckboxOption[]
  className?: string
  gridClassName?: string
  card?: boolean
  showSelectAll?: boolean
}

export function FormCheckboxGroup({
  options,
  className,
  gridClassName,
  card = true,
  showSelectAll = true,
  ...controlProps
}: FormCheckboxGroupProps) {
  const field = useFieldContext<string[]>()
  const selectedValues = field.state.value ?? []

  const allValues = options.map((o) => o.value)
  const isAllSelected =
    options.length > 0 && options.every((o) => selectedValues.includes(o.value))

  const handleToggle = (val: string) => {
    if (selectedValues.includes(val)) {
      field.handleChange(selectedValues.filter((v) => v !== val))
    } else {
      field.handleChange([...selectedValues, val])
    }
  }

  const handleSelectAll = () => {
    field.handleChange([...allValues])
  }

  const handleClearAll = () => {
    field.handleChange([])
  }

  const selectAllAction = showSelectAll ? (
    <div className="flex items-center gap-2">
      <Badge
        variant="secondary"
        className="h-5 px-1.5 font-mono text-[10px] text-muted-foreground"
      >
        {selectedValues.length} / {options.length} Selected
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
  ) : undefined

  return (
    <FormBase
      {...controlProps}
      labelAction={controlProps.labelAction || selectAllAction}
    >
      <div
        className={cn(
          card
            ? "grid grid-cols-2 gap-2.5 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5"
            : "flex flex-col gap-2",
          gridClassName,
          className
        )}
      >
        {options.map((option) => {
          const isSelected = selectedValues.includes(option.value)
          const Icon = option.icon

          if (card) {
            return (
              <button
                key={option.value}
                type="button"
                disabled={option.disabled}
                onClick={() => handleToggle(option.value)}
                className={cn(
                  "group flex cursor-pointer items-center justify-between gap-2 rounded-xl border p-3 text-left transition-all duration-150",
                  isSelected
                    ? "border-primary/50 bg-primary/10 text-foreground ring-2 ring-primary/20 shadow-xs font-semibold"
                    : "border-border/80 bg-card text-muted-foreground hover:border-border hover:bg-muted/40 hover:text-foreground",
                  option.disabled && "cursor-not-allowed opacity-50"
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
                    {isSelected && <Check className="size-3 stroke-[3]" />}
                  </div>

                  {Icon && (
                    <Icon className="size-3.5 shrink-0 text-muted-foreground group-hover:text-foreground" />
                  )}

                  <div className="min-w-0 flex-1">
                    <div className="truncate text-xs font-semibold tracking-tight">
                      {option.label}
                    </div>
                    {option.description && (
                      <div className="truncate text-[10px] text-muted-foreground">
                        {option.description}
                      </div>
                    )}
                  </div>
                </div>

                {option.badge && (
                  <span className="shrink-0">{option.badge}</span>
                )}
              </button>
            )
          }

          return (
            <label
              key={option.value}
              className={cn(
                "flex cursor-pointer items-center gap-2.5 rounded-lg border border-border/70 bg-card p-2.5 text-xs transition-colors hover:bg-muted/30 select-none",
                isSelected && "border-primary/40 bg-primary/5",
                option.disabled && "cursor-not-allowed opacity-50"
              )}
            >
              <Checkbox
                disabled={option.disabled}
                checked={isSelected}
                onCheckedChange={() => handleToggle(option.value)}
              />
              <div className="flex-1">
                <span className="font-medium text-foreground">
                  {option.label}
                </span>
                {option.description && (
                  <p className="text-[11px] text-muted-foreground">
                    {option.description}
                  </p>
                )}
              </div>
            </label>
          )
        })}
      </div>
    </FormBase>
  )
}

export default FormCheckbox
