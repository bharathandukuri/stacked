import { type ReactNode } from "react"
import { useFieldContext } from "@/hooks/form/create-form-hooks"
import { cn } from "@/lib/utils"
import {
  Field,
  FieldContent,
  FieldDescription,
  FieldError,
  FieldLabel,
} from "@/components/ui/field"

export type FormControlProps = {
  label: string
  description?: string
  required?: boolean
  labelAction?: ReactNode
  validator?: ReactNode
  className?: string
}

export type FormBaseProps = FormControlProps & {
  children: ReactNode
  horizontal?: boolean
  controlFirst?: boolean
  className?: string
}

export function FormBase({
  children,
  label,
  description,
  required,
  labelAction,
  validator,
  controlFirst,
  horizontal,
  className,
}: FormBaseProps) {
  const field = useFieldContext()
  const isInvalid = field.state.meta.isTouched && !field.state.meta.isValid

  return (
    <Field
      data-invalid={isInvalid}
      orientation={horizontal ? "horizontal" : undefined}
      className={cn("min-w-0 max-w-full", className)}
    >
      {controlFirst ? (
        <>
          {children}

          <FieldContent>
            <div className="flex items-center justify-between gap-2">
              <FieldLabel htmlFor={field.name}>
                {label}
                {required && <span className="ml-1 text-destructive">*</span>}
              </FieldLabel>

              {labelAction}
            </div>

            {description && <FieldDescription>{description}</FieldDescription>}

            {validator}

            {isInvalid && <FieldError errors={field.state.meta.errors} />}
          </FieldContent>
        </>
      ) : (
        <>
          <FieldContent>
            <div className="flex items-center justify-between gap-2">
              <FieldLabel htmlFor={field.name}>
                {label}
                {required && <span className="ml-1 text-destructive">*</span>}
              </FieldLabel>

              {labelAction}
            </div>

            {description && <FieldDescription>{description}</FieldDescription>}
          </FieldContent>

          {children}

          {validator}

          {isInvalid && <FieldError errors={field.state.meta.errors} />}
        </>
      )}
    </Field>
  )
}

export default FormBase
