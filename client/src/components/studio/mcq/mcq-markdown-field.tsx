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

export interface McqMarkdownFieldProps {
  id?: string
  label?: string
  description?: string
  required?: boolean
  value: string
  onChange: (content: string) => void
  onImageIdsChange?: (imageIds: string[]) => void
  placeholder?: string
  height?: number | string
  minHeight?: number | string
  defaultViewMode?: MarkdownViewMode
  showViewSwitcher?: boolean
  toolbar?: boolean | ToolbarConfig
  className?: string
  hideContainerBorder?: boolean
}

export function McqMarkdownField({
  id,
  label,
  description,
  required = false,
  value,
  onChange,
  onImageIdsChange,
  placeholder = "Write your markdown content here...",
  height = 360,
  minHeight = 220,
  defaultViewMode = "edit",
  showViewSwitcher = true,
  toolbar = "all",
  className,
  hideContainerBorder = false,
}: McqMarkdownFieldProps) {
  const [viewMode, setViewMode] = useState<MarkdownViewMode>(defaultViewMode)

  const effectiveHeight = typeof height === "number" ? `${height}px` : height
  const effectiveMinHeight =
    typeof minHeight === "number" ? `${minHeight}px` : minHeight

  const handleChange = (val: MarkdownEditorValue) => {
    onChange(val.content)
    if (onImageIdsChange && val.uploadedImages) {
      onImageIdsChange(val.uploadedImages.map((img) => img.fileId))
    }
  }

  const modeSwitcher = (
    <div className="flex items-center gap-0.5 rounded-lg border border-border/80 bg-muted/50 p-0.5 select-none">
      <button
        type="button"
        onClick={() => setViewMode("edit")}
        className={cn(
          "flex cursor-pointer items-center gap-1 rounded px-2 py-0.5 text-[11px] font-medium transition-all",
          viewMode === "edit"
            ? "bg-card text-foreground shadow-xs"
            : "text-muted-foreground hover:text-foreground"
        )}
      >
        <Edit3 className="size-3" />
        <span>Edit</span>
      </button>
      <button
        type="button"
        onClick={() => setViewMode("split")}
        className={cn(
          "flex cursor-pointer items-center gap-1 rounded px-2 py-0.5 text-[11px] font-medium transition-all",
          viewMode === "split"
            ? "bg-card text-foreground shadow-xs"
            : "text-muted-foreground hover:text-foreground"
        )}
      >
        <Columns className="size-3" />
        <span>Split</span>
      </button>
      <button
        type="button"
        onClick={() => setViewMode("preview")}
        className={cn(
          "flex cursor-pointer items-center gap-1 rounded px-2 py-0.5 text-[11px] font-medium transition-all",
          viewMode === "preview"
            ? "bg-primary text-primary-foreground shadow-xs"
            : "text-muted-foreground hover:text-foreground"
        )}
      >
        <Eye className="size-3" />
        <span>Preview</span>
      </button>
    </div>
  )

  const editorContent = (
    <>
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
            emptyMessage="No markdown content written yet."
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
    </>
  )

  if (hideContainerBorder) {
    return (
      <div className={cn("space-y-1.5 w-full", className)}>
        {(label || showViewSwitcher) && (
          <div className="flex items-center justify-between">
            {label ? (
              <label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                {label} {required && <span className="text-destructive">*</span>}
              </label>
            ) : <div />}
            {showViewSwitcher && modeSwitcher}
          </div>
        )}
        {editorContent}
      </div>
    )
  }

  return (
    <div className={cn("space-y-3 rounded-xl border border-border/70 bg-card p-5", className)}>
      <div className="flex items-center justify-between">
        {label && (
          <div>
            <label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              {label} {required && <span className="text-destructive">*</span>}
            </label>
            {description && (
              <p className="text-[11px] text-muted-foreground mt-0.5">{description}</p>
            )}
          </div>
        )}
        {showViewSwitcher && modeSwitcher}
      </div>
      {editorContent}
    </div>
  )
}
