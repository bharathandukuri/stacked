import { type ReactNode } from "react"
import { FormBase, type FormControlProps } from "@/components/form/form-base"
import { useFieldContext } from "@/hooks/form/create-form-hooks"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { cn } from "@/lib/utils"

export interface FormSelectOption {
  value: string
  label: ReactNode
  description?: string
  disabled?: boolean
}

export interface FormSelectProps extends FormControlProps {
  options?: FormSelectOption[]
  children?: ReactNode
  placeholder?: string
  className?: string
  triggerClassName?: string
  contentClassName?: string
  size?: "sm" | "default"
  disabled?: boolean
}

export function FormSelect({
  options,
  children,
  placeholder = "Select an option",
  className,
  triggerClassName,
  contentClassName,
  size = "default",
  disabled,
  ...controlProps
}: FormSelectProps) {
  const field = useFieldContext<string>()
  const isInvalid = field.state.meta.isTouched && !field.state.meta.isValid
  const value = field.state.value || null

  // Map options to Record<string, ReactNode> so Base UI SelectValue automatically renders the label
  const itemsRecord = options
    ? options.reduce<Record<string, ReactNode>>((acc, opt) => {
        acc[opt.value] = opt.label
        return acc
      }, {})
    : undefined

  return (
    <FormBase {...controlProps}>
      <div className={cn("w-full", className)}>
        <Select
          value={value}
          items={itemsRecord}
          disabled={disabled}
          onValueChange={(val) => {
            field.handleChange(val ?? "")
          }}
        >
          <SelectTrigger
            id={field.name}
            className={cn("w-full justify-between", triggerClassName)}
            size={size}
            aria-invalid={isInvalid}
            onBlur={field.handleBlur}
          >
            <SelectValue placeholder={placeholder} />
          </SelectTrigger>

          <SelectContent className={contentClassName}>
            {options
              ? options.map((opt) => (
                  <SelectItem
                    key={opt.value}
                    value={opt.value}
                    disabled={opt.disabled}
                  >
                    <div className="flex flex-col">
                      <span>{opt.label}</span>
                      {opt.description && (
                        <span className="text-xs text-muted-foreground">
                          {opt.description}
                        </span>
                      )}
                    </div>
                  </SelectItem>
                ))
              : children}
          </SelectContent>
        </Select>
      </div>
    </FormBase>
  )
}

export default FormSelect
