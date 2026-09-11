import type * as monaco from "monaco-editor"

export type MonacoTheme = "vs-dark" | "light" | "vs" | "hc-black" | "auto"

export interface MonacoEditorRef {
  /** Returns the underlying Monaco editor instance */
  getEditor: () => monaco.editor.IStandaloneCodeEditor | null
  /** Returns current document content */
  getValue: () => string
  /** Replaces entire document content */
  setValue: (value: string) => void
  /** Focuses the editor */
  focus: () => void
  /** Returns current selection range */
  getSelection: () => monaco.Selection | null
  /** Returns selected text string */
  getSelectedText: () => string
  /** Sets selection range */
  setSelection: (selection: monaco.IRange) => void
  /** Inserts text at the current cursor/selection, supporting undo */
  insertText: (text: string) => void
  /** Wraps current selection or inserts defaultText enclosed by before and after */
  wrapSelection: (before: string, after: string, defaultText?: string) => void
  /** Transforms the current active line */
  replaceCurrentLine: (updater: (lineText: string) => string) => void
  /** Executes a built-in or custom Monaco action (e.g., 'undo', 'redo') */
  triggerAction: (actionId: string) => void
}

export interface MonacoEditorProps {
  /** Document content */
  value?: string
  /** Initial defaultValue if uncontrolled */
  defaultValue?: string
  /** Change callback when content is updated */
  onChange?: (value: string) => void
  /** Blur callback */
  onBlur?: () => void
  /** Language identifier (default: "markdown") */
  language?: string
  /** Theme ("vs-dark", "light", or "auto" synced with app theme) */
  theme?: MonacoTheme
  /** Read-only mode */
  readOnly?: boolean
  /** Height string or number (e.g. "400px", "100%", 500) */
  height?: string | number
  /** Width string or number (default: "100%") */
  width?: string | number
  /** Placeholder text shown when content is empty */
  placeholder?: string
  /** Additional Monaco editor options to override defaults */
  options?: monaco.editor.IStandaloneEditorConstructionOptions
  /** Callback fired once Monaco is mounted and ready */
  onMount?: (
    editor: monaco.editor.IStandaloneCodeEditor,
    monacoInstance: typeof monaco
  ) => void
  /** Custom keyboard shortcuts to bind */
  keybindings?: Array<{
    key: number
    handler: (editor: monaco.editor.IStandaloneCodeEditor) => void
  }>
  /** Callback fired on keydown in editor */
  onKeyDown?: (
    e: monaco.IKeyboardEvent,
    editor: monaco.editor.IStandaloneCodeEditor,
    monacoInstance: typeof monaco
  ) => void
  /** Optional container className */
  className?: string
  /** Whether to show outer border (default: false when height is 100%, true otherwise) */
  bordered?: boolean
  /** Loading state indicator */
  isLoading?: boolean
}
