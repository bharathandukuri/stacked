import type * as React from "react"

export interface UploadedImage {
  fileId: string
  url: string
  originalFileName: string
  size: number
  contentType: string
  uploadedAt: string
  isPermanent: boolean
}

export interface MarkdownEditorValue {
  content: string
  uploadedImages: UploadedImage[]
}

export type MarkdownViewMode = "edit" | "split" | "preview"

export type ToolbarGroupId =
  | "history"
  | "heading"
  | "inline"
  | "math"
  | "code"
  | "list"
  | "insert"
  | "link"
  | "image"
  | "session-images"

export type ToolbarActionId =
  | "undo"
  | "redo"
  | "h1"
  | "h2"
  | "h3"
  | "bold"
  | "italic"
  | "underline"
  | "strikethrough"
  | "subscript"
  | "superscript"
  | "inline-code"
  | "code-block"
  | "inline-math"
  | "block-math"
  | "bullet-list"
  | "number-list"
  | "quote"
  | "table"
  | "hr"
  | "link"
  | "image"
  | "session-images"

export type ToolbarItem = ToolbarGroupId | ToolbarActionId

export type ToolbarConfig = "all" | "simple" | "minimal" | ToolbarItem[]

export interface MarkdownEditorProps {
  /** Markdown state with content and tracked images, or plain string */
  value?: MarkdownEditorValue | string
  /** Initial value if uncontrolled */
  defaultValue?: MarkdownEditorValue | string
  /** Callback fired whenever content or images change */
  onChange?: (value: MarkdownEditorValue) => void
  /** Simplified callback if parent only cares about content string */
  onContentChange?: (content: string) => void
  /** Callback fired on editor blur */
  onBlur?: () => void
  /** Optional form label */
  label?: string
  /** Optional description or helper text */
  description?: string
  /** Required field indicator */
  required?: boolean
  /** Custom action slot next to label (e.g. info tooltip or clear button) */
  labelAction?: React.ReactNode
  /** Custom validator error node or string */
  validator?: React.ReactNode
  /** Placeholder when editor is empty */
  placeholder?: string
  /** HTML input id */
  id?: string
  /** Total height of the editor container */
  height?: string | number
  /** Min height of the editor area */
  minHeight?: string | number
  /** Additional container classes */
  className?: string
  /** Disables editing */
  readOnly?: boolean
  /** Shows loading spinner overlay */
  isLoading?: boolean
  /**
   * Toolbar configuration:
   * - "all" (default): display all tools
   * - "simple": standard headings, inline, lists, insert, link, and image
   * - "minimal": inline formatting, list, and link
   * - Array: specific groups or actions, e.g. ["heading", "inline", "link"] or ["bold", "italic", "link"]
   * - false: hide toolbar completely
   */
  toolbar?: boolean | ToolbarConfig
  /** Optional save handler triggered by Mod+S shortcut */
  onSave?: (value: MarkdownEditorValue) => void | Promise<void>
}
