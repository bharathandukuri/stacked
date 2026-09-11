import { useState } from "react"
import { Eye, EyeOff, type LucideIcon } from "lucide-react"
import { FormBase, type FormControlProps } from "@/components/form/form-base"
import { useFieldContext } from "@/hooks/form/create-form-hooks"
import {
  InputGroup,
  InputGroupAddon,
  InputGroupButton,
  InputGroupInput,
} from "@/components/ui/input-group"
import { cn } from "@/lib/utils"

export interface FormTextProps extends FormControlProps {
  icon?: LucideIcon
  type?: "text" | "password" | "email" | "url"
  placeholder?: string
  className?: string
  inputClassName?: string
  inputProps?: React.InputHTMLAttributes<HTMLInputElement>
}

export function FormText(props: FormTextProps) {
  const field = useFieldContext<string>()
  const isInvalid = field.state.meta.isTouched && !field.state.meta.isValid

  const [show, setShow] = useState(false)

  return (
    <FormBase {...props}>
      <InputGroup className={cn("rounded-md", props.className)}>
        {props.icon && (
          <InputGroupAddon>
            <props.icon size={18} />
          </InputGroupAddon>
        )}

        <InputGroupInput
          id={field.name}
          name={field.name}
          value={field.state.value ?? ""}
          onBlur={field.handleBlur}
          onChange={(e) => field.handleChange(e.target.value)}
          aria-invalid={isInvalid}
          placeholder={props.placeholder}
          type={
            props.type === "password"
              ? show
                ? "text"
                : "password"
              : (props.type ?? "text")
          }
          className={cn(props.inputClassName)}
          {...props.inputProps}
        />

        {props.type === "password" && (
          <InputGroupButton onClick={() => setShow(!show)}>
            {show ? <EyeOff size={18} /> : <Eye size={18} />}
          </InputGroupButton>
        )}
      </InputGroup>
    </FormBase>
  )
}

export default FormText
