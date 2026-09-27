import * as React from "react"
import { X, Plus, Check } from "lucide-react"
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
  placeholder = "Type topic and press Enter...",
  popularOptions = [],
  className,
  ...controlProps
}: FormTagsProps) {
  const field = useFieldContext<string[]>()
  const selectedTags = field.state.value ?? []

  const [inputValue, setInputValue] = React.useState("")
  const inputRef = React.useRef<HTMLInputElement>(null)

  const isSelected = (tag: string) =>
    selectedTags.some((t) => t.toLowerCase() === tag.toLowerCase())

  const handleAddTag = (rawTag: string) => {
    const cleanTag = rawTag.trim()
    if (!cleanTag) return

    if (!isSelected(cleanTag)) {
      field.handleChange([...selectedTags, cleanTag])
    }
    setInputValue("")
  }

  const handleRemoveTag = (tagToRemove: string) => {
    field.handleChange(
      selectedTags.filter((t) => t.toLowerCase() !== tagToRemove.toLowerCase())
    )
  }

  const handleToggleTag = (tag: string) => {
    if (isSelected(tag)) {
      handleRemoveTag(tag)
    } else {
      handleAddTag(tag)
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter" || e.key === ",") {
      e.preventDefault()
      handleAddTag(inputValue)
    } else if (
      e.key === "Backspace" &&
      !inputValue &&
      selectedTags.length > 0
    ) {
      e.preventDefault()
      field.handleChange(selectedTags.slice(0, -1))
    }
  }

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
        {/* Inline Badges & Input Box */}
        <div
          onClick={() => inputRef.current?.focus()}
          className="flex min-h-10 w-full cursor-text flex-wrap items-center gap-1.5 rounded-lg border border-border/80 bg-background p-1.5 shadow-2xs transition-colors focus-within:border-ring focus-within:ring-2 focus-within:ring-ring/20"
        >
          {selectedTags.map((tag) => (
            <Badge
              key={tag}
              variant="secondary"
              className="flex items-center gap-1 border border-primary/20 bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary"
            >
              <span>{tag}</span>
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation()
                  handleRemoveTag(tag)
                }}
                className="cursor-pointer rounded-full p-0.5 text-primary/70 transition-colors hover:bg-primary/20 hover:text-primary"
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
              selectedTags.length === 0 ? placeholder : "Add more..."
            }
            className="h-7 min-w-32 flex-1 border-0 bg-transparent px-1 text-xs shadow-none focus-visible:ring-0"
          />
        </div>

        {/* Popular Quick-Select Tags */}
        {popularOptions.length > 0 && (
          <div className="flex flex-wrap items-center gap-1.5 pt-0.5">
            <span className="mr-1 text-[11px] text-muted-foreground">
              Suggested:
            </span>
            {popularOptions.map((tag) => {
              const active = isSelected(tag)
              return (
                <button
                  key={tag}
                  type="button"
                  onClick={() => handleToggleTag(tag)}
                  className={cn(
                    "inline-flex cursor-pointer items-center gap-1 rounded-md border px-2 py-0.5 text-[11px] font-medium transition-all select-none",
                    active
                      ? "border-primary/50 bg-primary/10 text-primary shadow-2xs"
                      : "border-border/70 bg-card text-muted-foreground hover:border-border hover:bg-muted hover:text-foreground"
                  )}
                >
                  {active ? (
                    <Check className="size-2.5 text-primary" />
                  ) : (
                    <Plus className="size-2.5 opacity-60" />
                  )}
                  <span>{tag}</span>
                </button>
              )
            })}
          </div>
        )}
      </div>
    </FormBase>
  )
}

export default FormTags
