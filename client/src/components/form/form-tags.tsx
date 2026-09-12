import * as React from "react"
import { X, Plus } from "lucide-react"
import { FormBase, type FormControlProps } from "@/components/form/form-base"
import { useFieldContext } from "@/hooks/form/create-form-hooks"
import { Badge } from "@/components/ui/badge"
import { Input } from "@/components/ui/input"
import { cn } from "@/lib/utils"

export interface FormTagsProps extends FormControlProps {
  placeholder?: string
  popularOptions?: readonly string[]
  className?: string
}

export function FormTags({
  placeholder = "Type a topic and press Enter...",
  popularOptions = [],
  className,
  ...controlProps
}: FormTagsProps) {
  const field = useFieldContext<string[]>()
  const selectedTags = field.state.value ?? []

  const [inputValue, setInputValue] = React.useState("")
  const inputRef = React.useRef<HTMLInputElement>(null)

  const handleAddTag = (rawTag: string) => {
    const cleanTag = rawTag.trim()
    if (!cleanTag) return

    const exists = selectedTags.some(
      (t) => t.toLowerCase() === cleanTag.toLowerCase()
    )
    if (!exists) {
      field.handleChange([...selectedTags, cleanTag])
    }
    setInputValue("")
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter" || e.key === ",") {
      e.preventDefault()
      handleAddTag(inputValue)
    } else if (e.key === "Backspace" && !inputValue && selectedTags.length > 0) {
      e.preventDefault()
      field.handleChange(selectedTags.slice(0, -1))
    }
  }

  const handleRemoveTag = (tagToRemove: string) => {
    field.handleChange(selectedTags.filter((t) => t !== tagToRemove))
  }

  const availablePopular = popularOptions.filter(
    (opt) => !selectedTags.some((t) => t.toLowerCase() === opt.toLowerCase())
  )

  return (
    <FormBase
      {...controlProps}
      labelAction={
        selectedTags.length > 0 ? (
          <span className="font-mono text-[11px] text-muted-foreground">
            {selectedTags.length} selected
          </span>
        ) : undefined
      }
    >
      <div className={cn("space-y-2.5", className)}>
        {/* Input box with inline badges */}
        <div
          onClick={() => inputRef.current?.focus()}
          className="flex min-h-10 w-full flex-wrap items-center gap-1.5 rounded-lg border border-border/80 bg-card p-1.5 transition-colors focus-within:border-ring focus-within:ring-2 focus-within:ring-ring/20 cursor-text"
        >
          {selectedTags.map((tag) => (
            <Badge
              key={tag}
              variant="secondary"
              className="flex items-center gap-1 bg-muted px-2 py-0.5 text-xs font-medium text-foreground transition-all"
            >
              <span>{tag}</span>
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation()
                  handleRemoveTag(tag)
                }}
                className="cursor-pointer rounded-full p-0.5 text-muted-foreground hover:bg-foreground/10 hover:text-foreground"
                aria-label={`Remove ${tag}`}
              >
                <X className="size-3" />
              </button>
            </Badge>
          ))}

          <Input
            ref={inputRef}
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            onKeyDown={handleKeyDown}
            onBlur={() => {
              if (inputValue.trim()) {
                handleAddTag(inputValue)
              }
            }}
            placeholder={
              selectedTags.length === 0 ? placeholder : "Add another topic..."
            }
            className="h-7 min-w-36 flex-1 border-0 bg-transparent px-1 text-xs shadow-none focus-visible:ring-0"
          />
        </div>

        {/* Popular Quick Suggestions */}
        {availablePopular.length > 0 && (
          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <span className="text-[10px] font-semibold tracking-wider text-muted-foreground uppercase">
                Popular topics:
              </span>
              <span className="text-[10px] text-muted-foreground">Click to add</span>
            </div>
            <div className="flex flex-wrap gap-1.5">
              {availablePopular.slice(0, 12).map((suggestion) => (
                <button
                  key={suggestion}
                  type="button"
                  onClick={() => handleAddTag(suggestion)}
                  className="inline-flex cursor-pointer items-center gap-1 rounded-md border border-border/70 bg-card px-2 py-0.5 text-[11px] font-medium text-muted-foreground transition-all hover:border-primary/40 hover:bg-primary/10 hover:text-primary"
                >
                  <Plus className="size-2.5" />
                  <span>{suggestion}</span>
                </button>
              ))}
            </div>
          </div>
        )}
      </div>
    </FormBase>
  )
}

export default FormTags
