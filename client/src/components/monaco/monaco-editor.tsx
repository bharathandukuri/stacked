import * as React from "react"
import Editor, { type OnMount, type Monaco } from "@monaco-editor/react"
import type * as monaco from "monaco-editor"
import { Loader2 } from "lucide-react"
import { useTheme } from "@/components/theme-provider"
import { cn } from "@/lib/utils"
import type { MonacoEditorProps, MonacoEditorRef } from "./types"

export const MonacoEditor = React.forwardRef<
  MonacoEditorRef,
  MonacoEditorProps
>(function MonacoEditor(
  {
    value,
    defaultValue = "",
    onChange,
    language = "markdown",
    theme: themeProp = "auto",
    readOnly = false,
    height = "320px",
    width = "100%",
    bordered,
    placeholder,
    options,
    onMount,
    onBlur,
    keybindings,
    onKeyDown,
    className,
    isLoading = false,
  },
  ref
) {
  const containerRef = React.useRef<HTMLDivElement>(null)
  const editorRef = React.useRef<monaco.editor.IStandaloneCodeEditor | null>(
    null
  )
  const monacoRef = React.useRef<Monaco | null>(null)
  const [contentLeft, setContentLeft] = React.useState(54)
  const { theme: appTheme } = useTheme()

  // Determine resolved Monaco theme
  const resolvedTheme = React.useMemo(() => {
    if (themeProp && themeProp !== "auto") {
      return themeProp
    }
    if (appTheme === "dark") return "vs-dark"
    if (appTheme === "light") return "light"
    if (
      typeof window !== "undefined" &&
      document.documentElement.classList.contains("dark")
    ) {
      return "vs-dark"
    }
    return "light"
  }, [themeProp, appTheme])

  // Imperative ref methods
  React.useImperativeHandle(
    ref,
    () => ({
      getEditor: () => editorRef.current,
      getValue: () => editorRef.current?.getValue() ?? "",
      setValue: (val: string) => {
        if (editorRef.current) {
          editorRef.current.setValue(val)
        }
      },
      focus: () => {
        editorRef.current?.focus()
      },
      getSelection: () => editorRef.current?.getSelection() ?? null,
      getSelectedText: () => {
        const editor = editorRef.current
        const model = editor?.getModel()
        const selection = editor?.getSelection()
        if (!editor || !model || !selection) return ""
        return model.getValueInRange(selection)
      },
      setSelection: (range: monaco.IRange) => {
        editorRef.current?.setSelection(range)
      },
      insertText: (text: string) => {
        const editor = editorRef.current
        if (!editor) return

        const model = editor.getModel()
        let selection = editor.getSelection()
        if (!selection && model) {
          const lineCount = model.getLineCount()
          const maxCol = model.getLineMaxColumn(lineCount)
          selection = {
            startLineNumber: lineCount,
            startColumn: maxCol,
            endLineNumber: lineCount,
            endColumn: maxCol,
          } as monaco.Selection
        }
        if (!selection) return

        editor.focus()
        editor.pushUndoStop()
        editor.executeEdits("monaco-insert", [
          {
            range: selection,
            text,
            forceMoveMarkers: true,
          },
        ])
        editor.pushUndoStop()
      },
      wrapSelection: (before: string, after: string, defaultText = "text") => {
        const editor = editorRef.current
        if (!editor) return

        const model = editor.getModel()
        let selection = editor.getSelection()
        if (!selection && model) {
          const lineCount = model.getLineCount()
          const maxCol = model.getLineMaxColumn(lineCount)
          selection = {
            startLineNumber: lineCount,
            startColumn: maxCol,
            endLineNumber: lineCount,
            endColumn: maxCol,
          } as monaco.Selection
        }
        if (!selection || !model) return

        const selectedText = model.getValueInRange(selection)
        const textToWrap = selectedText || defaultText
        const replacement = `${before}${textToWrap}${after}`

        editor.focus()
        editor.pushUndoStop()
        editor.executeEdits("monaco-wrap", [
          {
            range: selection,
            text: replacement,
            forceMoveMarkers: true,
          },
        ])
        editor.pushUndoStop()

        // Position cursor/selection inside wrapped text if no initial selection
        if (!selectedText) {
          const startCol = selection.startColumn + before.length
          const endCol = startCol + textToWrap.length
          editor.setSelection({
            startLineNumber: selection.startLineNumber,
            startColumn: startCol,
            endLineNumber: selection.startLineNumber,
            endColumn: endCol,
          })
        }
      },
      replaceCurrentLine: (updater: (lineText: string) => string) => {
        const editor = editorRef.current
        const model = editor?.getModel()
        if (!editor || !model) return

        const position = editor.getPosition()
        if (!position) return

        const lineNumber = position.lineNumber
        const currentLineContent = model.getLineContent(lineNumber)
        const newLineContent = updater(currentLineContent)

        const lineRange: monaco.IRange = {
          startLineNumber: lineNumber,
          startColumn: 1,
          endLineNumber: lineNumber,
          endColumn: currentLineContent.length + 1,
        }

        editor.focus()
        editor.pushUndoStop()
        editor.executeEdits("monaco-line-replace", [
          {
            range: lineRange,
            text: newLineContent,
            forceMoveMarkers: true,
          },
        ])
        editor.pushUndoStop()
      },
      triggerAction: (actionId: string) => {
        editorRef.current?.trigger("custom", actionId, null)
      },
    }),
    []
  )

  const handleEditorMount: OnMount = (editor, monacoInstance) => {
    editorRef.current = editor
    monacoRef.current = monacoInstance

    // Bind custom keybindings if provided
    if (keybindings && keybindings.length > 0) {
      for (const kb of keybindings) {
        editor.addCommand(kb.key, () => kb.handler(editor))
      }
    }

    // Register blur handler
    if (onBlur) {
      editor.onDidBlurEditorText(() => {
        onBlur()
      })
    }

    // Register keydown handler
    if (onKeyDown) {
      editor.onKeyDown((e) => {
        onKeyDown(e, editor, monacoInstance)
      })
    }

    // Update content offset for placeholder alignment
    const initialLayout = editor.getLayoutInfo()
    if (initialLayout.contentLeft > 0) {
      setContentLeft(initialLayout.contentLeft)
    }
    editor.onDidLayoutChange((layout) => {
      if (layout.contentLeft > 0) {
        setContentLeft(layout.contentLeft)
      }
    })

    // Ensure editor updates layout when container is resized (window resize, split-view drag)
    const resizeObserver = new ResizeObserver(() => {
      editor.layout()
    })
    if (containerRef.current) {
      resizeObserver.observe(containerRef.current)
    }
    editor.onDidDispose(() => {
      resizeObserver.disconnect()
    })

    onMount?.(editor, monacoInstance)
  }

  const defaultOptions: monaco.editor.IStandaloneEditorConstructionOptions = {
    readOnly,
    wordWrap: "on",
    minimap: { enabled: false },
    fontSize: 13,
    lineHeight: 20,
    fontFamily:
      "ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace",
    tabSize: 2,
    insertSpaces: true,
    lineNumbers: "on",
    lineNumbersMinChars: 3,
    glyphMargin: false,
    folding: true,
    scrollBeyondLastLine: false,
    automaticLayout: true,
    overviewRulerLanes: 0,
    hideCursorInOverviewRuler: true,
    renderLineHighlight: "none",
    padding: { top: 8, bottom: 8 },
    scrollbar: {
      vertical: "visible",
      horizontal: "auto",
      verticalScrollbarSize: 10,
      horizontalScrollbarSize: 10,
      useShadows: false,
    },
    ...options,
  }

  const isFullHeight = height === "100%"
  const showBorder = bordered !== undefined ? bordered : !isFullHeight

  return (
    <div
      ref={containerRef}
      style={{
        height: typeof height === "number" ? `${height}px` : height,
        width: typeof width === "number" ? `${width}px` : width,
      }}
      className={cn(
        "relative flex h-full min-h-0 w-full flex-col overflow-hidden bg-background",
        showBorder && "rounded-md border border-border/80",
        className
      )}
    >
      {isLoading && (
        <div className="absolute inset-0 z-20 flex items-center justify-center bg-background/80 backdrop-blur-xs">
          <Loader2 className="h-5 w-5 animate-spin text-primary" />
        </div>
      )}
      <div className="relative h-full min-h-0 w-full flex-1 overflow-hidden">
        <Editor
          height="100%"
          width="100%"
          language={language}
          value={value}
          defaultValue={defaultValue}
          theme={resolvedTheme}
          options={defaultOptions}
          onChange={(val) => onChange?.(val ?? "")}
          onMount={handleEditorMount}
          loading={
            <div className="flex h-full min-h-[200px] w-full items-center justify-center bg-muted/10 text-xs text-muted-foreground">
              <div className="flex items-center gap-2">
                <Loader2 className="h-4 w-4 animate-spin text-primary" />
                <span>Loading Editor...</span>
              </div>
            </div>
          }
        />
        {placeholder && (!value || value.length === 0) && (
          <div
            style={{
              top: 8,
              left: contentLeft,
              lineHeight: "20px",
              fontSize: "13px",
              fontFamily:
                "ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace",
            }}
            className="pointer-events-none absolute text-muted-foreground/50 select-none"
          >
            {placeholder}
          </div>
        )}
      </div>
    </div>
  )
})
