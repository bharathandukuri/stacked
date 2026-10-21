import { useState } from "react"
import { Edit3, Columns, Eye } from "lucide-react"
import { cn } from "@/lib/utils"
import { MarkdownEditor } from "@/components/markdown/markdown-editor"
import { MarkdownPreview } from "@/components/markdown-preview/markdown-preview"
import { SplitView } from "@/components/split-view"
import type {
  MarkdownEditorValue,
  MarkdownViewMode,
  ToolbarConfig,
} from "@/components/markdown/types"

export interface QuestionBankMarkdownFieldProps {
  id?: string
  label?: string
  description?: string
  required?: boolean
  value: string
  onChange: (content: string) => void
  placeholder?: string
  height?: number | string
  minHeight?: number | string
  defaultViewMode?: MarkdownViewMode
  showViewSwitcher?: boolean
  toolbar?: boolean | ToolbarConfig
  className?: string
}

export function QuestionBankMarkdownField({
  id,
  label,
  description,
  required = false,
  value,
  onChange,
  placeholder = "Write a comprehensive description, syllabus details, candidate instructions, or rubric for this question bank...",
  height = 280,
  minHeight = 200,
  defaultViewMode = "edit",
  showViewSwitcher = true,
  toolbar = "all",
  className,
}: QuestionBankMarkdownFieldProps) {
  const [viewMode, setViewMode] = useState<MarkdownViewMode>(defaultViewMode)

  const effectiveHeight = typeof height === "number" ? `${height}px` : height
  const effectiveMinHeight =
    typeof minHeight === "number" ? `${minHeight}px` : minHeight

  const handleChange = (val: MarkdownEditorValue) => {
    onChange(val.content)
  }

  return (
    <div className={cn("space-y-2", className)}>
      <div className="flex items-center justify-between">
        <div>
          {label && (
            <label
              htmlFor={id}
              className="text-sm font-semibold tracking-tight text-foreground"
            >
              {label}
              {required && <span className="text-destructive ml-1">*</span>}
            </label>
          )}
          {description && (
            <p className="text-xs text-muted-foreground mt-0.5">{description}</p>
          )}
        </div>

        {showViewSwitcher && (
          <div className="flex items-center rounded-lg border border-border/70 bg-muted/40 p-0.5 text-xs shadow-2xs">
            <button
              type="button"
              onClick={() => setViewMode("edit")}
              className={cn(
                "flex items-center gap-1.5 rounded-md px-2.5 py-1 font-medium transition-all",
                viewMode === "edit"
                  ? "bg-background text-foreground shadow-xs ring-1 ring-border/50"
                  : "text-muted-foreground hover:text-foreground"
              )}
              title="Editor Only"
            >
              <Edit3 className="size-3.5" />
              <span>Edit</span>
            </button>
            <button
              type="button"
              onClick={() => setViewMode("split")}
              className={cn(
                "flex items-center gap-1.5 rounded-md px-2.5 py-1 font-medium transition-all",
                viewMode === "split"
                  ? "bg-background text-foreground shadow-xs ring-1 ring-border/50"
                  : "text-muted-foreground hover:text-foreground"
              )}
              title="Split View (Editor + Live Preview)"
            >
              <Columns className="size-3.5" />
              <span>Split</span>
            </button>
            <button
              type="button"
              onClick={() => setViewMode("preview")}
              className={cn(
                "flex items-center gap-1.5 rounded-md px-2.5 py-1 font-medium transition-all",
                viewMode === "preview"
                  ? "bg-background text-foreground shadow-xs ring-1 ring-border/50"
                  : "text-muted-foreground hover:text-foreground"
              )}
              title="Preview Only"
            >
              <Eye className="size-3.5" />
              <span>Preview</span>
            </button>
          </div>
        )}
      </div>

      {viewMode === "edit" && (
        <div
          style={{
            height: effectiveHeight,
            minHeight: effectiveMinHeight,
            maxHeight: effectiveHeight,
          }}
          className="w-full overflow-hidden rounded-lg border border-border bg-card shadow-xs"
        >
          <MarkdownEditor
            id={id}
            value={value}
            onChange={handleChange}
            height="100%"
            minHeight="100%"
            placeholder={placeholder}
            toolbar={toolbar}
            showSessionImages={false}
            className="h-full rounded-none border-0 shadow-none"
          />
        </div>
      )}

      {viewMode === "preview" && (
        <div
          style={{
            height: effectiveHeight,
            minHeight: effectiveMinHeight,
            maxHeight: effectiveHeight,
          }}
          className="w-full overflow-y-auto rounded-lg border border-border bg-card p-4 shadow-xs"
        >
          <MarkdownPreview
            content={value}
            emptyMessage="No description written yet."
          />
        </div>
      )}

      {viewMode === "split" && (
        <div
          style={{
            height: effectiveHeight,
            minHeight: effectiveMinHeight,
            maxHeight: effectiveHeight,
          }}
          className="w-full overflow-hidden rounded-lg border border-border bg-card shadow-xs"
        >
          <SplitView
            firstPane={
              <MarkdownEditor
                id={id}
                value={value}
                onChange={handleChange}
                height="100%"
                minHeight="100%"
                placeholder={placeholder}
                toolbar={toolbar}
                showSessionImages={false}
                className="h-full rounded-none border-0 shadow-none"
              />
            }
            secondPane={
              <div className="h-full min-h-0 w-full overflow-y-auto bg-background/50 p-4">
                <MarkdownPreview
                  content={value}
                  emptyMessage="Live preview will appear here as you type."
                />
              </div>
            }
            defaultSize={50}
            minSize={25}
            maxSize={75}
            className="h-full w-full max-h-full min-h-0"
          />
        </div>
      )}
    </div>
  )
}

