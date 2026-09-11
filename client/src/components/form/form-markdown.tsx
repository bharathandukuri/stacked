import { useState } from "react"
import { Edit3, Columns, Eye } from "lucide-react"
import { FormBase, type FormControlProps } from "@/components/form/form-base"
import { useFieldContext } from "@/hooks/form/create-form-hooks"
import {
  MarkdownEditor,
  type MarkdownEditorValue,
  type MarkdownViewMode,
  type ToolbarConfig,
} from "@/components/markdown"
import { MarkdownPreview } from "@/components/markdown-preview"
import { SplitView } from "@/components/split-view"
import { cn } from "@/lib/utils"

export interface FormMarkdownProps extends FormControlProps {
  placeholder?: string
  height?: string | number
  minHeight?: string | number
  className?: string
  readOnly?: boolean
  defaultViewMode?: MarkdownViewMode
  toolbar?: boolean | ToolbarConfig
}

export function FormMarkdown(props: FormMarkdownProps) {
  const field = useFieldContext<string | MarkdownEditorValue>()
  const [viewMode, setViewMode] = useState<MarkdownViewMode>(
    props.defaultViewMode ?? "split"
  )

  const value = field.state.value ?? ""
  const content = typeof value === "string" ? value : (value.content ?? "")

  const handleChange = (val: MarkdownEditorValue) => {
    // If the field was initialized with string or is undefined, store as string
    if (
      typeof field.state.value === "string" ||
      field.state.value === undefined
    ) {
      field.handleChange(val.content)
    } else {
      field.handleChange(val)
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
            ? "bg-background text-foreground shadow-xs"
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
            ? "bg-background text-foreground shadow-xs"
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

  const height = props.height ?? 420
  const minHeight = props.minHeight ?? 240

  return (
    <FormBase {...props} labelAction={props.labelAction ?? modeSwitcher}>
      {viewMode === "edit" && (
        <MarkdownEditor
          id={field.name}
          value={value}
          onChange={handleChange}
          onBlur={field.handleBlur}
          height={height}
          minHeight={minHeight}
          placeholder={props.placeholder}
          readOnly={props.readOnly}
          toolbar={props.toolbar}
          className={props.className}
        />
      )}

      {viewMode === "preview" && (
        <div
          style={{ height, minHeight }}
          className={cn(
            "w-full overflow-hidden rounded-lg border border-border bg-card p-4 shadow-xs",
            props.className
          )}
        >
          <MarkdownPreview content={content} />
        </div>
      )}

      {viewMode === "split" && (
        <div
          style={{ height, minHeight }}
          className={cn(
            "w-full overflow-hidden rounded-lg border border-border bg-card shadow-xs",
            props.className
          )}
        >
          <SplitView
            firstPane={
              <MarkdownEditor
                id={field.name}
                value={value}
                onChange={handleChange}
                onBlur={field.handleBlur}
                height="100%"
                placeholder={props.placeholder}
                readOnly={props.readOnly}
                toolbar={props.toolbar}
                className="rounded-none border-0 shadow-none"
              />
            }
            secondPane={
              <div className="h-full min-h-0 w-full overflow-hidden bg-background p-4">
                <MarkdownPreview content={content} />
              </div>
            }
            defaultSize={50}
            minSize={20}
            maxSize={80}
            className="h-full w-full"
          />
        </div>
      )}
    </FormBase>
  )
}

export default FormMarkdown
