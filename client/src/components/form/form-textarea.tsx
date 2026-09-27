import * as React from "react"
import { FormBase, type FormControlProps } from "@/components/form/form-base"
import { useFieldContext } from "@/hooks/form/create-form-hooks"
import { Textarea } from "@/components/ui/textarea"
import { cn } from "@/lib/utils"

export interface FormTextareaProps extends FormControlProps {
  placeholder?: string
  rows?: number
  className?: string
  textareaClassName?: string
  textareaProps?: React.ComponentProps<"textarea">
  maxLength?: number
  showCount?: boolean
  monospace?: boolean
  disabled?: boolean
  readOnly?: boolean
}

export function FormTextarea({
  placeholder,
  rows = 4,
  className,
  textareaClassName,
  textareaProps,
  maxLength,
  showCount,
  monospace,
  disabled,
  readOnly,
  ...controlProps
}: FormTextareaProps) {
  const field = useFieldContext<string>()
  const isInvalid = field.state.meta.isTouched && !field.state.meta.isValid
  const value = field.state.value ?? ""

  return (
    <FormBase {...controlProps}>
      <div className="relative w-full">
        <Textarea
          id={field.name}
          name={field.name}
          value={value}
          onBlur={field.handleBlur}
          onChange={(e) => field.handleChange(e.target.value)}
          aria-invalid={isInvalid}
          placeholder={placeholder}
          rows={rows}
          maxLength={maxLength}
          disabled={disabled}
          readOnly={readOnly}
          className={cn(
            "bg-background text-xs",
            monospace && "font-mono",
            textareaClassName,
            className
          )}
          {...textareaProps}
        />
        {showCount && maxLength && (
          <div className="mt-1 text-right font-mono text-[10px] text-muted-foreground">
            {value.length} / {maxLength}
          </div>
        )}
      </div>
    </FormBase>
  )
}

export default FormTextarea
