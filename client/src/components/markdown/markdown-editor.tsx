import * as React from "react"
import axios from "axios"
import { useId, useRef, useState, useCallback, useMemo, useEffect } from "react"
import { useHotkeys } from "@tanstack/react-hotkeys"
import { TooltipProvider } from "@/components/ui/tooltip"
import { Spinner } from "@/components/ui/spinner"
import { Badge } from "@/components/ui/badge"
import { Label } from "@/components/ui/label"
import type * as monaco from "monaco-editor"
import { MonacoEditor } from "@/components/monaco"
import type { MonacoEditorRef } from "@/components/monaco/types"
import { toast } from "@/components/ui/toast"
import { cn } from "@/lib/utils"

import { MarkdownToolbar } from "./markdown-toolbar"
import { LinkDialog } from "./dialogs/link-dialog"
import {
  ImageUploadDialog,
  type ImageInsertResult,
} from "./dialogs/image-upload-dialog"
import { SessionImagesDialog } from "./dialogs/session-images-dialog"
import { TOOLBAR_ACTIONS, type ToolbarActionDef } from "./markdown-config"
import type {
  MarkdownEditorProps,
  MarkdownEditorValue,
  UploadedImage,
} from "./types"
import {
  createUploadedImageFromFileResponse,
  syncImageReferences,
} from "./utils/image-tracker"
import {
  handleMarkdownListEnter,
  handleMarkdownListTab,
} from "./utils/list-continuation"
import { parseTanstackHotkeyToMonaco } from "./utils/hotkey-monaco"
import { fileService } from "@/services/file-service"
import { UploadCloud } from "lucide-react"

export function MarkdownEditor({
  value,
  defaultValue,
  onChange,
  onContentChange,
  onBlur,
  onSave,
  label,
  description,
  required,
  labelAction,
  validator,
  placeholder = "Write your markdown content here...",
  id,
  height = 420,
  minHeight = 260,
  className,
  readOnly = false,
  isLoading = false,
  toolbar = "all",
}: MarkdownEditorProps) {
  const generatedId = useId()
  const editorId = id ?? generatedId
  const monacoRef = useRef<MonacoEditorRef | null>(null)
  const containerRef = useRef<HTMLDivElement>(null)

  // Internal state when uncontrolled
  const [internalState, setInternalState] = useState<MarkdownEditorValue>(
    () => {
      if (value !== undefined) {
        if (typeof value === "string") {
          return { content: value, uploadedImages: [] }
        }
        return value
      }
      if (defaultValue !== undefined) {
        if (typeof defaultValue === "string") {
          return { content: defaultValue, uploadedImages: [] }
        }
        return defaultValue
      }
      return { content: "", uploadedImages: [] }
    }
  )

  // Controlled vs uncontrolled resolution
  const isControlled = value !== undefined
  const currentState: MarkdownEditorValue = useMemo(() => {
    if (!isControlled) return internalState
    if (typeof value === "string") {
      return {
        content: value,
        uploadedImages: internalState.uploadedImages,
      }
    }
    return value
  }, [isControlled, value, internalState])

  // Ref tracking latest state to prevent closures from evaluating stale render state
  const latestStateRef = useRef<MarkdownEditorValue>(currentState)
  useEffect(() => {
    latestStateRef.current = currentState
  }, [currentState])

  // Flag to suppress Monaco's onChange during programmatic insertions (images, links)
  const isProgrammaticEditRef = useRef(false)

  const content = currentState.content
  const uploadedImages = currentState.uploadedImages

  // Dialog visibility states
  const [isLinkDialogOpen, setIsLinkDialogOpen] = useState(false)
  const [isImageUploadDialogOpen, setIsImageUploadDialogOpen] = useState(false)
  const [editingImageFile, setEditingImageFile] = useState<File | null>(null)
  const [isSessionImagesDialogOpen, setIsSessionImagesDialogOpen] =
    useState(false)
  const [selectedTextForLink, setSelectedTextForLink] = useState("")
  const [isUploadingDrop, setIsUploadingDrop] = useState(false)

  // Sync state updater helper
  const updateState = useCallback(
    (updater: (prev: MarkdownEditorValue) => MarkdownEditorValue) => {
      const prevState = latestStateRef.current
      const nextState = updater(prevState)
      latestStateRef.current = nextState
      setInternalState(nextState)
      onChange?.(nextState)
      if (nextState.content !== prevState.content) {
        onContentChange?.(nextState.content)
      }
    },
    [onChange, onContentChange]
  )

  const handleContentChange = useCallback(
    (newContent: string) => {
      if (isProgrammaticEditRef.current) return
      updateState((prev) => ({ ...prev, content: newContent }))
    },
    [updateState]
  )

  // Execute toolbar action
  const handleExecuteAction = useCallback((action: ToolbarActionDef) => {
    const editor = monacoRef.current
    if (!editor) return

    if (action.id === "undo") {
      editor.triggerAction("undo")
      return
    }
    if (action.id === "redo") {
      editor.triggerAction("redo")
      return
    }

    if (action.type === "heading" && action.level) {
      const hashes = "#".repeat(action.level) + " "
      editor.replaceCurrentLine((line) => {
        // Remove existing heading hashes if any
        const cleanLine = line.replace(/^#{1,6}\s*/, "")
        return `${hashes}${cleanLine || action.fallback || ""}`
      })
      return
    }

    if (action.id === "hr") {
      editor.insertText("\n---\n")
      return
    }

    if (action.id === "table") {
      editor.insertText(action.prefix || "")
      return
    }

    if (action.prefix || action.suffix) {
      editor.wrapSelection(
        action.prefix || "",
        action.suffix || "",
        action.fallback
      )
    }
  }, [])

  // Open dialog helpers (wrapped in useCallback to safely use without ref access in render)
  const handleOpenLinkDialog = useCallback(() => {
    const text = monacoRef.current?.getSelectedText() ?? ""
    setSelectedTextForLink(text)
    setIsLinkDialogOpen(true)
  }, [])

  const handleOpenImageDialog = useCallback(() => {
    setIsImageUploadDialogOpen(true)
  }, [])

  // Native Monaco editor mount handler for instant keystroke binding
  const handleEditorMount = useCallback(
    (
      editor: monaco.editor.IStandaloneCodeEditor,
      monacoInstance: typeof monaco
    ) => {
      // Dynamically register all defined toolbar action shortcuts into Monaco
      for (const action of TOOLBAR_ACTIONS) {
        if (action.shortcut) {
          const monacoKey = parseTanstackHotkeyToMonaco(
            monacoInstance,
            action.shortcut
          )
          if (monacoKey !== null) {
            editor.addCommand(monacoKey, () => {
              handleExecuteAction(action)
            })
          }
        }
      }

      // Link dialog shortcut (Mod+K)
      const linkKey = parseTanstackHotkeyToMonaco(monacoInstance, "Mod+K")
      if (linkKey !== null) {
        editor.addCommand(linkKey, handleOpenLinkDialog)
      }

      // Image upload dialog shortcut (Mod+Alt+I)
      const imageKey = parseTanstackHotkeyToMonaco(monacoInstance, "Mod+Alt+I")
      if (imageKey !== null) {
        editor.addCommand(imageKey, handleOpenImageDialog)
      }

      // Save document shortcut (Mod+S)
      if (onSave) {
        const saveKey = parseTanstackHotkeyToMonaco(monacoInstance, "Mod+S")
        if (saveKey !== null) {
          editor.addCommand(saveKey, () => {
            onSave(latestStateRef.current)
          })
        }
      }
    },
    [handleExecuteAction, handleOpenLinkDialog, handleOpenImageDialog, onSave]
  )

  // Intelligent markdown keydown interceptor (list continuation, empty list exit, tab indent/outdent)
  const handleEditorKeyDown = useCallback(
    (
      e: monaco.IKeyboardEvent,
      editor: monaco.editor.IStandaloneCodeEditor,
      monacoInstance: typeof monaco
    ) => {
      // Enter key: Intelligent Markdown list and blockquote continuation
      if (
        e.keyCode === monacoInstance.KeyCode.Enter &&
        !e.shiftKey &&
        !e.ctrlKey &&
        !e.altKey &&
        !e.metaKey
      ) {
        const handled = handleMarkdownListEnter(editor)
        if (handled) {
          e.preventDefault()
          e.stopPropagation()
          return
        }
      }

      // Tab / Shift+Tab key on list items: Indent / Outdent
      if (
        e.keyCode === monacoInstance.KeyCode.Tab &&
        !e.ctrlKey &&
        !e.altKey &&
        !e.metaKey
      ) {
        const handled = handleMarkdownListTab(editor, e.shiftKey)
        if (handled) {
          e.preventDefault()
          e.stopPropagation()
          return
        }
      }
    },
    []
  )

  // Hotkey handlers using TanStack Hotkeys (with ignoreInputs: false so they fire across inputs/editor)
  useHotkeys(
    [
      ...TOOLBAR_ACTIONS.filter((a) => Boolean(a.shortcut)).map((action) => ({
        hotkey:
          action.shortcut as import("@tanstack/hotkeys").RegisterableHotkey,
        callback: (e: KeyboardEvent) => {
          e.preventDefault()
          handleExecuteAction(action)
        },
      })),
      {
        hotkey: "Mod+K" as import("@tanstack/hotkeys").RegisterableHotkey,
        callback: (e: KeyboardEvent) => {
          e.preventDefault()
          handleOpenLinkDialog()
        },
      },
      {
        hotkey: "Mod+Alt+I" as import("@tanstack/hotkeys").RegisterableHotkey,
        callback: (e: KeyboardEvent) => {
          e.preventDefault()
          handleOpenImageDialog()
        },
      },
      ...(onSave
        ? [
            {
              hotkey: "Mod+S" as import("@tanstack/hotkeys").RegisterableHotkey,
              callback: (e: KeyboardEvent) => {
                e.preventDefault()
                onSave(latestStateRef.current)
              },
            },
          ]
        : []),
    ],
    { ignoreInputs: false }
  )

  // Helper to insert text into Monaco and immediately synchronize document content and tracked images
  const handleInsertText = useCallback(
    (textToInsert: string, newImage?: UploadedImage) => {
      const editor = monacoRef.current
      if (editor) {
        isProgrammaticEditRef.current = true
        editor.insertText(textToInsert)
        const latestContent = editor.getValue()
        updateState((prev) => ({
          ...prev,
          content: latestContent,
          uploadedImages: newImage
            ? [
                ...prev.uploadedImages.filter(
                  (img) => img.fileId !== newImage.fileId
                ),
                newImage,
              ]
            : prev.uploadedImages,
        }))
        isProgrammaticEditRef.current = false
      } else {
        updateState((prev) => ({
          ...prev,
          content: prev.content
            ? `${prev.content}\n${textToInsert}`
            : textToInsert,
          uploadedImages: newImage
            ? [
                ...prev.uploadedImages.filter(
                  (img) => img.fileId !== newImage.fileId
                ),
                newImage,
              ]
            : prev.uploadedImages,
        }))
      }
    },
    [updateState]
  )

  // Handle image upload from files (drag-drop, paste, or dialog)
  const handleUploadImageFile = useCallback(
    async (file: File) => {
      if (!file.type.startsWith("image/")) {
        toast.add({
          title: "Invalid File",
          description:
            "Only image files (PNG, JPG, WebP, GIF, SVG) are supported.",
          type: "error",
        })
        return
      }

      setIsUploadingDrop(true)
      try {
        const fileResponse = await fileService.uploadTemporary(file)
        const newImage = createUploadedImageFromFileResponse(fileResponse)

        // Insert markdown image tag and sync state
        const markdownTag = `\n![${file.name.replace(/\.[^/.]+$/, "")}](${newImage.url})\n`
        handleInsertText(markdownTag, newImage)

        toast.add({
          title: "Image Uploaded",
          description: `${file.name} uploaded to temporary storage.`,
          type: "success",
        })
      } catch (err: unknown) {
        console.error("Failed to upload image file:", err)
        let message = "Failed to upload image. Please try again."
        if (axios.isAxiosError(err) && err.response?.data?.message) {
          message = err.response.data.message
        } else if (err instanceof Error) {
          message = err.message
        }
        toast.add({
          title: "Upload Failed",
          description: message,
          type: "error",
        })
      } finally {
        setIsUploadingDrop(false)
      }
    },
    [handleInsertText]
  )

  // Drag and drop image upload handlers
  const handleDragOver = (e: React.DragEvent) => {
    if (readOnly) return
    if (e.dataTransfer.types.includes("Files")) {
      e.preventDefault()
    }
  }

  const handleDrop = async (e: React.DragEvent) => {
    if (readOnly) return
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      e.preventDefault()
      const files = Array.from(e.dataTransfer.files)
      const imageFiles = files.filter((f) => f.type.startsWith("image/"))

      if (imageFiles.length === 0) return

      for (const imgFile of imageFiles) {
        await handleUploadImageFile(imgFile)
      }
    }
  }

  // Paste image handler
  const handlePaste = async (e: React.ClipboardEvent) => {
    if (readOnly) return
    const items = e.clipboardData?.items
    if (!items) return

    for (let i = 0; i < items.length; i++) {
      if (items[i].type.startsWith("image/")) {
        const file = items[i].getAsFile()
        if (file) {
          e.preventDefault()
          await handleUploadImageFile(file)
          break
        }
      }
    }
  }

  // Image insertion from ImageUploadDialog
  const handleImageDialogSubmit = useCallback(
    (result: ImageInsertResult) => {
      const imgMarkdown = result.markdown.startsWith("\n")
        ? result.markdown
        : `\n${result.markdown}\n`
      handleInsertText(imgMarkdown, result.uploadedImage)
    },
    [handleInsertText]
  )

  // Delete image from session
  const handleDeleteSessionImage = useCallback(
    (fileId: string) => {
      updateState((prev) => ({
        ...prev,
        uploadedImages: prev.uploadedImages.filter(
          (img) => img.fileId !== fileId
        ),
      }))
    },
    [updateState]
  )

  // Stats calculation
  const stats = useMemo(() => {
    const chars = content.length
    const words = content.trim() ? content.trim().split(/\s+/).length : 0
    const lines = content ? content.split("\n").length : 0
    const { referencedCount, unusedCount } = syncImageReferences(
      content,
      uploadedImages
    )
    return { chars, words, lines, referencedCount, unusedCount }
  }, [content, uploadedImages])

  const effectiveHeight = typeof height === "number" ? `${height}px` : height
  const effectiveMinHeight =
    typeof minHeight === "number" ? `${minHeight}px` : minHeight

  return (
    <TooltipProvider>
      <div
        ref={containerRef}
        id={editorId}
        onDragOver={handleDragOver}
        onDrop={handleDrop}
        onPaste={handlePaste}
        className={cn(
          "group flex w-full flex-col text-foreground",
          height === "100%" && "h-full min-h-0 flex-1",
          className
        )}
      >
        {/* Label & Description Header */}
        {(label || description || labelAction) && (
          <div className="flex shrink-0 items-center justify-between pb-1.5">
            <div className="space-y-0.5">
              {label && (
                <Label
                  htmlFor={editorId}
                  className="flex items-center gap-1 text-xs font-semibold text-foreground"
                >
                  {label}
                  {required && <span className="text-destructive">*</span>}
                </Label>
              )}
              {description && (
                <p className="text-[11px] text-muted-foreground">
                  {description}
                </p>
              )}
            </div>
            {labelAction && <div>{labelAction}</div>}
          </div>
        )}

        {/* Editor Main Container */}
        <div
          className={cn(
            "relative flex flex-col overflow-hidden rounded-lg border border-border bg-card shadow-xs transition-colors focus-within:border-primary/60",
            height === "100%" && "h-full min-h-0 flex-1"
          )}
        >
          {/* Toolbar */}
          <MarkdownToolbar
            toolbar={toolbar}
            onExecuteAction={handleExecuteAction}
            onOpenLinkDialog={handleOpenLinkDialog}
            onOpenImageDialog={handleOpenImageDialog}
            onOpenSessionImagesDialog={() => setIsSessionImagesDialogOpen(true)}
            sessionImageCount={uploadedImages.length}
            disabled={readOnly || isLoading}
          />

          {/* Editor Body */}
          <div
            style={{
              height: height === "100%" ? undefined : effectiveHeight,
              minHeight: height === "100%" ? undefined : effectiveMinHeight,
            }}
            className="relative min-h-0 w-full flex-1 overflow-hidden"
          >
            <MonacoEditor
              ref={monacoRef}
              value={content}
              onChange={handleContentChange}
              onBlur={onBlur}
              language="markdown"
              readOnly={readOnly}
              placeholder={placeholder}
              height="100%"
              onMount={handleEditorMount}
              onKeyDown={handleEditorKeyDown}
              options={{
                wordWrap: "on",
                lineNumbers: "on",
                minimap: { enabled: false },
                scrollbar: {
                  vertical: "visible",
                  horizontal: "auto",
                  verticalScrollbarSize: 10,
                  horizontalScrollbarSize: 10,
                  useShadows: false,
                },
              }}
            />

            {/* Uploading Drag & Drop Overlay */}
            {isUploadingDrop && (
              <div className="absolute inset-0 z-20 flex flex-col items-center justify-center gap-2 bg-background/80 backdrop-blur-xs">
                <Spinner className="h-6 w-6 text-primary" />
                <span className="flex items-center gap-1.5 text-xs font-semibold text-foreground">
                  <UploadCloud className="h-4 w-4 animate-bounce text-primary" />
                  Uploading dropped image to storage...
                </span>
              </div>
            )}

            {/* Loading Overlay */}
            {isLoading && (
              <div className="absolute inset-0 z-20 flex items-center justify-center bg-background/60 backdrop-blur-xs">
                <Spinner className="h-6 w-6 text-primary" />
              </div>
            )}
          </div>

          {/* Footer Status Bar */}
          <div className="flex flex-wrap items-center justify-between border-t border-border/80 bg-muted/30 px-3 py-1 text-[11px] text-muted-foreground select-none">
            <div className="flex items-center gap-3">
              <span>{stats.lines} lines</span>
              <span>&bull;</span>
              <span>{stats.words} words</span>
              <span>&bull;</span>
              <span>{stats.chars} chars</span>

              {uploadedImages.length > 0 && (
                <>
                  <span>&bull;</span>
                  <button
                    type="button"
                    onClick={() => setIsSessionImagesDialogOpen(true)}
                    className="flex cursor-pointer items-center gap-1 font-medium hover:text-foreground"
                  >
                    <span>{uploadedImages.length} images</span>
                    {stats.unusedCount > 0 ? (
                      <Badge
                        variant="secondary"
                        className="h-3.5 bg-amber-500/10 px-1 py-0 text-[9px] font-medium text-amber-500"
                      >
                        {stats.unusedCount} unused
                      </Badge>
                    ) : (
                      <Badge
                        variant="secondary"
                        className="h-3.5 bg-emerald-500/10 px-1 py-0 text-[9px] font-medium text-emerald-500"
                      >
                        all in use
                      </Badge>
                    )}
                  </button>
                </>
              )}
            </div>
          </div>
        </div>

        {/* Validator Message Slot */}
        {validator && <div className="pt-1">{validator}</div>}

        {/* Dialogs */}
        <LinkDialog
          isOpen={isLinkDialogOpen}
          onClose={() => setIsLinkDialogOpen(false)}
          onSubmit={(linkMarkdown) => handleInsertText(linkMarkdown)}
          initialText={selectedTextForLink}
        />

        <ImageUploadDialog
          isOpen={isImageUploadDialogOpen}
          initialFile={editingImageFile}
          onClose={() => {
            setIsImageUploadDialogOpen(false)
            setEditingImageFile(null)
          }}
          onSubmit={handleImageDialogSubmit}
        />

        <SessionImagesDialog
          isOpen={isSessionImagesDialogOpen}
          onClose={() => setIsSessionImagesDialogOpen(false)}
          content={content}
          images={uploadedImages}
          onInsertImage={(imgMd) => handleInsertText(`\n${imgMd}\n`)}
          onDeleteImage={handleDeleteSessionImage}
        />
      </div>
    </TooltipProvider>
  )
}
