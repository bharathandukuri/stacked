import * as React from "react"
import { X, Plus } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Input } from "@/components/ui/input"
import { cn } from "@/lib/utils"

interface TagSelectorProps {
  label: string
  description?: string
  placeholder?: string
  value: string[]
  onChange: (tags: string[]) => void
  popularOptions?: readonly string[]
  error?: string
  className?: string
}

export function TagSelector({
  label,
  description,
  placeholder = "Type and press Enter...",
  value = [],
  onChange,
  popularOptions = [],
  error,
  className,
}: TagSelectorProps) {
  const [inputValue, setInputValue] = React.useState("")
  const inputRef = React.useRef<HTMLInputElement>(null)

  const handleAddTag = (rawTag: string) => {
    const cleanTag = rawTag.trim()
    if (!cleanTag) return

    // Avoid duplicate tags (case-insensitive check)
    const exists = value.some((t) => t.toLowerCase() === cleanTag.toLowerCase())
    if (!exists) {
      onChange([...value, cleanTag])
    }
    setInputValue("")
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter" || e.key === ",") {
      e.preventDefault()
      handleAddTag(inputValue)
    } else if (e.key === "Backspace" && !inputValue && value.length > 0) {
      // Remove last tag on backspace when input is empty
      e.preventDefault()
      onChange(value.slice(0, -1))
    }
  }

  const handleRemoveTag = (tagToRemove: string) => {
    onChange(value.filter((t) => t !== tagToRemove))
  }

  // Filter popular options that aren't already selected
  const availablePopular = popularOptions.filter(
    (opt) => !value.some((t) => t.toLowerCase() === opt.toLowerCase())
  )

  return (
    <div className={cn("space-y-2", className)}>
      <div className="flex items-center justify-between">
        <label className="text-xs font-semibold text-foreground">{label}</label>
        {value.length > 0 && (
          <span className="text-[11px] text-muted-foreground">
            {value.length} selected
          </span>
        )}
      </div>

      {description && (
        <p className="text-[11px] text-muted-foreground">{description}</p>
      )}

      {/* Main Tag Container & Input Box */}
      <div
        onClick={() => inputRef.current?.focus()}
        className={cn(
          "flex min-h-10 w-full flex-wrap items-center gap-1.5 rounded-lg border border-border bg-background p-1.5 transition-colors focus-within:border-ring focus-within:ring-2 focus-within:ring-ring/20",
          error && "border-destructive focus-within:border-destructive"
        )}
      >
        {value.map((tag) => (
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
              className="rounded-full p-0.5 text-muted-foreground hover:bg-foreground/10 hover:text-foreground"
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
          placeholder={value.length === 0 ? placeholder : "Add another..."}
          className="h-7 min-w-32 flex-1 border-0 bg-transparent px-1 text-xs shadow-none focus-visible:ring-0"
        />
      </div>

      {error && <p className="text-xs text-destructive">{error}</p>}

      {/* Popular Quick Suggestions */}
      {availablePopular.length > 0 && (
        <div className="pt-1">
          <span className="mb-1.5 block text-[10px] font-semibold tracking-wider text-muted-foreground uppercase">
            Popular suggestions:
          </span>
          <div className="flex flex-wrap gap-1">
            {availablePopular.slice(0, 10).map((suggestion) => (
              <button
                key={suggestion}
                type="button"
                onClick={() => handleAddTag(suggestion)}
                className="inline-flex cursor-pointer items-center gap-1 rounded-md border border-border/70 bg-muted/30 px-2 py-0.5 text-[11px] text-muted-foreground transition-all hover:border-primary/40 hover:bg-primary/10 hover:text-primary"
              >
                <Plus className="size-2.5" />
                <span>{suggestion}</span>
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
